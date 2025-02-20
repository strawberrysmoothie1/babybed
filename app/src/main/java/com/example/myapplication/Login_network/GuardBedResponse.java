package com.example.myapplication.Login_network;

public class GuardBedResponse {
    private boolean success;      // 해당 사용자의 GuardBed 레코드가 있는지 여부
    private String designation;   // 일치하는 레코드의 designation 값

    public boolean isSuccess() {
        return success;
    }

    public String getDesignation() {
        return designation;
    }
}
