package com.example.myapplication.item;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MiniCalendarView extends LinearLayout {

    private CalendarView calendarView;
    private TextView tvSelectedDate;
    private String selectedDate; // 클래스 필드

    public MiniCalendarView(Context context) {
        super(context);
        init(context);
    }

    public MiniCalendarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MiniCalendarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        calendarView = new CalendarView(context);
        // 오늘 날짜를 기준으로 이전 날짜 선택 불가 (현재 시각의 밀리초 값을 최소 날짜로 지정)
        calendarView.setMinDate(System.currentTimeMillis());
        addView(calendarView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        tvSelectedDate = new TextView(context);
        addView(tvSelectedDate, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        // 초기 선택된 날짜: CalendarView의 기본 날짜 (필드에 저장)
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        selectedDate = sdf.format(new Date(calendarView.getDate()));
        tvSelectedDate.setText("선택된 날짜: " + selectedDate);

        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(CalendarView view, int year, int month, int dayOfMonth) {
                // month는 0부터 시작하므로 +1
                selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                tvSelectedDate.setText("선택된 날짜: " + selectedDate);
            }
        });
    }

    public String getSelectedDate() {
        return selectedDate;
    }
}
