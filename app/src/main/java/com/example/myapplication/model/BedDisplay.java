package com.example.myapplication.model;

public class BedDisplay {
    private String bedID;
    private String designation;
    private int guardianCount;
    private int tempCount;
    private String userRole; // "guardian" 또는 "temp" (현재 사용자의 역할, 없으면 빈 문자열)
    private String serialNumber; // 보호자일 경우
    private String period; // 임시보호자일 경우
    private int remainingDays; // 임시보호자일 경우

    public BedDisplay(String bedID, String designation, int guardianCount, int tempCount,
                      String userRole, String serialNumber, String period, int remainingDays) {
        this.bedID = bedID;
        this.designation = designation;
        this.guardianCount = guardianCount;
        this.tempCount = tempCount;
        this.userRole = userRole;
        this.serialNumber = serialNumber;
        this.period = period;
        this.remainingDays = remainingDays;
    }
    // Getters and setters...
    public String getBedID() { return bedID; }
    public String getDesignation() { return designation; }
    public int getGuardianCount() { return guardianCount; }
    public int getTempCount() { return tempCount; }
    public String getUserRole() { return userRole; }
    public String getSerialNumber() { return serialNumber; }
    public String getPeriod() { return period; }
    public int getRemainingDays() { return remainingDays; }
}
