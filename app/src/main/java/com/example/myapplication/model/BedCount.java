package com.example.myapplication.model;

public class BedCount {
    private String bedID;
    private String designation;
    private int guardianCount;
    private int tempCount;
    private String serialNumber;
    private String period;
    private int remainingDays;

    public String getBedID() {
        return bedID;
    }
    public String getDesignation() {
        return designation;
    }
    public int getGuardianCount() {
        return guardianCount;
    }
    public int getTempCount() {
        return tempCount;
    }
    public String getSerialNumber() {
        return serialNumber;
    }
    public String getPeriod() {
        return period;
    }
    public int getRemainingDays() {
        return remainingDays;
    }
}
