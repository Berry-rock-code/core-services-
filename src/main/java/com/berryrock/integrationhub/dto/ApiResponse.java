package com.berryrock.integrationhub.dto;

import java.time.Instant;

public class ApiResponse<T> {
    private String status;
    private T data;
    private Instant timestamp;

    public ApiResponse() {
    }

    public ApiResponse(String status, T data) {
        this.status = status;
        this.data = data;
        this.timestamp = Instant.now();
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("SUCCESS", data);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
