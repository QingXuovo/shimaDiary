package com.shima.diary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shima.diary.entity.TaskTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 任务标签 Mapper 接口
 */
@Mapper
public interface TaskTagMapper extends BaseMapper<TaskTag> {
    
    /**
     * 根据任务ID查询标签
     */
    List<TaskTag> selectByTaskId(@Param("taskId") Long taskId);
}
