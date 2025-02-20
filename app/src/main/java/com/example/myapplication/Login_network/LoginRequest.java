package com.example.myapplication.Login_network;

public class LoginRequest {
    private String gdID;
    private String password;
    private boolean autoLogin;

    public LoginRequest(String gdID, String password, boolean autoLogin) {
        this.gdID = gdID;
        this.password = password;
        this.autoLogin = autoLogin;
    }

    public String getGdID() {
        return gdID;
    }

    public String getPassword() {
        return password;
    }

    public boolean isAutoLogin() {
        return autoLogin;
    }
}
