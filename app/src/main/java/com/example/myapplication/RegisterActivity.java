package com.example.myapplication;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.myapplication.Login_network.LoginClient;
import com.example.myapplication.Login_network.LoginService;
import com.example.myapplication.Login_network.RegisterRequest;
import com.example.myapplication.Login_network.RegisterResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText etRegisterId, etRegisterPassword, etRegisterPasswordConfirm;
    private Button btnCheckId, btnRegisterSubmit;
    private TextView tvErrorMessage;
    private boolean isIdChecked = false;
    private LoginService loginService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        Toolbar toolbar = findViewById(R.id.toolbarRegister);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etRegisterId = findViewById(R.id.etRegisterId);
        etRegisterPassword = findViewById(R.id.etRegisterPassword);
        etRegisterPasswordConfirm = findViewById(R.id.etRegisterPasswordConfirm);
        btnCheckId = findViewById(R.id.btnCheckId);
        btnRegisterSubmit = findViewById(R.id.btnRegisterSubmit);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);

        loginService = LoginClient.getClient("http://10.0.2.2:5000/").create(LoginService.class);

        // 아이디 중복확인 버튼 클릭
        btnCheckId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String id = etRegisterId.getText().toString().trim();
                if (id.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "아이디를 입력하세요.", Toast.LENGTH_SHORT).show();
                    isIdChecked = false;
                    return;
                }
                // 중복확인을 위해 Map으로 요청 데이터 구성
                Map<String, String> duplicateRequest = new HashMap<>();
                duplicateRequest.put("gdID", id);

                Call<CheckDuplicateResponse> call = loginService.checkDuplicate(duplicateRequest);
                call.enqueue(new Callback<CheckDuplicateResponse>() {
                    @Override
                    public void onResponse(Call<CheckDuplicateResponse> call, Response<CheckDuplicateResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            CheckDuplicateResponse resp = response.body();
                            if (resp.isAvailable()) {
                                Toast.makeText(RegisterActivity.this, "사용 가능한 아이디입니다.", Toast.LENGTH_SHORT).show();
                                isIdChecked = true;
                            } else {
                                Toast.makeText(RegisterActivity.this, "이미 존재하는 아이디입니다.", Toast.LENGTH_SHORT).show();
                                isIdChecked = false;
                            }
                        } else {
                            Toast.makeText(RegisterActivity.this, "서버 오류: " + response.code(), Toast.LENGTH_SHORT).show();
                            isIdChecked = false;
                        }
                    }

                    @Override
                    public void onFailure(Call<CheckDuplicateResponse> call, Throwable t) {
                        Toast.makeText(RegisterActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        isIdChecked = false;
                    }
                });
            }
        });

        // 회원가입 버튼 클릭
        btnRegisterSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                tvErrorMessage.setVisibility(View.GONE);
                final String id = etRegisterId.getText().toString().trim();
                final String password = etRegisterPassword.getText().toString().trim();
                String passwordConfirm = etRegisterPasswordConfirm.getText().toString().trim();

                if (id.isEmpty() || password.isEmpty() || passwordConfirm.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "모든 필드를 입력해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!isIdChecked) {
                    Toast.makeText(RegisterActivity.this, "아이디 중복확인을 먼저 진행해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (password.length() < 8) {
                    tvErrorMessage.setText("비밀번호는 8자리 이상이어야 합니다.");
                    tvErrorMessage.setVisibility(View.VISIBLE);
                    return;
                }
                if (!password.equals(passwordConfirm)) {
                    tvErrorMessage.setText("비밀번호와 확인이 일치하지 않습니다.");
                    tvErrorMessage.setVisibility(View.VISIBLE);
                    return;
                }

                // 비밀번호가 유효하면 Firebase에서 FCM 토큰을 받아옴
                FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w("FCM", "토큰 가져오기 실패", task.getException());
                            Toast.makeText(RegisterActivity.this, "FCM 토큰 가져오기 실패", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String fcmToken = task.getResult();
                        Log.d("FCM", "FCM 토큰: " + fcmToken);
                        // 서버에 회원가입 요청 (서버에서는 가입 날짜를 저장함)
                        registerUserOnServer(id, password, fcmToken);
                    }
                });
            }
        });
    }

    private void registerUserOnServer(String id, String password, String fcmToken) {
        RegisterRequest request = new RegisterRequest(id, password, fcmToken);
        Call<RegisterResponse> call = loginService.register(request);
        call.enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    RegisterResponse result = response.body();
                    if (result.isSuccess()) {
                        Toast.makeText(RegisterActivity.this, "회원가입 성공!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(RegisterActivity.this, "회원가입 실패: " + result.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(RegisterActivity.this, "서버 오류: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<RegisterResponse> call, Throwable t) {
                Toast.makeText(RegisterActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home){
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // 중복확인 응답 객체를 내부 클래스로 정의 (별도의 파일 생성을 피함)
    public static class CheckDuplicateResponse {
        private boolean available;
        private String message;
        public boolean isAvailable() {
            return available;
        }
        public String getMessage() {
            return message;
        }
    }
}
