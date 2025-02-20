package com.example.myapplication.Login_network;

public class TestNotificationRequest {
    private String fcmToken;

    public TestNotificationRequest(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public String getFcmToken() {
        return fcmToken;
    }
}
