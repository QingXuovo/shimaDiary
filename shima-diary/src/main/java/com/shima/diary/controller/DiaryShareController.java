package com.shima.diary.controller;

import com.shima.diary.common.Result;
import com.shima.diary.entity.Diary;
import com.shima.diary.entity.DiaryShare;
import com.shima.diary.entity.User;
import com.shima.diary.service.DiaryShareService;
import com.shima.diary.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 日记分享控制器
 */
@RestController
@RequestMapping("/share")
public class DiaryShareController {

    private static final Logger logger = LoggerFactory.getLogger(DiaryShareController.class);

    @Autowired
    private DiaryShareService diaryShareService;

    @Autowired
    private UserService userService;

    private Long getUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            User user = (User) session.getAttribute("user");
            if (user != null) {
                return user.getId();
            }
        }
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader != null && !userIdHeader.isEmpty()) {
            try {
                return Long.parseLong(userIdHeader);
            } catch (NumberFormatException e) {
                logger.warn("无效的用户ID: {}", userIdHeader);
            }
        }
        return null;
    }

    /**
     * 创建分享链接
     */
    @PostMapping("/diary/{diaryId}")
    public Result<Map<String, Object>> createShare(@PathVariable Long diaryId, 
                                                   @RequestBody Map<String, Integer> data,
                                                   HttpServletRequest request) {
        logger.info("创建分享链接: diaryId={}, data={}", diaryId, data);
        Long userId = getUserId(request);
        if (userId == null) {
            logger.warn("创建分享链接失败: 未登录");
            return Result.error("未登录");
        }

        int days = data.getOrDefault("days", 7);
        
        try {
            DiaryShare share = diaryShareService.createShare(diaryId, userId, days);
            if (share != null) {
                String shareUrl = "http://localhost:8080/api/vue/#/share/" + share.getShareToken();
                Map<String, Object> result = new HashMap<>();
                result.put("shareUrl", shareUrl);
                result.put("expireTime", share.getExpireTime());
                logger.info("分享链接创建成功: diaryId={}, token={}", diaryId, share.getShareToken());
                return Result.success("分享链接已创建", result);
            }
            logger.warn("创建分享链接失败: diaryId={}", diaryId);
            return Result.error("创建分享链接失败");
        } catch (Exception e) {
            logger.error("创建分享链接异常: diaryId={}", diaryId, e);
            return Result.error("创建分享链接失败");
        }
    }

    /**
     * 取消分享
     */
    @DeleteMapping("/diary/{diaryId}")
    public Result<Boolean> cancelShare(@PathVariable Long diaryId, HttpServletRequest request) {
        logger.info("取消分享: diaryId={}", diaryId);
        Long userId = getUserId(request);
        if (userId == null) {
            logger.warn("取消分享失败: 未登录");
            return Result.error("未登录");
        }

        try {
            boolean success = diaryShareService.cancelShare(diaryId, userId);
            if (success) {
                logger.info("取消分享成功: diaryId={}", diaryId);
            } else {
                logger.warn("取消分享失败: diaryId={}", diaryId);
            }
            return success ? Result.success("分享已取消", true) : Result.error("取消分享失败");
        } catch (Exception e) {
            logger.error("取消分享异常: diaryId={}", diaryId, e);
            return Result.error("取消分享失败");
        }
    }

    /**
     * 通过分享链接查看日记
     */
    @GetMapping("/{token}")
    public Result<Map<String, Object>> viewSharedDiary(@PathVariable String token) {
        logger.info("查看分享日记: token={}", token);
        try {
            DiaryShare share = diaryShareService.getByToken(token);
            if (share != null) {
                Diary diary = diaryShareService.getDiaryByShareToken(token);
                if (diary != null) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("diary", diary);
                    Map<String, Object> shareInfo = new HashMap<>();
                    shareInfo.put("expireTime", share.getExpireTime());
                    shareInfo.put("viewCount", share.getViewCount());
                    result.put("shareInfo", shareInfo);
                    logger.info("查看分享日记成功: diaryId={}", diary.getId());
                    return Result.success(result);
                }
            }
            logger.warn("分享链接无效或已过期: token={}", token);
            return Result.error("分享链接无效或已过期");
        } catch (Exception e) {
            logger.error("查看分享日记异常: token={}", token, e);
            return Result.error("分享链接无效或已过期");
        }
    }
}