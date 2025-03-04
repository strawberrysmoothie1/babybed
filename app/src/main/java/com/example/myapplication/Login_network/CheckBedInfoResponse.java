package com.example.myapplication.Login_network;

public class CheckBedInfoResponse {
    private boolean success;
    private String message;
    private String bedID;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getBedID() { return bedID; }
}
