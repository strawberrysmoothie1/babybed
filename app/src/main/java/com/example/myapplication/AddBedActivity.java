package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.Login_network.CheckMyBedResponse;
import com.example.myapplication.Login_network.LoginClient;
import com.example.myapplication.Login_network.LoginService;
import com.example.myapplication.Login_network.LogoutRequest;
import com.example.myapplication.Login_network.LogoutResponse;
import com.example.myapplication.item.BedAdapter;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddBedActivity extends AppCompatActivity {
    private SharedPreferences preferences;
    private LoginService loginService;
    private RecyclerView recyclerViewBeds;
    private BedAdapter bedAdapter;

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
        toolbar.setNavigationOnClickListener(view -> finish());

        ImageButton btnMenu = findViewById(R.id.btnMenuAddBed);
        btnMenu.setOnClickListener(view -> {
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

        // 좌우 패딩 설정: 고정된 아이템 크기를 160dp로 가정
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

        String userId = preferences.getString("id", "");
        if (userId == null || userId.isEmpty()) {
            userId = preferences.getString("lastLoggedInId", "");
        }
        CheckList checkList = new CheckList(loginService);
        checkList.CheckMyBed(userId, new Callback<CheckMyBedResponse>() {
            @Override
            public void onResponse(Call<CheckMyBedResponse> call, Response<CheckMyBedResponse> response) {
                TextView tvInstruction = findViewById(R.id.tvAddBedInstruction);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<List<String>> bedInfo = response.body().getBedInfo();
                    System.out.println("GuardBed bedInfo: " + bedInfo);
                    if (bedInfo == null || bedInfo.isEmpty()) {
                        tvInstruction.setText("침대추가");
                    } else {
                        bedAdapter = new BedAdapter(AddBedActivity.this, bedInfo);
                        recyclerViewBeds.setAdapter(bedAdapter);
                        tvInstruction.setVisibility(TextView.GONE);
                        recyclerViewBeds.post(() -> recyclerViewBeds.smoothScrollToPosition(0));
                    }
                } else {
                    tvInstruction.setText("침대추가");
                }
            }

            @Override
            public void onFailure(Call<CheckMyBedResponse> call, Throwable t) {
                TextView tvInstruction = findViewById(R.id.tvAddBedInstruction);
                tvInstruction.setText("침대추가");
                Toast.makeText(AddBedActivity.this, "CheckMyBed 조회 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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
