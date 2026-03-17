package com.berryrock.integrationhub.dto;

import java.time.Instant;
import java.util.List;

public class ErrorResponse {
    private String status;
    private String message;
    private String code;
    private List<String> details;
    private Instant timestamp;

    public ErrorResponse() {
    }

    public ErrorResponse(String message, String code, List<String> details) {
        this.status = "ERROR";
        this.message = message;
        this.code = code;
        this.details = details;
        this.timestamp = Instant.now();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public List<String> getDetails() {
        return details;
    }

    public void setDetails(List<String> details) {
        this.details = details;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
