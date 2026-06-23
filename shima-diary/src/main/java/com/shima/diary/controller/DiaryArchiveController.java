package com.shima.diary.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shima.diary.common.Result;
import com.shima.diary.entity.Diary;
import com.shima.diary.entity.User;
import com.shima.diary.service.DiaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 日记归档与回收站控制器
 */
@RestController
@RequestMapping("/diary/archive")
public class DiaryArchiveController {

    private static final Logger logger = LoggerFactory.getLogger(DiaryArchiveController.class);

    @Autowired
    private DiaryService diaryService;

    /**
     * 获取当前登录用户ID（支持Session和Header两种方式）
     */
    private Long getUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            User user = (User) session.getAttribute("user");
            if (user != null) {
                logger.debug("从Session获取用户ID: {}", user.getId());
                return user.getId();
            }
        }
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader != null && !userIdHeader.isEmpty()) {
            try {
                Long userId = Long.parseLong(userIdHeader);
                logger.debug("从Header获取用户ID: {}", userId);
                return userId;
            } catch (NumberFormatException e) {
                logger.warn("无效的用户ID: {}", userIdHeader);
            }
        }
        logger.debug("使用默认用户ID: 1");
        return 1L;
    }

    /**
     * 归档日记
     */
    @PostMapping("/{id}/archive")
    public Result<Boolean> archive(@PathVariable Long id, HttpServletRequest request) {
        logger.info("归档日记: id={}", id);
        Long userId = getUserId(request);
        try {
            Diary diary = diaryService.getById(id);
            if (diary == null) {
                return Result.error("日记不存在");
            }
            if (!diary.getUserId().equals(userId)) {
                return Result.error("无权操作");
            }
            diary.setArchived(1);
            diaryService.updateById(diary);
            logger.info("归档成功: id={}", id);
            return Result.success("归档成功", true);
        } catch (Exception e) {
            logger.error("归档失败: id={}", id, e);
            return Result.error("归档失败");
        }
    }

    /**
     * 取消归档
     */
    @PostMapping("/{id}/unarchive")
    public Result<Boolean> unarchive(@PathVariable Long id, HttpServletRequest request) {
        logger.info("取消归档: id={}", id);
        Long userId = getUserId(request);
        try {
            Diary diary = diaryService.getById(id);
            if (diary == null) {
                return Result.error("日记不存在");
            }
            if (!diary.getUserId().equals(userId)) {
                return Result.error("无权操作");
            }
            diary.setArchived(0);
            diaryService.updateById(diary);
            logger.info("取消归档成功: id={}", id);
            return Result.success("取消归档成功", true);
        } catch (Exception e) {
            logger.error("取消归档失败: id={}", id, e);
            return Result.error("取消归档失败");
        }
    }

    /**
     * 获取已归档日记列表
     */
    @GetMapping("/list")
    public Result<List<Diary>> getArchived(HttpServletRequest request) {
        logger.info("获取已归档日记列表");
        Long userId = getUserId(request);
        try {
            List<Diary> diaries = diaryService.list(new LambdaQueryWrapper<Diary>()
                .eq(Diary::getUserId, userId)
                .eq(Diary::getArchived, 1)
                .orderByDesc(Diary::getUpdateTime));
            logger.info("获取已归档日记成功: {}条", diaries.size());
            return Result.success(diaries);
        } catch (Exception e) {
            logger.error("获取已归档日记失败", e);
            return Result.error("获取失败");
        }
    }

    /**
     * 获取回收站日记列表（已删除）
     */
    @GetMapping("/recycle")
    public Result<List<Diary>> getRecycleBin(HttpServletRequest request) {
        logger.info("获取回收站日记列表");
        Long userId = getUserId(request);
        try {
            // 查询已删除的日记（MyBatis-Plus逻辑删除字段deleted=1）
            List<Diary> diaries = diaryService.list(new LambdaQueryWrapper<Diary>()
                .eq(Diary::getUserId, userId)
                .eq(Diary::getDeleted, 1)
                .orderByDesc(Diary::getUpdateTime));
            logger.info("获取回收站日记成功: {}条", diaries.size());
            return Result.success(diaries);
        } catch (Exception e) {
            logger.error("获取回收站日记失败", e);
            return Result.error("获取失败");
        }
    }

    /**
     * 从回收站恢复日记
     */
    @PostMapping("/{id}/restore")
    public Result<Boolean> restore(@PathVariable Long id, HttpServletRequest request) {
        logger.info("恢复日记: id={}", id);
        Long userId = getUserId(request);
        try {
            Diary diary = diaryService.getById(id);
            if (diary == null) {
                return Result.error("日记不存在");
            }
            if (!diary.getUserId().equals(userId)) {
                return Result.error("无权操作");
            }
            diary.setDeleted(0);
            diary.setArchived(0);
            diaryService.updateById(diary);
            logger.info("恢复成功: id={}", id);
            return Result.success("恢复成功", true);
        } catch (Exception e) {
            logger.error("恢复失败: id={}", id, e);
            return Result.error("恢复失败");
        }
    }

    /**
     * 永久删除日记（从回收站彻底删除）
     */
    @DeleteMapping("/{id}/permanent")
    public Result<Boolean> permanentDelete(@PathVariable Long id, HttpServletRequest request) {
        logger.info("永久删除日记: id={}", id);
        Long userId = getUserId(request);
        try {
            Diary diary = diaryService.getById(id);
            if (diary == null) {
                return Result.error("日记不存在");
            }
            if (!diary.getUserId().equals(userId)) {
                return Result.error("无权操作");
            }
            // 使用MyBatis-Plus的removeById会执行物理删除
            diaryService.removeById(id);
            logger.info("永久删除成功: id={}", id);
            return Result.success("永久删除成功", true);
        } catch (Exception e) {
            logger.error("永久删除失败: id={}", id, e);
            return Result.error("永久删除失败");
        }
    }
}