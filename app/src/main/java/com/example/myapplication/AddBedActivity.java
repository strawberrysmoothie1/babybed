package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.Login_network.CalcBedCountsResponse;
import com.example.myapplication.Login_network.CheckMyBedResponse;
import com.example.myapplication.Login_network.LoginClient;
import com.example.myapplication.Login_network.LoginService;
import com.example.myapplication.Login_network.LogoutRequest;
import com.example.myapplication.Login_network.LogoutResponse;
import com.example.myapplication.item.BedAdapter;
import com.example.myapplication.model.BedCount;
import com.example.myapplication.model.BedDisplay;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddBedActivity extends AppCompatActivity {
    private SharedPreferences preferences;
    private LoginService loginService;
    private RecyclerView recyclerViewBeds;
    private BedAdapter bedAdapter;
    private TextView tvInfoCounts, tvInfoAdditional;
    private TextView tvAddBedInstruction;

    // calcBedCounts 결과 저장
    private List<BedCount> calcBedCountsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_addbed);

        preferences = getSharedPreferences("AutoLogin", MODE_PRIVATE);
        loginService = LoginClient.getClient("http://10.0.2.2:5000/").create(LoginService.class);

        Toolbar toolbar = findViewById(R.id.toolbarAddBed);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        ImageButton btnMenu = findViewById(R.id.btnMenuAddBed);
        btnMenu.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(AddBedActivity.this, btnMenu);
            popupMenu.getMenuInflater().inflate(R.menu.menu_main, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.menu_logout) {
                    logout();
                    return true;
                } else if (item.getItemId() == R.id.menu_account) {
                    Toast.makeText(AddBedActivity.this, "계정 관리 선택", Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            });
            popupMenu.show();
        });

        recyclerViewBeds = findViewById(R.id.recyclerViewBeds);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerViewBeds.setLayoutManager(layoutManager);

        // 좌우 패딩 설정 (아이템 크기 160dp 기준)
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int itemWidthPx = (int) (160 * dm.density);
        int screenWidth = dm.widthPixels;
        int sidePadding = (screenWidth - itemWidthPx) / 2;
        recyclerViewBeds.setPadding(sidePadding, 0, sidePadding, 0);
        recyclerViewBeds.setClipToPadding(false);

        LinearSnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(recyclerViewBeds);

        recyclerViewBeds.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                // 스크롤이 멈추면 중앙 아이템에 맞춰 info frame 업데이트
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    View centerView = snapHelper.findSnapView(layoutManager);
                    if (centerView != null) {
                        int pos = layoutManager.getPosition(centerView);
                        updateFixedInfo(pos);
                    }
                }
            }
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                int centerX = recyclerView.getWidth() / 2;
                for (int i = 0; i < recyclerView.getChildCount(); i++) {
                    int childCenterX = (recyclerView.getChildAt(i).getLeft() + recyclerView.getChildAt(i).getRight()) / 2;
                    int distanceFromCenter = Math.abs(centerX - childCenterX);
                    float scale = 1.0f - (distanceFromCenter / (float) centerX) * 0.5f;
                    if (scale < 0.5f) scale = 0.5f;
                    recyclerView.getChildAt(i).setScaleX(scale);
                    recyclerView.getChildAt(i).setScaleY(scale);
                }
            }
        });

        tvInfoCounts = findViewById(R.id.tvInfoCounts);
        tvInfoAdditional = findViewById(R.id.tvInfoAdditional);
        tvAddBedInstruction = findViewById(R.id.tvAddBedInstruction);

        final String tempId = preferences.getString("id", "");
        final String userId = (tempId == null || tempId.isEmpty()) ? preferences.getString("lastLoggedInId", "") : tempId;

        // 기존 checkMyBed 호출 (버튼 구성에 사용)
        CheckList checkList = new CheckList(loginService);
        checkList.CheckMyBed(userId, new Callback<CheckMyBedResponse>() {
            @Override
            public void onResponse(Call<CheckMyBedResponse> call, Response<CheckMyBedResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<List<String>> rawData = response.body().getBedInfo();
                    System.out.println("GuardBed bedInfo: " + rawData);
                    List<BedDisplay> bedDisplayList = groupBedData(rawData, userId);
                    bedAdapter = new BedAdapter(AddBedActivity.this, bedDisplayList, userId, loginService);
                    recyclerViewBeds.setAdapter(bedAdapter);
                    tvAddBedInstruction.setVisibility(TextView.GONE);
                    recyclerViewBeds.post(() -> recyclerViewBeds.smoothScrollToPosition(0));
                    // 초기 중앙 아이템 정보 업데이트
                    recyclerViewBeds.post(() -> {
                        View centerView = snapHelper.findSnapView(layoutManager);
                        if (centerView != null) {
                            int pos = layoutManager.getPosition(centerView);
                            updateFixedInfo(pos);
                        }
                    });
                } else {
                    tvAddBedInstruction.setText("침대추가");
                }
            }
            @Override
            public void onFailure(Call<CheckMyBedResponse> call, Throwable t) {
                tvAddBedInstruction.setText("침대추가");
                Toast.makeText(AddBedActivity.this, "CheckMyBed 조회 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // 새 calcBedCounts 호출 (백엔드에서는 GdID를 무시함)
        Map<String, String> calcRequest = new HashMap<>();
        calcRequest.put("gdID", userId);
        loginService.calcBedCounts(calcRequest).enqueue(new Callback<CalcBedCountsResponse>() {
            @Override
            public void onResponse(Call<CalcBedCountsResponse> call, Response<CalcBedCountsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    calcBedCountsList = response.body().getBedCounts();
                } else {
                    Toast.makeText(AddBedActivity.this, "calcBedCounts 호출 실패", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<CalcBedCountsResponse> call, Throwable t) {
                Toast.makeText(AddBedActivity.this, "calcBedCounts 네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // rawData를 BedDisplay 객체 리스트로 그룹화 (checkMyBed 응답을 기반으로)
    private List<BedDisplay> groupBedData(List<List<String>> rawData, String currentUserId) {
        Map<String, List<List<String>>> groupMap = new HashMap<>();
        if (rawData != null) {
            for (List<String> row : rawData) {
                String bedID = row.get(1);
                if (!groupMap.containsKey(bedID)) {
                    groupMap.put(bedID, new ArrayList<>());
                }
                groupMap.get(bedID).add(row);
            }
        }
        List<BedDisplay> list = new ArrayList<>();
        DateTimeFormatter formatter = null;
        LocalDate today = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            today = LocalDate.now();
        }
        for (String bedID : groupMap.keySet()) {
            List<List<String>> groupRows = groupMap.get(bedID);
            int guardianCount = 0;
            int tempCount = 0;
            String userRole = "";
            String designation = groupRows.get(0).get(2);
            String serialNumber = groupRows.get(0).get(5);
            String periodForDisplay = "";
            int remainingDays = 0;
            for (List<String> row : groupRows) {
                String period = row.get(3);
                if (period == null || period.isEmpty() || period.equalsIgnoreCase("null")) {
                    guardianCount++;
                    if (row.get(0).equals(currentUserId)) {
                        userRole = "guardian";
                    }
                } else {
                    tempCount++;
                    if (row.get(0).equals(currentUserId)) {
                        userRole = "temp";
                        periodForDisplay = period;
                        if (today != null && formatter != null) {
                            try {
                                LocalDate periodDate = null;
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    periodDate = LocalDate.parse(period, formatter);
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    remainingDays = (int) ChronoUnit.DAYS.between(today, periodDate);
                                }
                            } catch (Exception e) {
                                remainingDays = 0;
                            }
                        }
                    }
                }
            }
            list.add(new BedDisplay(bedID, designation, guardianCount, tempCount, userRole, serialNumber, periodForDisplay, remainingDays));
        }
        // 마지막에 항상 "침대추가" 항목 추가
        list.add(new BedDisplay("", "침대추가", 0, 0, "", "", "", 0));
        return list;
    }

    // 중앙 아이템에 따른 고정 정보 프레임 업데이트
    private void updateFixedInfo(int pos) {
        if (bedAdapter != null && pos >= 0 && pos < bedAdapter.getItemCount()) {
            BedDisplay bed = bedAdapter.getBedDisplayAt(pos);
            View fixedFrame = findViewById(R.id.fixedInfoFrame);
            TextView tvRoleIndicator = findViewById(R.id.tvRoleIndicator);
            if ("침대추가".equals(bed.getDesignation())) {
                fixedFrame.setVisibility(View.GONE);
            } else {
                fixedFrame.setVisibility(View.VISIBLE);
                if ("temp".equals(bed.getUserRole())) {
                    tvRoleIndicator.setText("임시보호 침대");
                    tvRoleIndicator.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    tvRoleIndicator.setVisibility(View.VISIBLE);
                } else if ("guardian".equals(bed.getUserRole())) {
                    tvRoleIndicator.setText("보호 침대");
                    tvRoleIndicator.setTextColor(getResources().getColor(android.R.color.black));
                    tvRoleIndicator.setVisibility(View.VISIBLE);
                } else {
                    tvRoleIndicator.setVisibility(View.GONE);
                }

                BedCount matchingCount = null;
                if (calcBedCountsList != null) {
                    for (BedCount bc : calcBedCountsList) {
                        if (bc.getBedID().equals(bed.getBedID())) {
                            matchingCount = bc;
                            break;
                        }
                    }
                }
                if (matchingCount != null) {
                    String countsText = "보호자: " + matchingCount.getGuardianCount() + "/3" + "\n" +
                            "임시보호자: " + matchingCount.getTempCount() + "/5";
                    ((TextView) findViewById(R.id.tvInfoCounts)).setText(countsText);
                } else {
                    ((TextView) findViewById(R.id.tvInfoCounts)).setText("");
                }

                TextView tvAdditional = findViewById(R.id.tvInfoAdditional);
                if ("temp".equals(bed.getUserRole())) {
                    int n = (matchingCount != null) ? matchingCount.getRemainingDays() : bed.getRemainingDays();
                    tvAdditional.setText("임시보호까지 " + n + "일 남음");
                    if (n <= 3) {
                        tvAdditional.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    } else {
                        tvAdditional.setTextColor(getResources().getColor(android.R.color.black));
                    }
                } else if ("guardian".equals(bed.getUserRole())) {
                    String serial = (matchingCount != null) ? matchingCount.getSerialNumber() : bed.getSerialNumber();
                    tvAdditional.setText("일련번호: " + serial);
                    tvAdditional.setTextColor(getResources().getColor(android.R.color.black));
                } else {
                    tvAdditional.setText("");
                }
            }
        }
    }

    private void logout() {
        String userId = preferences.getString("id", "");
        if (userId.isEmpty()) {
            Toast.makeText(AddBedActivity.this, "저장된 사용자 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(AddBedActivity.this, LogActivity.class));
            finish();
            return;
        }
        LogoutRequest logoutRequest = new LogoutRequest(userId);
        Call<LogoutResponse> call = loginService.logout(logoutRequest);
        call.enqueue(new Callback<LogoutResponse>() {
            @Override
            public void onResponse(Call<LogoutResponse> call, Response<LogoutResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AddBedActivity.this, "로그아웃되었습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(AddBedActivity.this, "서버 오류: " + response.code(), Toast.LENGTH_SHORT).show();
                }
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("autoLogin", false);
                editor.remove("id");
                editor.apply();
                startActivity(new Intent(AddBedActivity.this, LogActivity.class));
                finish();
            }
            @Override
            public void onFailure(Call<LogoutResponse> call, Throwable t) {
                Toast.makeText(AddBedActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
