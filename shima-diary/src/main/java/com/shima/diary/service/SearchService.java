package com.shima.diary.service;

import com.shima.diary.entity.Diary;
import com.shima.diary.entity.Task;
import com.shima.diary.mapper.DiaryMapper;
import com.shima.diary.mapper.TaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 搜索服务 - 基于数据库实现全文搜索
 */
@Slf4j
@Service
public class SearchService {

    @Autowired
    private DiaryMapper diaryMapper;

    @Autowired
    private TaskMapper taskMapper;

    // ============ 日记搜索相关 ============

    /**
     * 全文搜索日记（按日期排序）
     */
    public Page<Diary> searchDiaries(Long userId, String keyword, int pageNum, int pageSize) {
        if (!StringUtils.hasText(keyword)) {
            return new Page<>();
        }

        return searchDiariesWithDB(userId, keyword, pageNum, pageSize);
    }

    /**
     * 使用数据库搜索日记
     */
    private Page<Diary> searchDiariesWithDB(Long userId, String keyword, int pageNum, int pageSize) {
        try {
            LambdaQueryWrapper<Diary> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Diary::getUserId, userId)
                   .and(w -> w.like(Diary::getTitle, keyword)
                              .or()
                              .like(Diary::getContent, keyword)
                              .or()
                              .like(Diary::getTags, keyword))
                   .orderByDesc(Diary::getDiaryDate);

            return diaryMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        } catch (Exception e) {
            log.error("数据库搜索日记失败: userId={}, keyword={}, error={}", userId, keyword, e.getMessage());
            return new Page<>();
        }
    }

    // ============ 任务搜索相关 ============

    /**
     * 全文搜索任务（按优先级和截止日期排序）
     * 优先级: 1-高, 2-中, 3-低
     */
    public Page<Task> searchTasks(Long userId, String keyword, int pageNum, int pageSize) {
        if (!StringUtils.hasText(keyword)) {
            return new Page<>();
        }

        return searchTasksWithDB(userId, keyword, pageNum, pageSize);
    }

    /**
     * 使用数据库搜索任务
     */
    private Page<Task> searchTasksWithDB(Long userId, String keyword, int pageNum, int pageSize) {
        try {
            LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Task::getUserId, userId)
                   .and(w -> w.like(Task::getTitle, keyword)
                              .or()
                              .like(Task::getDescription, keyword)
                              .or()
                              .like(Task::getTags, keyword)
                              .or()
                              .like(Task::getCategory, keyword))
                   .orderByAsc(Task::getPriority)
                   .orderByAsc(Task::getDueDate);

            return taskMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        } catch (Exception e) {
            log.error("数据库搜索任务失败: userId={}, keyword={}, error={}", userId, keyword, e.getMessage());
            return new Page<>();
        }
    }

    // ============ 通用方法 ============

    /**
     * 搜索建议（自动补全）- 同时搜索日记和任务标题
     */
    public List<String> searchSuggestions(Long userId, String prefix, int limit) {
        if (!StringUtils.hasText(prefix)) {
            return new ArrayList<>();
        }

        return searchSuggestionsWithDB(userId, prefix, limit);
    }

    /**
     * 使用数据库搜索建议
     */
    private List<String> searchSuggestionsWithDB(Long userId, String prefix, int limit) {
        try {
            List<String> suggestions = new ArrayList<>();

            // 搜索日记标题
            LambdaQueryWrapper<Diary> diaryWrapper = new LambdaQueryWrapper<>();
            diaryWrapper.eq(Diary::getUserId, userId)
                        .like(Diary::getTitle, prefix)
                        .last("LIMIT " + limit);
            List<Diary> diaries = diaryMapper.selectList(diaryWrapper);
            diaries.forEach(d -> suggestions.add(d.getTitle()));

            // 搜索任务标题
            LambdaQueryWrapper<Task> taskWrapper = new LambdaQueryWrapper<>();
            taskWrapper.eq(Task::getUserId, userId)
                       .like(Task::getTitle, prefix)
                       .last("LIMIT " + limit);
            List<Task> tasks = taskMapper.selectList(taskWrapper);
            tasks.forEach(t -> suggestions.add(t.getTitle()));

            return suggestions.stream()
                    .distinct()
                    .limit(limit)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("数据库搜索建议失败: userId={}, prefix={}, error={}", userId, prefix, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 检查搜索服务是否可用
     */
    public boolean isElasticsearchAvailable() {
        return false;
    }

    /**
     * 创建索引（如果不存在）- 空实现
     */
    public void createIndexIfNotExists() {
        // 空实现，因为不使用Elasticsearch
    }

    /**
     * 删除用户日记索引 - 空实现
     */
    public void deleteUserDiaryIndexes(Long userId) {
        // 空实现
    }

    /**
     * 删除用户任务索引 - 空实现
     */
    public void deleteUserTaskIndexes(Long userId) {
        // 空实现
    }

    /**
     * 索引日记 - 空实现
     */
    public void indexDiary(Diary diary) {
        // 空实现
    }

    /**
     * 索引任务 - 空实现
     */
    public void indexTask(Task task) {
        // 空实现
    }

    /**
     * 删除单篇日记索引 - 空实现
     */
    public void deleteDiaryIndex(Long diaryId) {
        // 空实现
    }

    /**
     * 批量索引日记 - 空实现
     */
    public void indexDiaries(List<Diary> diaries) {
        // 空实现
    }
}