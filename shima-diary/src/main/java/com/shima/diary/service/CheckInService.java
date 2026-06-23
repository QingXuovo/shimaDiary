package com.shima.diary.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shima.diary.entity.CheckIn;
import com.shima.diary.mapper.CheckInMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 打卡服务
 */
@Service
public class CheckInService extends ServiceImpl<CheckInMapper, CheckIn> {

    /**
     * 打卡
     */
    public boolean checkIn(CheckIn checkIn) {
        // 检查当天是否已打卡
        LambdaQueryWrapper<CheckIn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CheckIn::getUserId, checkIn.getUserId())
               .eq(CheckIn::getCheckType, checkIn.getCheckType())
               .eq(CheckIn::getCheckDate, checkIn.getCheckDate());
        
        if (count(wrapper) > 0) {
            return false;
        }
        
        return save(checkIn);
    }

    /**
     * 根据用户ID和日期查询打卡记录
     */
    public List<CheckIn> getByDate(Long userId, LocalDate date) {
        LambdaQueryWrapper<CheckIn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CheckIn::getUserId, userId)
               .eq(CheckIn::getCheckDate, date);
        return list(wrapper);
    }

    /**
     * 根据用户ID和类型查询打卡记录
     */
    public List<CheckIn> getByType(Long userId, String type) {
        LambdaQueryWrapper<CheckIn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CheckIn::getUserId, userId)
               .eq(CheckIn::getCheckType, type)
               .orderByDesc(CheckIn::getCheckDate);
        return list(wrapper);
    }

    /**
     * 获取连续打卡天数
     */
    public int getContinuousDays(Long userId, String type) {
        int days = 0;
        LocalDate today = LocalDate.now();
        
        for (int i = 0; i < 365; i++) {
            LocalDate date = today.minusDays(i);
            LambdaQueryWrapper<CheckIn> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CheckIn::getUserId, userId)
                   .eq(CheckIn::getCheckType, type)
                   .eq(CheckIn::getCheckDate, date);
            
            if (count(wrapper) > 0) {
                days++;
            } else {
                break;
            }
        }
        
        return days;
    }

    /**
     * 获取打卡统计
     */
    public Map<String, Object> getCheckInStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        
        // 获取总打卡天数
        LambdaQueryWrapper<CheckIn> totalWrapper = new LambdaQueryWrapper<>();
        totalWrapper.eq(CheckIn::getUserId, userId);
        int totalCheckins = (int) count(totalWrapper);
        stats.put("totalCheckins", totalCheckins);
        
        // 获取连续打卡天数
        int currentStreak = getContinuousDays(userId, "daily");
        stats.put("currentStreak", currentStreak);
        
        // 获取本月打卡天数
        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);
        LambdaQueryWrapper<CheckIn> monthWrapper = new LambdaQueryWrapper<>();
        monthWrapper.eq(CheckIn::getUserId, userId)
                   .ge(CheckIn::getCheckDate, monthStart)
                   .le(CheckIn::getCheckDate, now);
        int monthCheckins = (int) count(monthWrapper);
        stats.put("monthCheckins", monthCheckins);
        
        return stats;
    }

    /**
     * 获取最近N天的打卡记录
     */
    public List<CheckIn> getRecentDays(Long userId, int days) {
        LocalDate startDate = LocalDate.now().minusDays(days - 1);
        LambdaQueryWrapper<CheckIn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CheckIn::getUserId, userId)
               .ge(CheckIn::getCheckDate, startDate)
               .orderByDesc(CheckIn::getCheckDate);
        return list(wrapper);
    }

    /**
     * 取消打卡
     */
    public boolean cancelCheckIn(Long id) {
        return removeById(id);
    }
}