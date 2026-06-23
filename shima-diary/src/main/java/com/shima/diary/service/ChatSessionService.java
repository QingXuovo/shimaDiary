package com.shima.diary.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shima.diary.entity.ChatSession;
import com.shima.diary.mapper.ChatSessionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * AI对话会话服务
 */
@Service
public class ChatSessionService {
    
    @Autowired
    private ChatSessionMapper chatSessionMapper;
    
    /**
     * 创建新会话
     */
    public ChatSession createSession(Long userId, String sessionType, String title) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setSessionId(UUID.randomUUID().toString());
        session.setSessionType(sessionType);
        session.setTitle(title);
        session.setMessageCount(0);
        session.setLastMessageTime(LocalDateTime.now());
        chatSessionMapper.insert(session);
        return session;
    }
    
    /**
     * 获取用户的会话列表
     */
    public List<ChatSession> getUserSessions(Long userId) {
        return chatSessionMapper.selectList(
            new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUserId, userId)
                .orderByDesc(ChatSession::getLastMessageTime)
        );
    }
    
    /**
     * 获取会话详情
     */
    public ChatSession getSession(Long userId, String sessionId) {
        return chatSessionMapper.selectOne(
            new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUserId, userId)
                .eq(ChatSession::getSessionId, sessionId)
        );
    }
    
    /**
     * 更新会话
     */
    public void updateSession(ChatSession session) {
        chatSessionMapper.updateById(session);
    }
    
    /**
     * 删除会话
     */
    public void deleteSession(Long userId, String sessionId) {
        chatSessionMapper.delete(
            new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUserId, userId)
                .eq(ChatSession::getSessionId, sessionId)
        );
    }
}