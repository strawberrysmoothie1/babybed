package com.example.myapplication;

import com.example.myapplication.Login_network.CheckMyBedResponse;
import com.example.myapplication.Login_network.LoginService;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;

public class CheckList {
    private LoginService loginService;

    public CheckList(LoginService loginService) {
        this.loginService = loginService;
    }

    // CheckMyBed 메서드: gdID에 해당하는 bedID 목록을 조회합니다.
    public void CheckMyBed(String gdID, Callback<CheckMyBedResponse> callback) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("gdID", gdID);
        loginService.checkMyBed(requestBody).enqueue(callback);
    }
}
