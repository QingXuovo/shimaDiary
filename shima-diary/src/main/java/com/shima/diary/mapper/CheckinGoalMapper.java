package com.shima.diary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shima.diary.entity.CheckinGoal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 打卡目标Mapper
 */
@Mapper
public interface CheckinGoalMapper extends BaseMapper<CheckinGoal> {

    /**
     * 根据用户ID查询目标列表
     */
    List<CheckinGoal> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查询活跃目标
     */
    List<CheckinGoal> selectActiveGoals(@Param("userId") Long userId);
}