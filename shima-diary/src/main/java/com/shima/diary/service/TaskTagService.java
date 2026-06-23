package com.shima.diary.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shima.diary.entity.TaskTag;
import com.shima.diary.mapper.TaskTagMapper;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 任务标签服务类
 */
@Service
public class TaskTagService extends ServiceImpl<TaskTagMapper, TaskTag> {

    /**
     * 获取任务的所有标签
     */
    public List<TaskTag> getTagsByTaskId(Long taskId) {
        return baseMapper.selectByTaskId(taskId);
    }

    /**
     * 为任务添加标签
     */
    public boolean addTag(TaskTag tag) {
        return save(tag);
    }

    /**
     * 批量添加标签
     */
    public boolean addTags(Long taskId, List<TaskTag> tags) {
        tags.forEach(tag -> tag.setTaskId(taskId));
        return saveBatch(tags);
    }

    /**
     * 删除任务标签
     */
    public boolean deleteTag(Long id) {
        return removeById(id);
    }

    /**
     * 删除任务的所有标签
     */
    public boolean deleteByTaskId(Long taskId) {
        LambdaQueryWrapper<TaskTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskTag::getTaskId, taskId);
        return remove(wrapper);
    }
}
