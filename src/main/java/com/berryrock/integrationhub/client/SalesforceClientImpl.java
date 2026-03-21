package com.berryrock.integrationhub.client;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.berryrock.integrationhub.model.SalesforceAddressRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class SalesforceClientImpl implements SalesforceClient {
    private static final Logger log = LoggerFactory.getLogger(SalesforceClientImpl.class);

    @Value("${integration.vendor.salesforce.enabled:false}")
    private boolean enabled;

    @Value("${integration.vendor.salesforce.login-url:https://login.salesforce.com}")
    private String loginUrl;

    @Value("${integration.vendor.salesforce.api-version:v61.0}")
    private String apiVersion;

    @Value("${integration.vendor.salesforce.client-id:}")
    private String clientId;

    @Value("${integration.vendor.salesforce.username:}")
    private String username;

    @Value("${integration.vendor.salesforce.private-key-path:}")
    private String privateKeyPath;

    @Value("${integration.vendor.salesforce.token-skew-seconds:60}")
    private int tokenSkewSeconds;

    private final RestTemplate restTemplate;

    public SalesforceClientImpl() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public boolean isConfigured() {
        log.debug("Checking Salesforce configuration status");
        return enabled;
    }

    private static class SalesforceAuthResponse {
        private final String accessToken;
        private final String instanceUrl;

        public SalesforceAuthResponse(String accessToken, String instanceUrl) {
            this.accessToken = accessToken;
            this.instanceUrl = instanceUrl;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getInstanceUrl() {
            return instanceUrl;
        }
    }

    private SalesforceAuthResponse authenticate() {
        if (clientId == null || clientId.isEmpty()) {
            throw new IllegalStateException("Salesforce client-id is missing from configuration");
        }
        if (username == null || username.isEmpty()) {
            throw new IllegalStateException("Salesforce username is missing from configuration");
        }
        if (privateKeyPath == null || privateKeyPath.isEmpty()) {
            throw new IllegalStateException("Salesforce private-key-path is missing from configuration");
        }

        log.info("Initiating Salesforce JWT Bearer token flow for user: {}", username);

        try {
            // Load Private Key
            String keyContent = new String(Files.readAllBytes(Paths.get(privateKeyPath)))
                    .replaceAll("-----BEGIN PRIVATE KEY-----", "")
                    .replaceAll("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");

            byte[] keyBytes = Base64.getDecoder().decode(keyContent);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            RSAPrivateKey privateKey = (RSAPrivateKey) kf.generatePrivate(spec);

            Algorithm algorithm = Algorithm.RSA256(null, privateKey);

            Instant now = Instant.now();
            String jwt = JWT.create()
                    .withIssuer(clientId)
                    .withSubject(username)
                    .withAudience(loginUrl)
                    .withExpiresAt(Date.from(now.plusSeconds(180))) // Give it a generous 3 min expiry window for transit
                    .sign(algorithm);

            // Make request for token
            String tokenUrl = loginUrl + "/services/oauth2/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
            body.add("assertion", jwt);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> respBody = response.getBody();
            if (respBody == null || !respBody.containsKey("access_token") || !respBody.containsKey("instance_url")) {
                throw new IllegalStateException("Token response was missing required fields. Body keys: " + (respBody != null ? respBody.keySet() : "null"));
            }

            return new SalesforceAuthResponse(
                    (String) respBody.get("access_token"),
                    (String) respBody.get("instance_url")
            );

        } catch (Exception e) {
            log.error("Salesforce JWT authentication failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to authenticate with Salesforce", e);
        }
    }

    @Override
    public List<SalesforceAddressRecord> fetchAddressesForGoogleSheetBuildiumSync() {
        if (!enabled) {
            log.warn("Salesforce integration is disabled. Returning empty list.");
            return new ArrayList<>();
        }

        log.info("Fetching Salesforce records for sync");

        SalesforceAuthResponse authResponse;
        try {
            authResponse = authenticate();
        } catch (Exception e) {
            // The actual real cause is already logged in authenticate(), but we capture it here to abort the sync.
            log.error("Aborting Salesforce sync due to authentication failure. Cause: {}", e.getMessage());
            return new ArrayList<>();
        }

        String instanceUrl = authResponse.getInstanceUrl();
        log.info("Salesforce Query Host (instance_url): {}", instanceUrl);

        String soql = "SELECT Id, Property_Address__c, Property_City__c, Property_State__c, Property_Zip__c FROM Opportunity WHERE StageName = 'Closed Won'";
        String queryUrl = instanceUrl + "/services/data/" + apiVersion + "/query?q={soql}";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authResponse.getAccessToken());
        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    queryUrl,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {},
                    soql
            );

            return parseSalesforceResponse(response.getBody());
        } catch (Exception e) {
            log.error("Failed to fetch from Salesforce using host {}: {}", instanceUrl, e.getMessage());
            return new ArrayList<>();
        }
    }

    @SuppressWarnings("unchecked")
    private List<SalesforceAddressRecord> parseSalesforceResponse(Map<String, Object> body) {
        List<SalesforceAddressRecord> records = new ArrayList<>();
        if (body == null || !body.containsKey("records")) {
            return records;
        }

        List<Map<String, Object>> sfRecords = (List<Map<String, Object>>) body.get("records");
        for (Map<String, Object> record : sfRecords) {
            SalesforceAddressRecord sf = new SalesforceAddressRecord();
            sf.setOpportunityId((String) record.get("Id"));
            sf.setRawAddress((String) record.get("Property_Address__c"));
            sf.setAddressLine((String) record.get("Property_Address__c"));
            sf.setCity((String) record.get("Property_City__c"));
            sf.setState((String) record.get("Property_State__c"));
            sf.setPostalCode((String) record.get("Property_Zip__c"));
            sf.setSourceSystem("Salesforce");
            records.add(sf);
        }
        return records;
    }
}
