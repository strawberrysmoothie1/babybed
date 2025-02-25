package com.example.myapplication.item;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import java.util.List;

public class BedAdapter extends RecyclerView.Adapter<BedAdapter.BedViewHolder> {
    private Context context;
    // 각 아이템은 [GdID, bedID, designation, period, bed_order] 형태의 문자열 리스트
    private List<List<String>> bedInfoList;

    public BedAdapter(Context context, List<List<String>> bedInfoList) {
        this.context = context;
        this.bedInfoList = bedInfoList;
    }

    @Override
    public BedViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_bed, parent, false);
        return new BedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(BedViewHolder holder, int position) {
        List<String> row = bedInfoList.get(position);
        // row.get(0)=GdID, row.get(1)=bedID, row.get(2)=designation, row.get(3)=period, row.get(4)=bed_order
        String designation = row.get(2);
        String period = row.get(3);
        // 임시 보호기간이 존재하면 아래 TextView에 표시, 없으면 빈 문자열
        holder.tvDesignation.setText(designation);
        if (period != null && !period.equals("null") && !period.isEmpty()) {
            holder.tvPeriod.setText("임시 보호기간: " + period);
        } else {
            holder.tvPeriod.setText("");
        }

        // 버튼 클릭 리스너 추가 (필요 시)
    }

    @Override
    public int getItemCount() {
        return bedInfoList.size();
    }

    public static class BedViewHolder extends RecyclerView.ViewHolder {
        TextView tvDesignation;
        TextView tvPeriod;
        public BedViewHolder(View itemView) {
            super(itemView);
            tvDesignation = itemView.findViewById(R.id.tvDesignation);
            tvPeriod = itemView.findViewById(R.id.tvPeriod);
        }
    }
}
