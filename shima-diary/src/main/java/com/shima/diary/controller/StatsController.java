package com.shima.diary.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shima.diary.common.Result;
import com.shima.diary.entity.Diary;
import com.shima.diary.entity.User;
import com.shima.diary.service.DiaryService;
import com.shima.diary.service.YearReviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据统计控制器
 */
@RestController
@RequestMapping("/stats")
public class StatsController {

    private static final Logger logger = LoggerFactory.getLogger(StatsController.class);

    @Autowired
    private YearReviewService yearReviewService;
    
    @Autowired
    private DiaryService diaryService;

    /**
     * 获取年度回顾数据
     */
    @GetMapping("/year-review")
    public Result<YearReviewService.YearReview> getYearReview(@RequestParam(defaultValue = "-1") int year, 
                                                              HttpSession session) {
        logger.info("获取年度回顾数据: year={}", year);
        User user = (User) session.getAttribute("user");
        if (user == null) {
            logger.warn("获取年度回顾失败: 未登录");
            return Result.error("未登录");
        }

        if (year == -1) {
            year = LocalDateTime.now().getYear();
        }

        try {
            YearReviewService.YearReview review = yearReviewService.getYearReview(user.getId(), year);
            logger.info("获取年度回顾成功: userId={}, year={}", user.getId(), year);
            return Result.success(review);
        } catch (Exception e) {
            logger.error("获取年度回顾失败: userId={}, year={}", user.getId(), year, e);
            return Result.error("获取失败");
        }
    }

    /**
     * 获取月度统计数据
     */
    @GetMapping("/monthly")
    public Result<Map<String, Object>> getMonthlyStats(@RequestParam(defaultValue = "-1") int year,
                                                       HttpSession session) {
        logger.info("获取月度统计数据: year={}", year);
        User user = (User) session.getAttribute("user");
        if (user == null) {
            logger.warn("获取月度统计失败: 未登录");
            return Result.error("未登录");
        }

        if (year == -1) {
            year = LocalDateTime.now().getYear();
        }

        try {
            Map<String, Object> stats = yearReviewService.getMonthlyStats(user.getId(), year);
            logger.info("获取月度统计成功: userId={}, year={}", user.getId(), year);
            return Result.success(stats);
        } catch (Exception e) {
            logger.error("获取月度统计失败: userId={}, year={}", user.getId(), year, e);
            return Result.error("获取失败");
        }
    }

    /**
     * 获取仪表盘数据（综合统计）
     */
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getDashboard(HttpSession session) {
        logger.info("获取仪表盘数据");
        User user = (User) session.getAttribute("user");
        if (user == null) {
            logger.warn("获取仪表盘失败: 未登录");
            return Result.error("未登录");
        }

        try {
            int currentYear = LocalDateTime.now().getYear();
            YearReviewService.YearReview review = yearReviewService.getYearReview(user.getId(), currentYear);

            Map<String, Object> dashboard = new HashMap<>();
            dashboard.put("year", currentYear);
            dashboard.put("diaryCount", review.getDiaryCount());
            dashboard.put("checkinDays", review.getCheckinDays());
            dashboard.put("maxStreak", review.getMaxStreak());
            dashboard.put("monthlyCheckins", review.getMonthlyCheckins());

            logger.info("获取仪表盘成功: userId={}, diaryCount={}, checkinDays={}", 
                user.getId(), review.getDiaryCount(), review.getCheckinDays());
            return Result.success(dashboard);
        } catch (Exception e) {
            logger.error("获取仪表盘失败: userId={}", user.getId(), e);
            return Result.error("获取失败");
        }
    }

    /**
     * 获取心情分布统计
     */
    @GetMapping("/mood-distribution")
    public Result<Map<String, Object>> getMoodDistribution(HttpSession session) {
        logger.info("获取心情分布统计");
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error("未登录");
        }

        try {
            List<Diary> diaries = diaryService.list(new LambdaQueryWrapper<Diary>()
                .eq(Diary::getUserId, user.getId())
                .isNotNull(Diary::getMood)
                .ne(Diary::getMood, "")
                .eq(Diary::getDeleted, 0));

            Map<String, Long> moodCounts = diaries.stream()
                .collect(Collectors.groupingBy(Diary::getMood, Collectors.counting()));

            Map<String, Object> result = new HashMap<>();
            result.put("total", diaries.size());
            result.put("distribution", moodCounts);
            
            List<Map<String, Object>> chartData = new ArrayList<>();
            Map<String, String> moodLabels = Map.of(
                "happy", "开心",
                "sad", "难过",
                "angry", "生气",
                "calm", "平静",
                "excited", "兴奋",
                "neutral", "中性"
            );
            
            for (Map.Entry<String, Long> entry : moodCounts.entrySet()) {
                Map<String, Object> item = new HashMap<>();
                item.put("mood", entry.getKey());
                item.put("label", moodLabels.getOrDefault(entry.getKey(), entry.getKey()));
                item.put("count", entry.getValue());
                chartData.add(item);
            }
            result.put("chartData", chartData);

            logger.info("获取心情分布成功: userId={}, total={}", user.getId(), diaries.size());
            return Result.success(result);
        } catch (Exception e) {
            logger.error("获取心情分布失败: userId={}", user.getId(), e);
            return Result.error("获取失败");
        }
    }

    /**
     * 获取日记发布趋势（最近30天）
     */
    @GetMapping("/diary-trend")
    public Result<Map<String, Object>> getDiaryTrend(@RequestParam(defaultValue = "30") int days, 
                                                     HttpSession session) {
        logger.info("获取日记发布趋势: days={}", days);
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error("未登录");
        }

        try {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(days - 1);

            List<Diary> diaries = diaryService.list(new LambdaQueryWrapper<Diary>()
                .eq(Diary::getUserId, user.getId())
                .ge(Diary::getDiaryDate, startDate)
                .le(Diary::getDiaryDate, endDate)
                .eq(Diary::getDeleted, 0)
                .orderByAsc(Diary::getDiaryDate));

            Map<String, Long> dateCounts = diaries.stream()
                .collect(Collectors.groupingBy(d -> d.getDiaryDate().toString(), Collectors.counting()));

            List<Map<String, Object>> trendData = new ArrayList<>();
            List<String> dates = new ArrayList<>();
            List<Integer> counts = new ArrayList<>();

            for (int i = 0; i < days; i++) {
                LocalDate date = startDate.plusDays(i);
                String dateStr = date.toString();
                Long count = dateCounts.getOrDefault(dateStr, 0L);
                
                dates.add(dateStr);
                counts.add(count.intValue());
                
                Map<String, Object> item = new HashMap<>();
                item.put("date", dateStr);
                item.put("count", count);
                trendData.add(item);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("dates", dates);
            result.put("counts", counts);
            result.put("trendData", trendData);
            result.put("total", diaries.size());

            logger.info("获取日记趋势成功: userId={}, total={}", user.getId(), diaries.size());
            return Result.success(result);
        } catch (Exception e) {
            logger.error("获取日记趋势失败: userId={}", user.getId(), e);
            return Result.error("获取失败");
        }
    }

    /**
     * 获取仪表盘完整数据（包含图表数据）
     */
    @GetMapping("/dashboard-full")
    public Result<Map<String, Object>> getDashboardFull(HttpSession session) {
        logger.info("获取仪表盘完整数据");
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error("未登录");
        }

        try {
            int currentYear = LocalDateTime.now().getYear();
            YearReviewService.YearReview review = yearReviewService.getYearReview(user.getId(), currentYear);

            Map<String, Object> dashboard = new HashMap<>();
            dashboard.put("year", currentYear);
            dashboard.put("diaryCount", review.getDiaryCount());
            dashboard.put("checkinDays", review.getCheckinDays());
            dashboard.put("maxStreak", review.getMaxStreak());
            dashboard.put("monthlyCheckins", review.getMonthlyCheckins());

            Result<Map<String, Object>> moodResult = getMoodDistribution(session);
            if (moodResult.getCode() == 200) {
                dashboard.put("moodDistribution", moodResult.getData());
            }

            Result<Map<String, Object>> trendResult = getDiaryTrend(30, session);
            if (trendResult.getCode() == 200) {
                dashboard.put("diaryTrend", trendResult.getData());
            }

            logger.info("获取仪表盘完整数据成功: userId={}", user.getId());
            return Result.success(dashboard);
        } catch (Exception e) {
            logger.error("获取仪表盘完整数据失败: userId={}", user.getId(), e);
            return Result.error("获取失败");
        }
    }
}