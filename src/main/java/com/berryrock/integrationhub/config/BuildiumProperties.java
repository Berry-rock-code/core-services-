package com.berryrock.integrationhub.config;
// LAYER: PLATFORM -- stays in integration-hub

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed configuration properties for the Buildium integration.
 *
 * Part of the config package — binds the {@code integration.vendor.buildium.*} prefix
 * from {@code application.yml} (or environment variable overrides) to strongly typed
 * fields. Registered as a managed bean via
 * {@link IntegrationConfig#@EnableConfigurationProperties}.
 *
 * All credential fields default to empty strings and must be supplied via environment
 * variables ({@code BUILDIUM_CLIENT_ID}, {@code BUILDIUM_CLIENT_SECRET}) before the
 * Buildium client will function in a non-local environment.
 */
@ConfigurationProperties(prefix = "integration.vendor.buildium")
public class BuildiumProperties
{
    /**
     * Master on/off switch for the Buildium integration.
     * When {@code false}, {@link com.berryrock.integrationhub.client.BuildiumClientImpl}
     * returns empty results without making any API calls.
     */
    private boolean enabled;

    /**
     * Base URL for the Buildium REST API.
     * Defaults to {@code https://api.buildium.com} via the environment variable
     * {@code BUILDIUM_BASE_URL}.
     */
    private String baseUrl;

    /** Buildium API client ID, supplied via {@code BUILDIUM_CLIENT_ID}. */
    private String clientId;

    /** Buildium API client secret, supplied via {@code BUILDIUM_CLIENT_SECRET}. */
    private String clientSecret;

    /**
     * TCP connection timeout in seconds for outbound requests to Buildium.
     * Defaults to 10.
     */
    private int connectTimeoutSeconds;

    /**
     * Read timeout in seconds for outbound requests to Buildium.
     * Defaults to 30.
     */
    private int readTimeoutSeconds;

    /**
     * Number of records to request per paginated API page.
     * Defaults to 100 in {@code application.yml}; overridden to 1000 inside
     * {@link com.berryrock.integrationhub.client.BuildiumClientImpl} for internal pagination.
     */
    private int pageSize;

    /**
     * Returns whether the Buildium integration is enabled.
     *
     * @return {@code true} if Buildium API calls are allowed
     */
    public boolean isEnabled()
    {
        return enabled;
    }

    /**
     * Sets the enabled flag.
     *
     * @param enabled {@code true} to allow Buildium API calls
     */
    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    /**
     * Returns the base URL for the Buildium API.
     *
     * @return base URL string
     */
    public String getBaseUrl()
    {
        return baseUrl;
    }

    /**
     * Sets the base URL.
     *
     * @param baseUrl Buildium API base URL (no trailing slash)
     */
    public void setBaseUrl(String baseUrl)
    {
        this.baseUrl = baseUrl;
    }

    /**
     * Returns the Buildium API client ID.
     *
     * @return client ID string
     */
    public String getClientId()
    {
        return clientId;
    }

    /**
     * Sets the Buildium API client ID.
     *
     * @param clientId Buildium client ID credential
     */
    public void setClientId(String clientId)
    {
        this.clientId = clientId;
    }

    /**
     * Returns the Buildium API client secret.
     *
     * @return client secret string
     */
    public String getClientSecret()
    {
        return clientSecret;
    }

    /**
     * Sets the Buildium API client secret.
     *
     * @param clientSecret Buildium client secret credential
     */
    public void setClientSecret(String clientSecret)
    {
        this.clientSecret = clientSecret;
    }

    /**
     * Returns the TCP connection timeout in seconds.
     *
     * @return connection timeout
     */
    public int getConnectTimeoutSeconds()
    {
        return connectTimeoutSeconds;
    }

    /**
     * Sets the TCP connection timeout.
     *
     * @param connectTimeoutSeconds connection timeout in seconds
     */
    public void setConnectTimeoutSeconds(int connectTimeoutSeconds)
    {
        this.connectTimeoutSeconds = connectTimeoutSeconds;
    }

    /**
     * Returns the read timeout in seconds.
     *
     * @return read timeout
     */
    public int getReadTimeoutSeconds()
    {
        return readTimeoutSeconds;
    }

    /**
     * Sets the read timeout.
     *
     * @param readTimeoutSeconds read timeout in seconds
     */
    public void setReadTimeoutSeconds(int readTimeoutSeconds)
    {
        this.readTimeoutSeconds = readTimeoutSeconds;
    }

    /**
     * Returns the configured page size for Buildium API pagination.
     *
     * @return page size
     */
    public int getPageSize()
    {
        return pageSize;
    }

    /**
     * Sets the page size.
     *
     * @param pageSize number of records to request per page
     */
    public void setPageSize(int pageSize)
    {
        this.pageSize = pageSize;
    }
}
