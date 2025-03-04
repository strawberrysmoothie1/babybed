package com.example.myapplication.item;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.AddBedActivity;
import com.example.myapplication.Login_network.AddGuardBedResponse;
import com.example.myapplication.Login_network.CheckBedInfoResponse;
import com.example.myapplication.R;
import com.example.myapplication.model.BedDisplay;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BedAdapter extends RecyclerView.Adapter<BedAdapter.BedViewHolder> {
    private Context context;
    private List<BedDisplay> bedDisplayList;
    private Random random = new Random();
    private String currentUserId;
    private com.example.myapplication.Login_network.LoginService loginService;

    public BedAdapter(Context context, List<BedDisplay> bedDisplayList, String currentUserId,
                      com.example.myapplication.Login_network.LoginService loginService) {
        this.context = context;
        this.bedDisplayList = bedDisplayList;
        this.currentUserId = currentUserId;
        this.loginService = loginService;
    }

    @Override
    public BedViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_bed, parent, false);
        return new BedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(BedViewHolder holder, int position) {
        BedDisplay bed = bedDisplayList.get(position);
        holder.tvDesignation.setText(bed.getDesignation());

        // "침대추가" 항목 처리
        if ("침대추가".equals(bed.getDesignation())) {
            holder.ivBedImage.setImageResource(R.drawable.addbed);
            holder.btnSetting.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(v -> showAddBedDialog());
        } else {
            int[] images = {R.drawable.babybed1, R.drawable.babybed2, R.drawable.babybed3};
            int index = random.nextInt(images.length);
            holder.ivBedImage.setImageResource(images[index]);
            holder.btnSetting.setVisibility(View.VISIBLE);
            holder.itemView.setOnClickListener(null);

            // 설정 버튼 클릭 시 팝업 메뉴 표시
            holder.btnSetting.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(context, holder.btnSetting);
                popup.getMenu().add("침대 삭제");
                popup.getMenu().add("순서 변경");
                // 로그인 사용자가 해당 침대의 보호자라면 "임시보호자 추가" 옵션 추가
                if ("guardian".equals(bed.getUserRole())) {
                    popup.getMenu().add("임시보호자 추가");
                }
                popup.setOnMenuItemClickListener(item -> {
                    if ("침대 삭제".equals(item.getTitle())) {
                        int pos = holder.getAdapterPosition();
                        showDeleteDialog(bed, pos);
                        return true;
                    } else if ("순서 변경".equals(item.getTitle())) {
                        Toast.makeText(context, "순서 변경 호출", Toast.LENGTH_SHORT).show();
                        // TODO: 순서 변경 로직 구현
                        return true;
                    } else if ("임시보호자 추가".equals(item.getTitle())) {
                        showAddTempGuardianDialog(bed);
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }
    }

    private void showAddBedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("침대 추가");
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_bed, null);
        builder.setView(dialogView);
        builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel());
        builder.setPositiveButton("확인", null);
        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String currentText = dialog.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString();
            if ("확인".equals(currentText)) {
                EditText etSerial = dialogView.findViewById(R.id.etBedSerial);
                String serialInput = etSerial.getText().toString().trim();
                if (serialInput.isEmpty()) {
                    Toast.makeText(context, "일련번호를 입력하세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                Map<String, String> requestBody = new HashMap<>();
                requestBody.put("serialNumber", serialInput);
                Call<CheckBedInfoResponse> callApi = loginService.checkBedInfo(requestBody);
                callApi.enqueue(new Callback<CheckBedInfoResponse>() {
                    @Override
                    public void onResponse(Call<CheckBedInfoResponse> call, Response<CheckBedInfoResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            String foundBedID = response.body().getBedID();
                            etSerial.setVisibility(View.GONE);
                            EditText etDesignation = dialogView.findViewById(R.id.etBedDesignation);
                            etDesignation.setVisibility(View.VISIBLE);
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText("추가");
                            dialogView.setTag(foundBedID);
                        } else {
                            Toast.makeText(context, "침대 정보가 없습니다.", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        }
                    }
                    @Override
                    public void onFailure(Call<CheckBedInfoResponse> call, Throwable t) {
                        Toast.makeText(context, "서버 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                });
            } else if ("추가".equals(currentText)) {
                EditText etDesignation = dialogView.findViewById(R.id.etBedDesignation);
                String designationInput = etDesignation.getText().toString().trim();
                if (designationInput.isEmpty()) {
                    Toast.makeText(context, "침대 명칭을 입력하세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                String bedID = (String) dialogView.getTag();
                if (bedID == null || bedID.isEmpty()) {
                    Toast.makeText(context, "유효하지 않은 침대 정보입니다.", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    return;
                }
                Map<String, String> requestBody = new HashMap<>();
                requestBody.put("gdID", currentUserId);
                requestBody.put("bedID", bedID);
                requestBody.put("designation", designationInput);
                requestBody.put("period", "");
                Call<AddGuardBedResponse> callAdd = loginService.addGuardBed(requestBody);
                callAdd.enqueue(new Callback<AddGuardBedResponse>() {
                    @Override
                    public void onResponse(Call<AddGuardBedResponse> call, Response<AddGuardBedResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            Toast.makeText(context, "침대가 추가되었습니다.", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            if (context instanceof AddBedActivity) {
                                ((AddBedActivity) context).recreate();
                            }
                        } else {
                            String errMsg = (response.body() != null) ? response.body().getMessage() : "알 수 없는 오류";
                            Toast.makeText(context, "침대 추가 실패: " + errMsg, Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<AddGuardBedResponse> call, Throwable t) {
                        Toast.makeText(context, "침대 추가 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void showDeleteDialog(BedDisplay bed, int pos) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("침대 삭제");
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_delete_bed, null);
        builder.setView(dialogView);
        builder.setPositiveButton("삭제", (dialog, which) -> {
            EditText etPassword = dialogView.findViewById(R.id.etDeletePassword);
            String password = etPassword.getText().toString().trim();
            if (password.isEmpty()) {
                Toast.makeText(context, "비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            deleteBed(bed.getBedID(), password, pos);
        });
        builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void deleteBed(String bedID, String password, int pos) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("gdID", currentUserId);
        requestBody.put("bedID", bedID);
        requestBody.put("password", password);
        Call<Void> call = loginService.deleteBed(requestBody);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(context, "삭제 성공", Toast.LENGTH_SHORT).show();
                    bedDisplayList.remove(pos);
                    notifyItemRemoved(pos);
                } else {
                    Toast.makeText(context, "삭제 실패: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(context, "삭제 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return bedDisplayList.size();
    }

    public BedDisplay getBedDisplayAt(int position) {
        return bedDisplayList.get(position);
    }

    // 임시보호자 추가 다이얼로그 표시
    private void showAddTempGuardianDialog(BedDisplay bed) {
        String message = "'" + bed.getDesignation() + "' 침대에 대한 임시 보호자를 추가합니다.";
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("임시보호자 추가");

        // 메시지와 입력 필드를 담을 LinearLayout 생성
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * context.getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        // 메시지 TextView 생성 및 스타일 적용
        TextView messageView = new TextView(context);
        messageView.setText(message);
        messageView.setTextSize(15); // 텍스트 크기를 줄임 (sp 단위)
        messageView.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(context, R.font.hakgyoansim_geurimilgi));
        // 메시지와 입력필드 사이에 추가 여백 (예: 16dp)
        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        messageParams.bottomMargin = (int) (16 * context.getResources().getDisplayMetrics().density);
        messageView.setLayoutParams(messageParams);
        layout.addView(messageView);

        // 사용자 입력을 위한 EditText 생성
        final EditText input = new EditText(context);
        input.setHint("임시보호자 아이디 입력");
        input.setTextSize(14); // 텍스트 크기를 줄임
        input.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(context, R.font.hakgyoansim_geurimilgi));
        layout.addView(input);

        builder.setView(layout);

        builder.setNegativeButton("취소", (dialog, which) -> dialog.dismiss());
        builder.setPositiveButton("추가", (dialog, which) -> {
            String tempGuardianId = input.getText().toString().trim();
            if (tempGuardianId.isEmpty()) {
                Toast.makeText(context, "임시보호자 아이디를 입력하세요.", Toast.LENGTH_SHORT).show();
            } else {
                // 임시보호자 추가 API 호출 또는 처리 로직 구현
                Toast.makeText(context, tempGuardianId + " 임시보호자 추가 처리", Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }


    public static class BedViewHolder extends RecyclerView.ViewHolder {
        ImageView ivBedImage;
        TextView tvDesignation;
        ImageButton btnSetting;
        // btnAddTemp 필드는 더 이상 사용하지 않으므로 제거합니다.

        public BedViewHolder(View itemView) {
            super(itemView);
            ivBedImage = itemView.findViewById(R.id.ivBedImage);
            tvDesignation = itemView.findViewById(R.id.tvDesignation);
            btnSetting = itemView.findViewById(R.id.btnSetting);
        }
    }
}
