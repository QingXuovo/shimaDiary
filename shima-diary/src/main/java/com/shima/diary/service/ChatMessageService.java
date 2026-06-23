package com.shima.diary.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shima.diary.entity.ChatMessage;
import com.shima.diary.mapper.ChatMessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI对话消息服务
 */
@Service
public class ChatMessageService {
    
    @Autowired
    private ChatMessageMapper chatMessageMapper;
    
    /**
     * 保存消息
     */
    public void saveMessage(ChatMessage message) {
        chatMessageMapper.insert(message);
    }
    
    /**
     * 获取会话的消息列表
     */
    public List<ChatMessage> getSessionMessages(Long userId, String sessionId) {
        return chatMessageMapper.selectList(
            new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getUserId, userId)
                .eq(ChatMessage::getSessionId, sessionId)
                .orderByAsc(ChatMessage::getCreateTime)
        );
    }
    
    /**
     * 获取用户最近的消息
     */
    public List<ChatMessage> getRecentMessages(Long userId, int limit) {
        return chatMessageMapper.selectList(
            new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getUserId, userId)
                .orderByDesc(ChatMessage::getCreateTime)
                .last("LIMIT " + limit)
        );
    }
    
    /**
     * 删除会话的所有消息
     */
    public void deleteSessionMessages(Long userId, String sessionId) {
        chatMessageMapper.delete(
            new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getUserId, userId)
                .eq(ChatMessage::getSessionId, sessionId)
        );
    }
}