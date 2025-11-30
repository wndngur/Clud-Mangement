package com.example.clubmanagement.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.clubmanagement.R;
import com.example.clubmanagement.models.Schedule;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CalendarAdapter extends BaseAdapter {

    private Context context;
    private List<Integer> days;
    private int currentMonth;
    private int currentYear;
    private int selectedDay = -1;
    private Set<Integer> scheduledDays; // 일정이 있는 날짜들
    private OnDateClickListener listener;

    public interface OnDateClickListener {
        void onDateClick(int year, int month, int day);
        void onDateLongClick(int year, int month, int day);
    }

    public CalendarAdapter(Context context, int year, int month) {
        this.context = context;
        this.currentYear = year;
        this.currentMonth = month;
        this.days = new ArrayList<>();
        this.scheduledDays = new HashSet<>();
        generateCalendarDays();
    }

    public void setOnDateClickListener(OnDateClickListener listener) {
        this.listener = listener;
    }

    public void setScheduledDays(List<Schedule> schedules) {
        scheduledDays.clear();
        for (Schedule schedule : schedules) {
            if (schedule.getYear() == currentYear && schedule.getMonth() == currentMonth) {
                scheduledDays.add(schedule.getDay());
            }
        }
        notifyDataSetChanged();
    }

    public void setMonth(int year, int month) {
        this.currentYear = year;
        this.currentMonth = month;
        this.selectedDay = -1;
        generateCalendarDays();
        notifyDataSetChanged();
    }

    public void setSelectedDay(int day) {
        this.selectedDay = day;
        notifyDataSetChanged();
    }

    private void generateCalendarDays() {
        days.clear();

        Calendar calendar = Calendar.getInstance();
        calendar.set(currentYear, currentMonth, 1);

        // 이번 달 1일이 무슨 요일인지 (일요일=1, 토요일=7)
        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        // 이전 달의 빈 칸 (0으로 표시)
        for (int i = 1; i < firstDayOfWeek; i++) {
            days.add(0);
        }

        // 이번 달의 날짜
        for (int i = 1; i <= daysInMonth; i++) {
            days.add(i);
        }

        // 다음 달의 빈 칸 (6주를 채우기 위해)
        while (days.size() < 42) {
            days.add(0);
        }
    }

    @Override
    public int getCount() {
        return days.size();
    }

    @Override
    public Integer getItem(int position) {
        return days.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_calendar_day, parent, false);
            holder = new ViewHolder();
            holder.llDayContainer = convertView.findViewById(R.id.llDayContainer);
            holder.tvDay = convertView.findViewById(R.id.tvDay);
            holder.viewScheduleIndicator = convertView.findViewById(R.id.viewScheduleIndicator);
            holder.viewTodayBg = convertView.findViewById(R.id.viewTodayBg);
            holder.viewSelectedBg = convertView.findViewById(R.id.viewSelectedBg);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        int day = days.get(position);

        if (day == 0) {
            // 빈 칸
            holder.tvDay.setText("");
            holder.viewScheduleIndicator.setVisibility(View.GONE);
            holder.viewTodayBg.setVisibility(View.GONE);
            holder.viewSelectedBg.setVisibility(View.GONE);
            holder.llDayContainer.setClickable(false);
        } else {
            holder.tvDay.setText(String.valueOf(day));
            holder.llDayContainer.setClickable(true);

            // 오늘 날짜 확인
            Calendar today = Calendar.getInstance();
            boolean isToday = (today.get(Calendar.YEAR) == currentYear &&
                    today.get(Calendar.MONTH) == currentMonth &&
                    today.get(Calendar.DAY_OF_MONTH) == day);

            // 선택된 날짜 확인
            boolean isSelected = (day == selectedDay);

            // 일정 있는 날짜 확인
            boolean hasSchedule = scheduledDays.contains(day);

            // 요일 색상 설정 (일요일=빨강, 토요일=파랑)
            int dayOfWeek = (position % 7);
            if (isToday) {
                holder.tvDay.setTextColor(Color.WHITE);
                holder.viewTodayBg.setVisibility(View.VISIBLE);
            } else {
                holder.viewTodayBg.setVisibility(View.GONE);
                if (dayOfWeek == 0) { // 일요일
                    holder.tvDay.setTextColor(context.getResources().getColor(android.R.color.holo_red_light, null));
                } else if (dayOfWeek == 6) { // 토요일
                    holder.tvDay.setTextColor(context.getResources().getColor(android.R.color.holo_blue_light, null));
                } else {
                    holder.tvDay.setTextColor(Color.BLACK);
                }
            }

            // 선택된 날짜 표시
            if (isSelected && !isToday) {
                holder.viewSelectedBg.setVisibility(View.VISIBLE);
            } else {
                holder.viewSelectedBg.setVisibility(View.GONE);
            }

            // 일정 인디케이터 표시
            if (hasSchedule) {
                holder.viewScheduleIndicator.setVisibility(View.VISIBLE);
            } else {
                holder.viewScheduleIndicator.setVisibility(View.GONE);
            }

            // 클릭 리스너
            final int clickedDay = day;
            holder.llDayContainer.setOnClickListener(v -> {
                selectedDay = clickedDay;
                notifyDataSetChanged();
                if (listener != null) {
                    listener.onDateClick(currentYear, currentMonth, clickedDay);
                }
            });

            holder.llDayContainer.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onDateLongClick(currentYear, currentMonth, clickedDay);
                }
                return true;
            });
        }

        return convertView;
    }

    private static class ViewHolder {
        LinearLayout llDayContainer;
        TextView tvDay;
        View viewScheduleIndicator;
        View viewTodayBg;
        View viewSelectedBg;
    }

    public int getCurrentYear() {
        return currentYear;
    }

    public int getCurrentMonth() {
        return currentMonth;
    }
}
