package com.shima.diary.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shima.diary.entity.Task;
import com.shima.diary.mapper.TaskMapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务服务类
 */
@Slf4j
@Service
public class TaskService extends ServiceImpl<TaskMapper, Task> {

    @Autowired
    private SearchService searchService;

    /**
     * 分页查询任务
     */
    public Page<Task> pageList(Long userId, Integer status, int pageNum, int pageSize) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getUserId, userId)
               .eq(status != null, Task::getStatus, status)
               .isNull(Task::getParentId) // 只查询主任务
               .orderByAsc(Task::getPriority)
               .orderByDesc(Task::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    /**
     * 创建任务
     */
    public boolean createTask(Task task) {
        if (task.getUserId() == null) {
            task.setUserId(1L); // 默认用户
        }
        if (task.getStatus() == null) {
            task.setStatus(0);
        }
        if (task.getPriority() == null) {
            task.setPriority(2);
        }
        if (task.getProgress() == null) {
            task.setProgress(0);
        }
        boolean result = save(task);
        if (result) {
            // 异步同步到Elasticsearch
            asyncIndexTask(task);
        }
        return result;
    }

    /**
     * 更新任务
     */
    public boolean updateTask(Task task) {
        boolean result = updateById(task);
        if (result) {
            // 异步同步到Elasticsearch
            asyncIndexTask(task);
        }
        return result;
    }

    /**
     * 异步索引任务到Elasticsearch
     */
    @Async
    public void asyncIndexTask(Task task) {
        try {
            if (task.getId() == null) {
                log.warn("无法索引任务: ID为空");
                return;
            }
            searchService.indexTask(task);
            log.info("任务已同步到Elasticsearch: id={}, title={}", task.getId(), task.getTitle());
        } catch (Exception e) {
            log.error("任务同步到Elasticsearch失败: id={}, error={}", task.getId(), e.getMessage());
        }
    }

    /**
     * 同步索引单个任务（不异步）
     */
    public void syncIndexTask(Task task) {
        try {
            if (task.getId() == null) {
                log.warn("无法索引任务: ID为空");
                return;
            }
            searchService.indexTask(task);
            log.info("任务已同步到Elasticsearch: id={}, title={}", task.getId(), task.getTitle());
        } catch (Exception e) {
            log.error("任务同步到Elasticsearch失败: id={}, error={}", task.getId(), e.getMessage());
        }
    }

    /**
     * 更新任务状态
     */
    public boolean updateStatus(Long id, Integer status) {
        LambdaUpdateWrapper<Task> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Task::getId, id)
               .set(Task::getStatus, status);
        if (status == 2) { // 已完成
            wrapper.set(Task::getCompleteTime, LocalDateTime.now())
                   .set(Task::getProgress, 100);
        }
        return update(wrapper);
    }

    /**
     * 更新任务进度
     */
    public boolean updateProgress(Long id, Integer progress) {
        LambdaUpdateWrapper<Task> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Task::getId, id)
               .set(Task::getProgress, progress);
        if (progress == 100) {
            wrapper.set(Task::getStatus, 2)
                   .set(Task::getCompleteTime, LocalDateTime.now());
        }
        return update(wrapper);
    }

    /**
     * 删除任务
     */
    public boolean deleteTask(Long id) {
        return removeById(id);
    }

    /**
     * 查询子任务
     */
    public List<Task> getSubTasks(Long parentId) {
        return baseMapper.selectByParentId(parentId);
    }

    /**
     * 查询今日到期任务
     */
    public List<Task> getTodayDue(Long userId) {
        return baseMapper.selectTodayDue(userId, LocalDate.now());
    }

    /**
     * 查询过期未完成任务
     */
    public List<Task> getOverdue(Long userId) {
        return baseMapper.selectOverdue(userId, LocalDate.now());
    }

    /**
     * 按分类查询任务
     */
    public List<Task> getByCategory(Long userId, String category) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getUserId, userId)
               .eq(Task::getCategory, category)
               .isNull(Task::getParentId)
               .orderByAsc(Task::getPriority);
        return list(wrapper);
    }

    /**
     * 按优先级查询任务
     */
    public List<Task> getByPriority(Long userId, Integer priority) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getUserId, userId)
               .eq(Task::getPriority, priority)
               .isNull(Task::getParentId)
               .orderByDesc(Task::getCreateTime);
        return list(wrapper);
    }

    /**
     * 按标签搜索任务
     */
    public List<Task> searchByTag(Long userId, String tag) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getUserId, userId)
               .like(Task::getTags, tag)
               .isNull(Task::getParentId)
               .orderByDesc(Task::getCreateTime);
        return list(wrapper);
    }

    /**
     * 搜索任务
     */
    public List<Task> search(Long userId, String keyword) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getUserId, userId)
               .and(w -> w.like(Task::getTitle, keyword)
                          .or()
                          .like(Task::getDescription, keyword)
                          .or()
                          .like(Task::getTags, keyword))
               .isNull(Task::getParentId)
               .orderByDesc(Task::getCreateTime);
        return list(wrapper);
    }

    /**
     * 统计各状态任务数量
     */
    public int countByStatus(Long userId, Integer status) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getUserId, userId)
               .eq(Task::getStatus, status)
               .isNull(Task::getParentId);
        return (int) count(wrapper);
    }

    /**
     * 统计任务完成率
     */
    public double getCompletionRate(Long userId) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getUserId, userId)
               .isNull(Task::getParentId);
        long total = count(wrapper);
        
        if (total == 0) return 0;
        
        wrapper.eq(Task::getStatus, 2);
        long completed = count(wrapper);
        
        return (double) completed / total * 100;
    }

    /**
     * 获取任务统计数据
     */
    public TaskStats getStats(Long userId) {
        TaskStats stats = new TaskStats();
        stats.setTotal((int) count(new LambdaQueryWrapper<Task>().eq(Task::getUserId, userId).isNull(Task::getParentId)));
        stats.setPending(countByStatus(userId, 0));
        stats.setInProgress(countByStatus(userId, 1));
        stats.setCompleted(countByStatus(userId, 2));
        stats.setOverdue((int) baseMapper.selectOverdue(userId, LocalDate.now()).size());
        stats.setCompletionRate(getCompletionRate(userId));
        return stats;
    }

    /**
     * 任务统计内部类
     */
    public static class TaskStats {
        private int total;
        private int pending;
        private int inProgress;
        private int completed;
        private int overdue;
        private double completionRate;

        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        public int getPending() { return pending; }
        public void setPending(int pending) { this.pending = pending; }
        public int getInProgress() { return inProgress; }
        public void setInProgress(int inProgress) { this.inProgress = inProgress; }
        public int getCompleted() { return completed; }
        public void setCompleted(int completed) { this.completed = completed; }
        public int getOverdue() { return overdue; }
        public void setOverdue(int overdue) { this.overdue = overdue; }
        public double getCompletionRate() { return completionRate; }
        public void setCompletionRate(double completionRate) { this.completionRate = completionRate; }
    }
}
