package com.shima.diary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shima.diary.entity.Task;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDate;
import java.util.List;

/**
 * 任务 Mapper 接口
 */
@Mapper
public interface TaskMapper extends BaseMapper<Task> {
    
    /**
     * 查询子任务
     */
    List<Task> selectByParentId(@Param("parentId") Long parentId);
    
    /**
     * 查询今日到期任务
     */
    List<Task> selectTodayDue(@Param("userId") Long userId, @Param("today") LocalDate today);
    
    /**
     * 查询过期未完成任务
     */
    List<Task> selectOverdue(@Param("userId") Long userId, @Param("today") LocalDate today);
}
