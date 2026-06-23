package com.shima.diary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shima.diary.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI对话会话Mapper
 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
}