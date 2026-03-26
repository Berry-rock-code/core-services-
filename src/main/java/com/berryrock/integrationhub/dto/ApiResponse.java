package com.berryrock.integrationhub.dto;

import java.time.Instant;

/**
 * Generic wrapper for successful API responses.
 *
 * All successful REST endpoints in this service return their payloads wrapped in this
 * envelope. The {@code status} field is always {@code "SUCCESS"} for non-error responses,
 * and {@code timestamp} is set automatically to the moment the response object is created.
 *
 * @param <T> the type of the payload carried in the {@code data} field
 */
public class ApiResponse<T>
{
    /** Always {@code "SUCCESS"} for responses produced by the {@link #success} factory method. */
    private String status;

    /** The actual response payload. */
    private T data;

    /** Server-side timestamp at the moment the response was created. */
    private Instant timestamp;

    /** Constructs an empty response (for deserialization). */
    public ApiResponse()
    {
    }

    /**
     * Constructs a response with the given status and payload, setting the timestamp to now.
     *
     * @param status status label (e.g., {@code "SUCCESS"})
     * @param data   response payload
     */
    public ApiResponse(String status, T data)
    {
        this.status = status;
        this.data = data;
        this.timestamp = Instant.now();
    }

    /**
     * Factory method that creates a {@code "SUCCESS"} response wrapping the given payload.
     *
     * @param data response payload
     * @param <T>  payload type
     * @return a new {@code ApiResponse} with {@code status = "SUCCESS"} and the current timestamp
     */
    public static <T> ApiResponse<T> success(T data)
    {
        return new ApiResponse<>("SUCCESS", data);
    }

    /**
     * Returns the status label for this response.
     *
     * @return status string
     */
    public String getStatus()
    {
        return status;
    }

    /**
     * Sets the status label.
     *
     * @param status status string
     */
    public void setStatus(String status)
    {
        this.status = status;
    }

    /**
     * Returns the response payload.
     *
     * @return payload of type {@code T}
     */
    public T getData()
    {
        return data;
    }

    /**
     * Sets the response payload.
     *
     * @param data payload of type {@code T}
     */
    public void setData(T data)
    {
        this.data = data;
    }

    /**
     * Returns the server-side timestamp at which this response was created.
     *
     * @return creation timestamp
     */
    public Instant getTimestamp()
    {
        return timestamp;
    }

    /**
     * Sets the creation timestamp.
     *
     * @param timestamp timestamp to record
     */
    public void setTimestamp(Instant timestamp)
    {
        this.timestamp = timestamp;
    }
}
