package com.berryrock.integrationhub.dto;
// LAYER: PLATFORM -- stays in integration-hub

import java.time.Instant;
import java.util.List;

/**
 * Structured error response returned by the API when a request fails.
 *
 * Produced by {@link com.berryrock.integrationhub.controller.GlobalExceptionHandler} for
 * validation errors, illegal argument exceptions, and any uncaught runtime exceptions.
 * The {@code code} field is a machine-readable error token that clients can switch on;
 * the {@code details} list carries field-level validation messages when applicable.
 */
public class ErrorResponse
{
    /** Always {@code "ERROR"} for responses produced by the exception handler. */
    private String status;

    /** Human-readable description of the failure. */
    private String message;

    /**
     * Machine-readable error code.
     * Known values: {@code ERR_VALIDATION}, {@code ERR_BAD_REQUEST}, {@code ERR_INTERNAL_SERVER_ERROR}.
     */
    private String code;

    /**
     * Additional details, typically field-level validation errors in the form
     * {@code "fieldName: constraint message"}.
     */
    private List<String> details;

    /** Server-side timestamp at the moment the error response was created. */
    private Instant timestamp;

    /** Constructs an empty error response (for deserialization). */
    public ErrorResponse()
    {
    }

    /**
     * Constructs a fully populated error response, setting {@code status = "ERROR"} and
     * the timestamp to now.
     *
     * @param message human-readable failure description
     * @param code    machine-readable error token
     * @param details field-level detail messages; may be an empty list but not {@code null}
     */
    public ErrorResponse(String message, String code, List<String> details)
    {
        this.status = "ERROR";
        this.message = message;
        this.code = code;
        this.details = details;
        this.timestamp = Instant.now();
    }

    /**
     * Returns the status label.
     *
     * @return always {@code "ERROR"}
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
     * Returns the human-readable error description.
     *
     * @return error message
     */
    public String getMessage()
    {
        return message;
    }

    /**
     * Sets the human-readable error description.
     *
     * @param message error message
     */
    public void setMessage(String message)
    {
        this.message = message;
    }

    /**
     * Returns the machine-readable error code.
     *
     * @return error code token
     */
    public String getCode()
    {
        return code;
    }

    /**
     * Sets the machine-readable error code.
     *
     * @param code error code token
     */
    public void setCode(String code)
    {
        this.code = code;
    }

    /**
     * Returns the list of detail messages.
     *
     * @return detail messages; never {@code null} when constructed via the primary constructor
     */
    public List<String> getDetails()
    {
        return details;
    }

    /**
     * Sets the detail messages.
     *
     * @param details field-level validation messages or other supplemental details
     */
    public void setDetails(List<String> details)
    {
        this.details = details;
    }

    /**
     * Returns the server-side creation timestamp.
     *
     * @return timestamp at the moment the error response was created
     */
    public Instant getTimestamp()
    {
        return timestamp;
    }

    /**
     * Sets the creation timestamp.
     *
     * @param timestamp error response creation time
     */
    public void setTimestamp(Instant timestamp)
    {
        this.timestamp = timestamp;
    }
}
