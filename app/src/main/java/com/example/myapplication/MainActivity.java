package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.Login_network.LoginClient;
import com.example.myapplication.Login_network.LoginService;
import com.example.myapplication.Login_network.LogoutRequest;
import com.example.myapplication.Login_network.LogoutResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences preferences;
    private LoginService loginService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = getSharedPreferences("AutoLogin", MODE_PRIVATE);

        // 메뉴 버튼 설정
        ImageButton btnMenu = findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(view -> {
            PopupMenu popupMenu = new PopupMenu(MainActivity.this, btnMenu);
            popupMenu.getMenuInflater().inflate(R.menu.menu_main, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.menu_logout) {
                    logout();
                    return true;
                }
                return false;
            });
            popupMenu.show();
        });

        // 실시간 영상 버튼
        Button btnRealTime = findViewById(R.id.btnRealTime);
        btnRealTime.setOnClickListener(view ->
                Toast.makeText(this, "실시간 영상 준비 중입니다.", Toast.LENGTH_SHORT).show()
        );

        // 이벤트 로그 버튼
        Button btnEventLog = findViewById(R.id.btnEventLog);
        btnEventLog.setOnClickListener(view ->
                Toast.makeText(this, "이벤트 로그 준비 중입니다.", Toast.LENGTH_SHORT).show()
        );

        loginService = LoginClient.getClient("http://10.0.2.2:5000/").create(LoginService.class);
    }

    private void logout() {
        // 1. SharedPreferences에서 사용자 아이디 읽기
        String userId = preferences.getString("id", "");
        if (userId.isEmpty()) {
            Toast.makeText(MainActivity.this, "저장된 사용자 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(MainActivity.this, LogActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // 2. 로그아웃 API 호출
        LogoutRequest logoutRequest = new LogoutRequest(userId);
        Call<LogoutResponse> call = loginService.logout(logoutRequest);
        call.enqueue(new Callback<LogoutResponse>() {
            @Override
            public void onResponse(Call<LogoutResponse> call, Response<LogoutResponse> response) {
                if(response.isSuccessful()){
                    Toast.makeText(MainActivity.this, "로그아웃되었습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "서버 오류: " + response.code(), Toast.LENGTH_SHORT).show();
                }
                // 3. API 호출 후 SharedPreferences 정리
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("autoLogin", false);
                editor.remove("id");
                editor.apply();
                // 4. 로그인 화면으로 이동
                Intent intent = new Intent(MainActivity.this, LogActivity.class);
                startActivity(intent);
                finish();
            }
            @Override
            public void onFailure(Call<LogoutResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
