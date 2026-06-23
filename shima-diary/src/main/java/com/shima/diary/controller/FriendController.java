package com.shima.diary.controller;

import com.shima.diary.common.Result;
import com.shima.diary.entity.Friend;
import com.shima.diary.entity.User;
import com.shima.diary.service.FriendService;
import com.shima.diary.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;

/**
 * 好友控制器
 */
@RestController
@RequestMapping("/friend")
public class FriendController {

    private static final Logger logger = LoggerFactory.getLogger(FriendController.class);

    @Autowired
    private FriendService friendService;

    @Autowired
    private UserService userService;

    private User getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        User currentUser = null;
        if (session != null) {
            currentUser = (User) session.getAttribute("user");
            if (currentUser != null) {
                logger.debug("[好友控制器] 从Session获取用户，用户ID: {}", currentUser.getId());
                return currentUser;
            }
        }
        
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader != null && !userIdHeader.isEmpty()) {
            try {
                Long userId = Long.parseLong(userIdHeader);
                currentUser = userService.getById(userId);
                if (currentUser != null) {
                    logger.debug("[好友控制器] 从请求头X-User-Id获取用户，用户ID: {}", userId);
                    if (session != null) {
                        session.setAttribute("user", currentUser);
                    }
                    return currentUser;
                }
            } catch (NumberFormatException e) {
                logger.warn("[好友控制器] X-User-Id格式错误: {}", userIdHeader);
            }
        }
        
        return null;
    }

    /**
     * 搜索用户
     */
    @GetMapping("/search")
    public Result<List<User>> search(@RequestParam String keyword, HttpServletRequest request) {
        logger.info("[好友搜索] 收到搜索请求，关键词: {}", keyword);
        
        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            logger.warn("[好友搜索] 用户未登录");
            return Result.error("未登录");
        }
        
        logger.info("[好友搜索] 当前用户ID: {}", currentUser.getId());
        List<User> users = friendService.searchUsers(currentUser.getId(), keyword);
        logger.info("[好友搜索] 搜索结果数量: {}", users != null ? users.size() : 0);
        return Result.success(users);
    }

    /**
     * 发送好友请求
     */
    @PostMapping("/request/{friendId}")
    public Result<Friend> sendRequest(@PathVariable Long friendId, HttpServletRequest request) {
        logger.info("[好友请求] 收到好友请求，目标用户ID: {}", friendId);
        
        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            logger.warn("[好友请求] 用户未登录");
            return Result.error("未登录");
        }

        logger.info("[好友请求] 当前用户ID: {}, 用户名: {}", currentUser.getId(), currentUser.getUsername());

        if (currentUser.getId().equals(friendId)) {
            logger.warn("[好友请求] 用户尝试添加自己为好友");
            return Result.error("不能添加自己为好友");
        }

        Friend existing = friendService.getBaseMapper().selectFriendship(currentUser.getId(), friendId);
        if (existing != null) {
            logger.info("[好友请求] 已存在好友关系，状态: {}", existing.getStatus());
            if (existing.getStatus() == 1) {
                return Result.error("已经是好友关系");
            } else {
                return Result.error("已有待处理的好友请求");
            }
        }

        logger.info("[好友请求] 调用服务层发送好友请求...");
        Friend friend = friendService.sendRequest(currentUser.getId(), friendId);
        
        if (friend != null) {
            logger.info("[好友请求] 好友请求发送成功，好友关系ID: {}", friend.getId());
            return Result.success("好友请求已发送", friend);
        }
        
        logger.error("[好友请求] 用户不存在，用户ID: {}", friendId);
        return Result.error("用户不存在");
    }

    /**
     * 接受好友请求
     */
    @PostMapping("/accept/{friendId}")
    public Result<Boolean> acceptRequest(@PathVariable Long friendId, HttpServletRequest request) {
        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            return Result.error("未登录");
        }

        boolean success = friendService.acceptRequest(currentUser.getId(), friendId);
        return success ? Result.success("已接受好友请求", true) : Result.error("接受失败");
    }

    /**
     * 拒绝好友请求
     */
    @PostMapping("/reject/{friendId}")
    public Result<Boolean> rejectRequest(@PathVariable Long friendId, HttpServletRequest request) {
        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            return Result.error("未登录");
        }

        boolean success = friendService.rejectRequest(currentUser.getId(), friendId);
        return success ? Result.success("已拒绝好友请求", true) : Result.error("拒绝失败");
    }

    /**
     * 删除好友
     */
    @DeleteMapping("/{friendId}")
    public Result<Boolean> removeFriend(@PathVariable Long friendId, HttpServletRequest request) {
        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            return Result.error("未登录");
        }

        boolean success = friendService.removeFriend(currentUser.getId(), friendId);
        return success ? Result.success("已删除好友", true) : Result.error("删除失败");
    }

    /**
     * 获取好友列表
     */
    @GetMapping("/list")
    public Result<List<User>> getFriends(HttpServletRequest request) {
        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            return Result.error("未登录");
        }
        List<User> friends = friendService.getFriends(currentUser.getId());
        return Result.success(friends);
    }

    /**
     * 获取待验证的好友请求
     */
    @GetMapping("/requests")
    public Result<List<User>> getPendingRequests(HttpServletRequest request) {
        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            return Result.error("未登录");
        }
        List<User> requests = friendService.getPendingRequests(currentUser.getId());
        return Result.success(requests);
    }

    /**
     * 检查是否是好友
     */
    @GetMapping("/check/{friendId}")
    public Result<Boolean> checkFriendship(@PathVariable Long friendId, HttpServletRequest request) {
        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            return Result.error("未登录");
        }
        boolean isFriend = friendService.isFriend(currentUser.getId(), friendId);
        return Result.success(isFriend);
    }
}