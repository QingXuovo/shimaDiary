package com.shima.diary.service;

import com.shima.diary.entity.CheckIn;
import com.shima.diary.entity.Diary;
import com.shima.diary.entity.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 年度回顾服务
 */
@Service
public class YearReviewService {

    @Autowired
    private DiaryService diaryService;

    @Autowired
    private CheckInService checkInService;

    @Autowired
    private TaskService taskService;

    /**
     * 获取年度回顾数据
     */
    public YearReview getYearReview(Long userId, int year) {
        YearReview review = new YearReview();
        review.setYear(year);

        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        // 日记统计
        List<Diary> diaries = diaryService.getByDateRange(userId, startDate, endDate);
        review.setDiaryCount(diaries.size());

        // 打卡统计
        int checkinDays = 0;
        int[] monthlyCheckins = new int[12];
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            List<CheckIn> records = checkInService.getByDate(userId, current);
            if (!records.isEmpty()) {
                checkinDays++;
                monthlyCheckins[current.getMonthValue() - 1]++;
            }
            current = current.plusDays(1);
        }
        review.setCheckinDays(checkinDays);
        review.setMonthlyCheckins(monthlyCheckins);

        // 计算连续打卡天数
        review.setMaxStreak(calculateMaxStreak(userId, year));

        return review;
    }

    /**
     * 计算最大连续打卡天数
     */
    private int calculateMaxStreak(Long userId, int year) {
        int maxStreak = 0;
        int currentStreak = 0;

        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            List<CheckIn> records = checkInService.getByDate(userId, current);
            if (!records.isEmpty()) {
                currentStreak++;
                if (currentStreak > maxStreak) {
                    maxStreak = currentStreak;
                }
            } else {
                currentStreak = 0;
            }
            current = current.plusDays(1);
        }

        return maxStreak;
    }

    /**
     * 获取月度统计
     */
    public Map<String, Object> getMonthlyStats(Long userId, int year) {
        Map<String, Object> stats = new HashMap<>();

        for (int month = 1; month <= 12; month++) {
            Map<String, Integer> monthStats = new HashMap<>();
            
            LocalDate startDate = LocalDate.of(year, month, 1);
            LocalDate endDate = startDate.plusMonths(1).minusDays(1);
            
            List<Diary> diaries = diaryService.getByDateRange(userId, startDate, endDate);
            monthStats.put("diaryCount", diaries.size());

            int checkins = 0;
            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                List<CheckIn> records = checkInService.getByDate(userId, current);
                if (!records.isEmpty()) {
                    checkins++;
                }
                current = current.plusDays(1);
            }
            monthStats.put("checkinDays", checkins);

            stats.put("month_" + month, monthStats);
        }

        return stats;
    }

    /**
     * 年度回顾数据类
     */
    public static class YearReview {
        private int year;
        private int diaryCount;
        private int checkinDays;
        private int maxStreak;
        private int[] monthlyCheckins;

        public int getYear() { return year; }
        public void setYear(int year) { this.year = year; }
        public int getDiaryCount() { return diaryCount; }
        public void setDiaryCount(int diaryCount) { this.diaryCount = diaryCount; }
        public int getCheckinDays() { return checkinDays; }
        public void setCheckinDays(int checkinDays) { this.checkinDays = checkinDays; }
        public int getMaxStreak() { return maxStreak; }
        public void setMaxStreak(int maxStreak) { this.maxStreak = maxStreak; }
        public int[] getMonthlyCheckins() { return monthlyCheckins; }
        public void setMonthlyCheckins(int[] monthlyCheckins) { this.monthlyCheckins = monthlyCheckins; }
    }
}