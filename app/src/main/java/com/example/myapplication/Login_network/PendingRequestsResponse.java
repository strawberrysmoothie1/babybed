package com.example.myapplication.Login_network;

import java.util.List;

public class PendingRequestsResponse {
    private boolean success;
    private List<PendingRequest> requests;
    private String message;

    public boolean isSuccess() {
        return success;
    }
    public List<PendingRequest> getRequests() {
        return requests;
    }
    public String getMessage() {
        return message;
    }
}