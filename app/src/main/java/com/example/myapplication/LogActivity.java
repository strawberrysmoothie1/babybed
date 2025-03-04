package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.Login_network.LoginClient;
import com.example.myapplication.Login_network.LoginRequest;
import com.example.myapplication.Login_network.LoginResponse;
import com.example.myapplication.Login_network.LoginService;
import java.util.HashMap;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LogActivity extends AppCompatActivity {

    private LoginService loginService;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        preferences = getSharedPreferences("AutoLogin", MODE_PRIVATE);
        // 자동로그인 체크: autoLogin 플래그와 "id"가 저장되어 있으면 바로 다음 화면으로 이동
        if (preferences.getBoolean("autoLogin", false)) {
            String savedId = preferences.getString("id", "");
            if (!savedId.isEmpty()) {
                Toast.makeText(this, "자동 로그인 중...", Toast.LENGTH_SHORT).show();
                navigateToAddBed();
                return;  // 로그인 화면을 더 이상 표시하지 않음
            }
        }
        // View 요소
        EditText etId = findViewById(R.id.etId);
        EditText etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        CheckBox cbAutoLogin = findViewById(R.id.cbAutoLogin);
        Button btnRegister = findViewById(R.id.btnRegister);

        // Retrofit 초기화
        loginService = LoginClient.getClient("http://10.0.2.2:5000/").create(LoginService.class);

        // SharedPreferences 초기화
        preferences = getSharedPreferences("AutoLogin", MODE_PRIVATE);

        // 로그인 버튼 클릭 이벤트
        btnLogin.setOnClickListener(view -> {
            String id = etId.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (id.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "아이디/비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 로그인 요청 (자동 로그인 여부 포함)
            LoginRequest request = new LoginRequest(id, password, cbAutoLogin.isChecked());
            Call<LoginResponse> call = loginService.login(request);
            call.enqueue(new Callback<LoginResponse>() {
                @Override
                public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        LoginResponse result = response.body();
                        if (result.isSuccess()) {
                            // 자동 로그인 설정 저장 및 SharedPreferences 업데이트
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putString("lastLoggedInId", id);
                            if (cbAutoLogin.isChecked()) {
                                editor.putBoolean("autoLogin", true);
                                editor.putString("id", id);
                            } else {
                                editor.putBoolean("autoLogin", false);
                                editor.remove("id");
                            }
                            editor.apply();

                            Toast.makeText(LogActivity.this, "로그인 성공!", Toast.LENGTH_SHORT).show();
                            // 로그인 성공하면 바로 AddBedActivity로 이동
                            navigateToAddBed();
                        } else {
                            Toast.makeText(LogActivity.this, result.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        if (response.code() == 401) {
                            Toast.makeText(LogActivity.this, "아이디 또는 비밀번호가 틀립니다.", Toast.LENGTH_SHORT).show();
                        } else if (response.code() == 403) {
                            Toast.makeText(LogActivity.this, "승인 대기 중입니다.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(LogActivity.this, "서버 오류: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onFailure(Call<LoginResponse> call, Throwable t) {
                    Toast.makeText(LogActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        // 회원가입 버튼 클릭 이벤트
        btnRegister.setOnClickListener(view -> {
            Intent intent = new Intent(LogActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void navigateToAddBed() {
        Intent intent = new Intent(LogActivity.this, AddBedActivity.class);
        startActivity(intent);
        finish();
    }
}
