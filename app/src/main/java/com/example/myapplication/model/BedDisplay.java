package com.example.myapplication.model;

public class BedDisplay {
    private String bedID;
    private String designation;
    private int guardianCount;
    private int tempCount;
    private String userRole; // "guardian", "temp" 또는 ""
    private String serialNumber;
    private String period; // 임시보호자 표시용
    private int remainingDays; // 임시보호 기간까지 남은 일수
    private int bedOrder; // 추가: bed_order 값

    // 생성자에 bedOrder 추가
    public BedDisplay(String bedID, String designation, int guardianCount, int tempCount,
                      String userRole, String serialNumber, String period, int remainingDays, int bedOrder) {
        this.bedID = bedID;
        this.designation = designation;
        this.guardianCount = guardianCount;
        this.tempCount = tempCount;
        this.userRole = userRole;
        this.serialNumber = serialNumber;
        this.period = period;
        this.remainingDays = remainingDays;
        this.bedOrder = bedOrder;
    }

    // 기존 생성자 없이, 또는 필요에 따라 오버로딩 가능
    // getter들...
    public String getBedID() { return bedID; }
    public String getDesignation() { return designation; }
    public int getGuardianCount() { return guardianCount; }
    public int getTempCount() { return tempCount; }
    public String getUserRole() { return userRole; }
    public String getSerialNumber() { return serialNumber; }
    public String getPeriod() { return period; }
    public int getRemainingDays() { return remainingDays; }
    public int getBedOrder() { return bedOrder; }
}
