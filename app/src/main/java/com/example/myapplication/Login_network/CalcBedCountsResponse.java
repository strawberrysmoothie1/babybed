package com.example.myapplication.Login_network;

import java.util.List;
import com.example.myapplication.model.BedCount;

public class CalcBedCountsResponse {
    private boolean success;
    private String message;
    private List<BedCount> bedCounts;

    public boolean isSuccess() {
        return success;
    }
    public String getMessage() {
        return message;
    }
    public List<BedCount> getBedCounts() {
        return bedCounts;
    }
}
