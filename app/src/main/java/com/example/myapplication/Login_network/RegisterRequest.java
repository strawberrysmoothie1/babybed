package com.example.myapplication.Login_network;

public class RegisterRequest {
    private String gdID;
    private String password;
    private String fcmToken;

    public RegisterRequest(String gdID, String password, String fcmToken) {
        this.gdID = gdID;
        this.password = password;
        this.fcmToken = fcmToken;
    }

    public String getGdID() {
        return gdID;
    }

    public String getPassword() {
        return password;
    }

    public String getFcmToken() {
        return fcmToken;
    }
}
