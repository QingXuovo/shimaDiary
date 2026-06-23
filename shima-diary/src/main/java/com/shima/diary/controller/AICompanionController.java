package com.shima.diary.controller;

import com.shima.diary.common.Result;
import com.shima.diary.entity.ChatSession;
import com.shima.diary.entity.User;
import com.shima.diary.service.AICompanionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

/**
 * AI情感陪伴控制器
 */
@RestController
@RequestMapping("/ai/companion")
public class AICompanionController {
    
    private static final Logger logger = LoggerFactory.getLogger(AICompanionController.class);
    
    @Autowired
    private AICompanionService aiCompanionService;
    
    /**
     * 获取当前登录用户ID
     */
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
        return 1L;
    }
    
    /**
     * 发送消息给AI
     */
    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(
            @RequestBody Map<String, String> params,
            HttpServletRequest request) {
        
        logger.info("AI对话请求: {}", params);
        
        Long userId = getUserId(request);
        String sessionId = params.get("sessionId");
        String message = params.get("message");
        String messageType = params.getOrDefault("messageType", "text");
        
        if (message == null || message.trim().isEmpty()) {
            return Result.error("消息不能为空");
        }
        
        try {
            Map<String, Object> result = aiCompanionService.sendMessage(userId, sessionId, message, messageType);
            
            if (result.containsKey("error")) {
                return Result.error((String) result.get("error"));
            }
            
            return Result.success(result);
        } catch (Exception e) {
            logger.error("AI对话失败", e);
            return Result.error("对话失败，请稍后重试");
        }
    }
    
    /**
     * 获取会话历史
     */
    @GetMapping("/session/{sessionId}")
    public Result<Map<String, Object>> getSessionHistory(
            @PathVariable String sessionId,
            HttpServletRequest request) {
        
        logger.info("获取会话历史: sessionId={}", sessionId);
        
        Long userId = getUserId(request);
        
        try {
            Map<String, Object> history = aiCompanionService.getSessionHistory(userId, sessionId);
            
            if (history == null) {
                return Result.error("会话不存在");
            }
            
            return Result.success(history);
        } catch (Exception e) {
            logger.error("获取会话历史失败", e);
            return Result.error("获取失败");
        }
    }
    
    /**
     * 获取用户的所有会话
     */
    @GetMapping("/sessions")
    public Result<List<ChatSession>> getUserSessions(HttpServletRequest request) {
        logger.info("获取用户会话列表");
        
        Long userId = getUserId(request);
        
        try {
            List<ChatSession> sessions = aiCompanionService.getUserSessions(userId);
            return Result.success(sessions);
        } catch (Exception e) {
            logger.error("获取会话列表失败", e);
            return Result.error("获取失败");
        }
    }
    
    /**
     * 删除会话
     */
    @DeleteMapping("/session/{sessionId}")
    public Result<String> deleteSession(
            @PathVariable String sessionId,
            HttpServletRequest request) {
        
        logger.info("删除会话: sessionId={}", sessionId);
        
        Long userId = getUserId(request);
        
        try {
            aiCompanionService.deleteSession(userId, sessionId);
            return Result.success("删除成功");
        } catch (Exception e) {
            logger.error("删除会话失败", e);
            return Result.error("删除失败");
        }
    }
    
    /**
     * 创建新会话
     */
    @PostMapping("/session")
    public Result<Map<String, Object>> createSession(
            @RequestBody(required = false) Map<String, String> params,
            HttpServletRequest request) {
        
        logger.info("创建新会话");
        
        Long userId = getUserId(request);
        String sessionType = params != null ? params.getOrDefault("sessionType", "companion") : "companion";
        String title = params != null ? params.get("title") : null;
        
        try {
            Map<String, Object> result = aiCompanionService.sendMessage(
                userId, 
                null, 
                "你好，我想和你聊天", 
                "text"
            );
            return Result.success(result);
        } catch (Exception e) {
            logger.error("创建会话失败", e);
            return Result.error("创建失败");
        }
    }
    
    /**
     * 心情分析
     */
    @PostMapping("/mood-analysis")
    public Result<Map<String, Object>> moodAnalysis(
            @RequestBody Map<String, String> params,
            HttpServletRequest request) {
        
        logger.info("心情分析请求: {}", params);
        
        Long userId = getUserId(request);
        String message = params.get("message");
        
        if (message == null || message.trim().isEmpty()) {
            message = "请帮我分析一下最近的心情";
        }
        
        try {
            Map<String, Object> result = aiCompanionService.sendMessage(
                userId, 
                null, 
                message, 
                "mood_analysis"
            );
            return Result.success(result);
        } catch (Exception e) {
            logger.error("心情分析失败", e);
            return Result.error("分析失败");
        }
    }
    
    /**
     * 日记建议
     */
    @PostMapping("/diary-suggestion")
    public Result<Map<String, Object>> diarySuggestion(HttpServletRequest request) {
        logger.info("日记建议请求");
        
        Long userId = getUserId(request);
        
        try {
            Map<String, Object> result = aiCompanionService.sendMessage(
                userId, 
                null, 
                "请给我一些日记写作建议", 
                "diary_suggestion"
            );
            return Result.success(result);
        } catch (Exception e) {
            logger.error("获取日记建议失败", e);
            return Result.error("获取失败");
        }
    }
}