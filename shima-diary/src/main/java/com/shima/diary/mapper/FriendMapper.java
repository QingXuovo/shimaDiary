package com.shima.diary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shima.diary.entity.Friend;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 好友Mapper
 */
@Mapper
public interface FriendMapper extends BaseMapper<Friend> {

    /**
     * 查询用户的好友列表
     */
    List<Friend> selectFriends(@Param("userId") Long userId);

    /**
     * 查询用户的待验证好友请求
     */
    List<Friend> selectPendingRequests(@Param("userId") Long userId);

    /**
     * 查询好友关系
     */
    Friend selectFriendship(@Param("userId") Long userId, @Param("friendId") Long friendId);
}