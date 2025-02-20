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

        // 자동 로그인 체크
        if (preferences.getBoolean("autoLogin", false)) {
            String savedId = preferences.getString("id", "");
            if (!savedId.isEmpty()) {
                Toast.makeText(this, "자동 로그인 중...", Toast.LENGTH_SHORT).show();
                navigateToMain();
            }
        }

        // 로그인 버튼 클릭 이벤트
        btnLogin.setOnClickListener(view -> {
            String id = etId.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (id.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "아이디/비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 자동 로그인 여부를 체크박스에서 가져와 LoginRequest에 포함시킵니다.
            LoginRequest request = new LoginRequest(id, password, cbAutoLogin.isChecked());
            Call<LoginResponse> call = loginService.login(request);
            call.enqueue(new Callback<LoginResponse>() {
                @Override
                public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        LoginResponse result = response.body();
                        if (result.isSuccess()) {
                            handleLoginSuccess(id, cbAutoLogin.isChecked());
                            Toast.makeText(LogActivity.this, "로그인 성공!", Toast.LENGTH_SHORT).show();

                            // 자동 로그인 설정 저장
                            SharedPreferences.Editor editor = preferences.edit();
                            if (cbAutoLogin.isChecked()) {
                                editor.putBoolean("autoLogin", true);
                                editor.putString("id", id);
                            } else {
                                editor.putBoolean("autoLogin", false);
                                editor.remove("id");
                            }
                            editor.apply();

                            navigateToMain();
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

    private void handleLoginSuccess(String id, boolean autoLoginChecked) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("lastLoggedInId", id);
        if (autoLoginChecked) {
            editor.putBoolean("autoLogin", true);
            editor.putString("id", id);
        } else {
            editor.putBoolean("autoLogin", false);
            editor.remove("id");
        }
        editor.apply();
        navigateToMain();
    }

    private void navigateToMain() {
        Intent intent = new Intent(LogActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
