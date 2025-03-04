package com.example.myapplication.Login_network;

import com.example.myapplication.RegisterActivity;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface LoginService {

    @POST("/api/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("/api/register")
    Call<RegisterResponse> register(@Body RegisterRequest request);

    @POST("/api/logout")
    Call<LogoutResponse> logout(@Body LogoutRequest request);

    @POST("/api/sendTestNotification")
    Call<TestNotificationResponse> sendTestNotification(@Body TestNotificationRequest request);

    @POST("/api/checkDuplicate")
    Call<RegisterActivity.CheckDuplicateResponse> checkDuplicate(@Body Map<String, String> body);

    @POST("/api/checkMyBed")
    Call<CheckMyBedResponse> checkMyBed(@Body Map<String, String> body);

    @POST("/api/checkBedInfo")
    Call<CheckBedInfoResponse> checkBedInfo(@Body Map<String, String> body);

    @POST("/api/addGuardBed")
    Call<AddGuardBedResponse> addGuardBed(@Body Map<String, String> body);

    @POST("/api/deleteBed")
    Call<Void> deleteBed(@Body Map<String, String> body);

    @POST("/api/calcBedCounts")
    Call<CalcBedCountsResponse> calcBedCounts(@Body Map<String, String> body);

}
