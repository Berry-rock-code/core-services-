package com.berryrock.integrationhub.client;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.berryrock.integrationhub.model.SalesforceAddressRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
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
import java.util.*;

/**
 * Salesforce REST API client that authenticates via the OAuth 2.0 JWT bearer token flow.
 *
 * Part of the client layer — performs the JWT grant to obtain a short-lived access token,
 * then issues a SOQL query against the Opportunity object to retrieve address fields needed
 * for the Google Sheet and Buildium sync pipeline.
 *
 * The RSA private key used to sign the JWT is loaded from the file path injected via
 * {@code integration.vendor.salesforce.private-key-path}. All configurable field names
 * and the SOQL stage filter are injected from {@code application.yml} so the query can be
 * adapted to different Salesforce org schemas without code changes.
 *
 * When {@code integration.vendor.salesforce.enabled} is {@code false}, all data-fetch
 * methods return empty lists without making any HTTP calls.
 */
@Component
public class SalesforceClientImpl implements SalesforceClient
{
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

    @Value("${integration.vendor.salesforce.fields.id:Id}")
    private String idField;

    @Value("${integration.vendor.salesforce.fields.full-address:New_Home_Address__c}")
    private String fullAddressField;

    @Value("${integration.vendor.salesforce.fields.city:}")
    private String cityField;

    @Value("${integration.vendor.salesforce.fields.state:}")
    private String stateField;

    @Value("${integration.vendor.salesforce.fields.postal-code:}")
    private String postalCodeField;

    @Value("${integration.vendor.salesforce.fields.country-code:}")
    private String countryCodeField;

    @Value("${integration.vendor.salesforce.fields.stage:StageName}")
    private String stageField;

    @Value("${integration.vendor.salesforce.query.stage-filter:}")
    private String stageFilter;

    private final RestTemplate restTemplate;

    /**
     * Constructs the client with a default {@link org.springframework.web.client.RestTemplate}.
     *
     * All configurable properties are injected by Spring after construction via
     * {@code @Value} field injection.
     */
    public SalesforceClientImpl()
    {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public boolean isConfigured()
    {
        return enabled;
    }

    private static class SalesforceAuthResponse
    {
        private final String accessToken;
        private final String instanceUrl;

        public SalesforceAuthResponse(String accessToken, String instanceUrl)
        {
            this.accessToken = accessToken;
            this.instanceUrl = instanceUrl;
        }

        public String getAccessToken()
        {
            return accessToken;
        }

        public String getInstanceUrl()
        {
            return instanceUrl;
        }
    }

    private SalesforceAuthResponse authenticate()
    {
        try
        {
            String keyContent = new String(Files.readAllBytes(Paths.get(privateKeyPath)))
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
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
                    .withExpiresAt(Date.from(now.plusSeconds(180)))
                    .sign(algorithm);

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
            if (respBody == null)
            {
                throw new IllegalStateException("Token response body was null");
            }

            String accessToken = (String) respBody.get("access_token");
            String instanceUrl = (String) respBody.get("instance_url");

            if (accessToken == null || instanceUrl == null)
            {
                throw new IllegalStateException("Token response missing access_token or instance_url");
            }

            return new SalesforceAuthResponse(accessToken, instanceUrl);
        }
        catch (Exception e)
        {
            log.error("Salesforce JWT authentication failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to authenticate with Salesforce", e);
        }
    }

    @Override
    public List<SalesforceAddressRecord> fetchAddressesForGoogleSheetBuildiumSync()
    {
        if (!enabled)
        {
            log.warn("Salesforce integration is disabled. Returning empty list.");
            return new ArrayList<>();
        }

        SalesforceAuthResponse auth = authenticate();
        String instanceUrl = auth.getInstanceUrl();

        List<String> fieldList = new ArrayList<>();
        fieldList.add(idField);

        if (!isBlank(fullAddressField)) fieldList.add(fullAddressField);
        if (!isBlank(cityField)) fieldList.add(cityField);
        if (!isBlank(stateField)) fieldList.add(stateField);
        if (!isBlank(postalCodeField)) fieldList.add(postalCodeField);
        if (!isBlank(countryCodeField)) fieldList.add(countryCodeField);

        String whereClause = fullAddressField + " != null";
        if (!isBlank(stageFilter))
        {
            whereClause += " AND " + stageField + " = '" + stageFilter + "'";
        }

        String soql = "SELECT " + String.join(", ", fieldList)
                + " FROM Opportunity"
                + " WHERE " + whereClause;

        String queryUrl = instanceUrl + "/services/data/" + apiVersion + "/query?q={soql}";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(auth.getAccessToken());
        HttpEntity<?> entity = new HttpEntity<>(headers);

        List<SalesforceAddressRecord> allRecords = new ArrayList<>();
        String nextRecordsUrl = queryUrl;
        boolean isNext = false;

        try
        {
            log.info("Salesforce Query Host (instance_url): {}", instanceUrl);
            log.info("Salesforce SOQL: {}", soql);

            while (nextRecordsUrl != null)
            {
                ResponseEntity<Map<String, Object>> response;
                if (isNext)
                {
                    response = restTemplate.exchange(
                            instanceUrl + nextRecordsUrl,
                            HttpMethod.GET,
                            entity,
                            new ParameterizedTypeReference<Map<String, Object>>() {}
                    );
                }
                else
                {
                    response = restTemplate.exchange(
                            queryUrl,
                            HttpMethod.GET,
                            entity,
                            new ParameterizedTypeReference<Map<String, Object>>() {},
                            soql
                    );
                }

                Map<String, Object> body = response.getBody();
                if (body != null)
                {
                    allRecords.addAll(parseSalesforceResponse(body));
                    nextRecordsUrl = (String) body.get("nextRecordsUrl");
                    isNext = true;
                }
                else
                {
                    nextRecordsUrl = null;
                }
            }

            return allRecords;
        }
        catch (Exception e)
        {
            log.error("Failed to fetch from Salesforce using host {}: {}", instanceUrl, e.getMessage(), e);
            return allRecords;
        }
    }

    @SuppressWarnings("unchecked")
    private List<SalesforceAddressRecord> parseSalesforceResponse(Map<String, Object> body)
    {
        List<SalesforceAddressRecord> records = new ArrayList<>();

        if (body == null || !body.containsKey("records"))
        {
            return records;
        }

        List<Map<String, Object>> sfRecords = (List<Map<String, Object>>) body.get("records");

        for (Map<String, Object> record : sfRecords)
        {
            SalesforceAddressRecord sf = new SalesforceAddressRecord();

            sf.setOpportunityId(asString(record.get(idField)));

            String fullAddress = asString(record.get(fullAddressField));
            String city = isBlank(cityField) ? null : asString(record.get(cityField));
            String state = isBlank(stateField) ? null : asString(record.get(stateField));
            String postalCode = isBlank(postalCodeField) ? null : asString(record.get(postalCodeField));
            String countryCode = isBlank(countryCodeField) ? null : asString(record.get(countryCodeField));

            StringBuilder raw = new StringBuilder();
            if (fullAddress != null)
            {
                raw.append(fullAddress);
            }
            if (city != null)
            {
                if (raw.length() > 0)
                {
                    raw.append(", ");
                }
                raw.append(city);
            }
            if (state != null)
            {
                if (raw.length() > 0)
                {
                    raw.append(", ");
                }
                raw.append(state);
            }
            if (postalCode != null)
            {
                if (raw.length() > 0)
                {
                    raw.append(" ");
                }
                raw.append(postalCode);
            }

            sf.setRawAddress(raw.toString());
            sf.setAddressLine(fullAddress);
            sf.setCity(city);
            sf.setState(state);
            sf.setPostalCode(postalCode);
            sf.setCountryCode(countryCode);
            sf.setSourceSystem("Salesforce");

            records.add(sf);
        }

        return records;
    }

    private boolean isBlank(String value)
    {
        return value == null || value.trim().isEmpty();
    }

    private String asString(Object value)
    {
        return value == null ? null : String.valueOf(value);
    }
}