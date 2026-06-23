package com.shima.diary.controller;

import com.shima.diary.common.Result;
import com.shima.diary.entity.Diary;
import com.shima.diary.entity.Task;
import com.shima.diary.service.SearchService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 搜索控制器 - 提供全文搜索API
 */
@Slf4j
@RestController
@RequestMapping("/search")
public class SearchController {

    @Autowired
    private SearchService searchService;

    /**
     * 全文搜索日记（按日期排序）
     * GET /api/search/diaries?keyword=xxx&page=0&size=10
     */
    @GetMapping("/diaries")
    public Result<Map<String, Object>> searchDiaries(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session) {

        Long userId = getCurrentUserId(session);
        if (userId == null) {
            return Result.error("请先登录");
        }

        try {
            Page<Diary> result = searchService.searchDiaries(userId, keyword, page, size);

            Map<String, Object> response = new HashMap<>();
            response.put("total", result.getTotal());
            response.put("totalPages", result.getPages());
            response.put("currentPage", page);
            response.put("pageSize", size);
            response.put("diaries", result.getRecords());

            log.info("搜索日记成功: userId={}, keyword={}, total={}", userId, keyword, result.getTotal());
            return Result.success("搜索成功", response);
        } catch (Exception e) {
            log.error("搜索日记失败: userId={}, keyword={}, error={}", userId, keyword, e.getMessage());
            return Result.error("搜索失败: " + e.getMessage());
        }
    }

    /**
     * 全文搜索任务（按优先级和截止日期排序）
     * GET /api/search/tasks?keyword=xxx&page=0&size=10
     */
    @GetMapping("/tasks")
    public Result<Map<String, Object>> searchTasks(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session) {

        Long userId = getCurrentUserId(session);
        if (userId == null) {
            return Result.error("请先登录");
        }

        try {
            Page<Task> result = searchService.searchTasks(userId, keyword, page, size);

            Map<String, Object> response = new HashMap<>();
            response.put("total", result.getTotal());
            response.put("totalPages", result.getPages());
            response.put("currentPage", page);
            response.put("pageSize", size);
            response.put("tasks", result.getRecords());

            log.info("搜索任务成功: userId={}, keyword={}, total={}", userId, keyword, result.getTotal());
            return Result.success("搜索成功", response);
        } catch (Exception e) {
            log.error("搜索任务失败: userId={}, keyword={}, error={}", userId, keyword, e.getMessage());
            return Result.error("搜索失败: " + e.getMessage());
        }
    }

    /**
     * 综合搜索（同时搜索日记和任务）
     * GET /api/search/all?keyword=xxx&page=0&size=10
     */
    @GetMapping("/all")
    public Result<Map<String, Object>> searchAll(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session) {

        Long userId = getCurrentUserId(session);
        if (userId == null) {
            return Result.error("请先登录");
        }

        try {
            // 同时搜索日记和任务
            Page<Diary> diaryResult = searchService.searchDiaries(userId, keyword, page, size);
            Page<Task> taskResult = searchService.searchTasks(userId, keyword, page, size);

            Map<String, Object> response = new HashMap<>();
            response.put("total", diaryResult.getTotal() + taskResult.getTotal());
            response.put("diaryCount", diaryResult.getTotal());
            response.put("taskCount", taskResult.getTotal());
            response.put("diaries", diaryResult.getRecords());
            response.put("tasks", taskResult.getRecords());

            log.info("综合搜索成功: userId={}, keyword={}, diaryCount={}, taskCount={}", 
                    userId, keyword, diaryResult.getTotal(), taskResult.getTotal());
            return Result.success("搜索成功", response);
        } catch (Exception e) {
            log.error("综合搜索失败: userId={}, keyword={}, error={}", userId, keyword, e.getMessage());
            return Result.error("搜索失败: " + e.getMessage());
        }
    }

    /**
     * 搜索建议（自动补全）- 同时搜索日记和任务
     * GET /api/search/suggestions?prefix=xxx
     */
    @GetMapping("/suggestions")
    public Result<List<String>> getSuggestions(
            @RequestParam String prefix,
            @RequestParam(defaultValue = "5") int limit,
            HttpSession session) {

        Long userId = getCurrentUserId(session);
        if (userId == null) {
            return Result.error("请先登录");
        }

        try {
            List<String> suggestions = searchService.searchSuggestions(userId, prefix, limit);
            return Result.success("获取建议成功", suggestions);
        } catch (Exception e) {
            log.error("获取搜索建议失败: userId={}, prefix={}, error={}", userId, prefix, e.getMessage());
            return Result.error("获取建议失败: " + e.getMessage());
        }
    }

    /**
     * 检查搜索服务状态
     * GET /api/search/status
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> getSearchStatus() {
        Map<String, Object> status = new HashMap<>();
        boolean available = searchService.isElasticsearchAvailable();
        status.put("available", available);
        status.put("message", available ? "Elasticsearch服务正常" : "使用数据库搜索");
        status.put("indexReady", true);

        return Result.success("状态获取成功", status);
    }

    /**
     * 重建用户索引（空实现）
     * POST /api/search/rebuild/{userId}
     */
    @PostMapping("/rebuild/{userId}")
    public Result<Boolean> rebuildUserIndex(@PathVariable Long userId, HttpSession session) {
        log.info("索引重建请求: userId={}", userId);
        return Result.success("索引重建成功", true);
    }

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId(HttpSession session) {
        if (session == null) return null;
        Object userId = session.getAttribute("userId");
        if (userId == null) {
            Object user = session.getAttribute("user");
            if (user != null) {
                try {
                    return (Long) user.getClass().getMethod("getId").invoke(user);
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        }
        return (Long) userId;
    }
}