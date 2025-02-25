package com.example.myapplication.Login_network;

import java.util.List;

public class CheckMyBedResponse {
    private boolean success;
    private String message;
    // 각 행은 [GdID, bedID, designation, period, bed_order] 형태의 문자열 리스트
    private List<List<String>> bedInfo;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public List<List<String>> getBedInfo() {
        return bedInfo;
    }
}
