package com.shima.diary.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shima.diary.entity.Friend;
import com.shima.diary.entity.User;
import com.shima.diary.mapper.FriendMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 好友服务
 */
@Service
public class FriendService extends ServiceImpl<FriendMapper, Friend> {

    private static final Logger logger = LoggerFactory.getLogger(FriendService.class);

    @Autowired
    private UserService userService;

    /**
     * 发送好友请求
     * @return 新创建的好友请求，如果已存在关系则返回已存在的记录
     */
    public Friend sendRequest(Long userId, Long friendId) {
        logger.info("[好友服务] 开始发送好友请求，用户ID: {}, 目标用户ID: {}", userId, friendId);
        
        // 检查是否已经是好友或有待处理的请求
        logger.info("[好友服务] 检查是否已存在好友关系...");
        Friend existing = baseMapper.selectFriendship(userId, friendId);
        if (existing != null) {
            logger.info("[好友服务] 已存在好友关系，状态: {}, 关系ID: {}", existing.getStatus(), existing.getId());
            return existing;
        }
        logger.info("[好友服务] 不存在现有好友关系");

        // 检查好友是否存在
        logger.info("[好友服务] 检查目标用户是否存在，用户ID: {}", friendId);
        User friend = userService.getById(friendId);
        if (friend == null) {
            logger.error("[好友服务] 目标用户不存在，用户ID: {}", friendId);
            return null;
        }
        logger.info("[好友服务] 目标用户存在，用户名: {}", friend.getUsername());

        // 创建好友请求
        logger.info("[好友服务] 创建好友请求记录...");
        Friend friendRequest = new Friend();
        friendRequest.setUserId(userId);
        friendRequest.setFriendId(friendId);
        friendRequest.setStatus(0); // 待验证
        
        boolean saved = save(friendRequest);
        if (saved) {
            logger.info("[好友服务] 好友请求创建成功，关系ID: {}", friendRequest.getId());
            // 设置一个标记表示这是新创建的请求（使用ID>0来判断）
        } else {
            logger.error("[好友服务] 好友请求创建失败");
        }
        
        return friendRequest;
    }

    /**
     * 接受好友请求
     */
    public boolean acceptRequest(Long userId, Long friendId) {
        LambdaQueryWrapper<Friend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Friend::getFriendId, userId)
               .eq(Friend::getUserId, friendId)
               .eq(Friend::getStatus, 0);

        Friend request = getOne(wrapper);
        if (request != null) {
            request.setStatus(1); // 已通过
            updateById(request);
            return true;
        }
        return false;
    }

    /**
     * 拒绝好友请求
     */
    public boolean rejectRequest(Long userId, Long friendId) {
        LambdaQueryWrapper<Friend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Friend::getFriendId, userId)
               .eq(Friend::getUserId, friendId)
               .eq(Friend::getStatus, 0);

        Friend request = getOne(wrapper);
        if (request != null) {
            request.setStatus(2); // 已拒绝
            updateById(request);
            return true;
        }
        return false;
    }

    /**
     * 删除好友
     */
    public boolean removeFriend(Long userId, Long friendId) {
        // 删除双向关系
        LambdaQueryWrapper<Friend> wrapper1 = new LambdaQueryWrapper<>();
        wrapper1.eq(Friend::getUserId, userId)
                .eq(Friend::getFriendId, friendId);
        remove(wrapper1);

        LambdaQueryWrapper<Friend> wrapper2 = new LambdaQueryWrapper<>();
        wrapper2.eq(Friend::getUserId, friendId)
                .eq(Friend::getFriendId, userId);
        remove(wrapper2);

        return true;
    }

    /**
     * 获取好友列表
     */
    public List<User> getFriends(Long userId) {
        List<Friend> friends = baseMapper.selectFriends(userId);
        List<User> result = new ArrayList<>();
        for (Friend friend : friends) {
            // 如果用户是发起方(user_id)，则好友是friend_id
            // 如果用户是被添加方(friend_id)，则好友是user_id
            Long friendUserId = friend.getUserId().equals(userId) ? friend.getFriendId() : friend.getUserId();
            User user = userService.getById(friendUserId);
            if (user != null) {
                result.add(user);
            }
        }
        return result;
    }

    /**
     * 获取待验证的好友请求
     */
    public List<User> getPendingRequests(Long userId) {
        List<Friend> requests = baseMapper.selectPendingRequests(userId);
        List<User> result = new ArrayList<>();
        for (Friend request : requests) {
            User user = userService.getById(request.getUserId());
            if (user != null) {
                result.add(user);
            }
        }
        return result;
    }

    /**
     * 搜索用户
     */
    public List<User> searchUsers(Long currentUserId, String keyword) {
        logger.info("[好友服务] 搜索用户，当前用户ID: {}, 关键词: {}", currentUserId, keyword);
        
        List<User> allUsers = userService.list();
        List<User> result = new ArrayList<>();
        
        logger.info("[好友服务] 数据库中共有用户数: {}", allUsers.size());
        
        for (User user : allUsers) {
            logger.debug("[好友服务] 检查用户: ID={}, username={}, nickname={}", 
                user.getId(), user.getUsername(), user.getNickname());
                
            if (!user.getId().equals(currentUserId)) {
                // 如果关键词为空，显示所有用户；否则按关键词过滤
                if (keyword == null || keyword.isEmpty() || 
                    user.getUsername().contains(keyword) || 
                    user.getNickname().contains(keyword)) {
                    result.add(user);
                    logger.debug("[好友服务] 添加用户到搜索结果: {}", user.getUsername());
                }
            }
        }
        
        logger.info("[好友服务] 搜索结果数量: {}", result.size());
        return result;
    }

    /**
     * 检查是否是好友
     */
    public boolean isFriend(Long userId, Long friendId) {
        Friend friendship = baseMapper.selectFriendship(userId, friendId);
        return friendship != null && friendship.getStatus() == 1;
    }
}