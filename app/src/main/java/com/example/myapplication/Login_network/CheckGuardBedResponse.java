package com.example.myapplication.Login_network;

public class CheckGuardBedResponse {
    private boolean success;
    private boolean exists;
    private String message;

    public boolean isSuccess() {
        return success;
    }

    public boolean isExists() {
        return exists;
    }

    public String getMessage() {
        return message;
    }
}
