package com.shima.diary.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shima.diary.entity.CheckinGoal;
import com.shima.diary.mapper.CheckinGoalMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 打卡目标服务
 */
@Service
public class CheckinGoalService extends ServiceImpl<CheckinGoalMapper, CheckinGoal> {

    /**
     * 获取用户的所有目标
     */
    public List<CheckinGoal> getByUserId(Long userId) {
        return baseMapper.selectByUserId(userId);
    }

    /**
     * 获取用户的活跃目标
     */
    public List<CheckinGoal> getActiveGoals(Long userId) {
        return baseMapper.selectActiveGoals(userId);
    }

    /**
     * 创建目标
     */
    public CheckinGoal create(CheckinGoal goal) {
        goal.setCurrentCount(0);
        goal.setIsActive(1);
        goal.setStartDate(LocalDateTime.now());
        save(goal);
        return goal;
    }

    /**
     * 更新目标进度
     */
    public boolean updateProgress(Long goalId) {
        CheckinGoal goal = getById(goalId);
        if (goal != null && goal.getIsActive() == 1) {
            goal.setCurrentCount(goal.getCurrentCount() + 1);
            // 检查是否完成目标
            if (goal.getCurrentCount() >= goal.getTargetCount()) {
                goal.setIsActive(0);
            }
            return updateById(goal);
        }
        return false;
    }

    /**
     * 重置目标进度
     */
    public boolean resetProgress(Long goalId) {
        CheckinGoal goal = getById(goalId);
        if (goal != null) {
            goal.setCurrentCount(0);
            goal.setIsActive(1);
            return updateById(goal);
        }
        return false;
    }

    /**
     * 更新目标
     */
    public boolean updateGoal(CheckinGoal goal) {
        return updateById(goal);
    }

    /**
     * 删除目标
     */
    public boolean deleteGoal(Long id) {
        return removeById(id);
    }

    /**
     * 获取目标完成进度百分比
     */
    public double getProgressPercent(Long goalId) {
        CheckinGoal goal = getById(goalId);
        if (goal != null && goal.getTargetCount() > 0) {
            return (double) goal.getCurrentCount() / goal.getTargetCount() * 100;
        }
        return 0;
    }
}