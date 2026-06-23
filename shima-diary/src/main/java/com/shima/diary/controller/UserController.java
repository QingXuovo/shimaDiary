package com.shima.diary.controller;

import com.shima.diary.common.Result;
import com.shima.diary.entity.User;
import com.shima.diary.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5算法不可用", e);
        }
    }

    private boolean isAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            User user = (User) session.getAttribute("user");
            if (user != null && "admin".equals(user.getRole())) {
                logger.debug("检查管理员权限: true");
                return true;
            }
        }
        logger.debug("检查管理员权限: false");
        return false;
    }
    
    private User getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            User user = (User) session.getAttribute("user");
            if (user != null) {
                return user;
            }
        }
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader != null && !userIdHeader.isEmpty()) {
            try {
                Long userId = Long.parseLong(userIdHeader);
                User user = userService.getById(userId);
                if (user != null) {
                    if (session != null) {
                        session.setAttribute("user", user);
                    }
                    return user;
                }
            } catch (NumberFormatException e) {
                logger.warn("无效的用户ID: {}", userIdHeader);
            }
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    @PostMapping("/login")
    public Result<User> login(@RequestBody Map<String, String> loginData, HttpSession session, HttpServletRequest request) {
        String username = loginData.get("username");
        String password = loginData.get("password");
        String clientIp = getClientIp(request);
        
        logger.info("用户登录请求: username={}, clientIp={}", username, clientIp);
        
        if (username == null || username.trim().isEmpty()) {
            return Result.error("用户名不能为空");
        }
        
        if (password == null || password.trim().isEmpty()) {
            return Result.error("密码不能为空");
        }
        
        try {
            User user = userService.login(username, password);
            
            if (user != null) {
                session.setAttribute("user", user);
                logger.info("登录成功: userId={}, username={}", user.getId(), user.getUsername());
                return Result.success("登录成功", user);
            } else {
                logger.warn("登录失败: 用户名或密码错误");
                return Result.error("用户名或密码错误");
            }
        } catch (Exception e) {
            logger.error("登录异常", e);
            return Result.error("登录失败，请稍后重试");
        }
    }

    @PostMapping("/register")
    public Result<User> register(@RequestBody Map<String, String> data, HttpServletRequest request) {
        String username = data.get("username");
        String password = data.get("password");
        String nickname = data.get("nickname");
        String clientIp = getClientIp(request);
        
        logger.info("用户注册请求: username={}, nickname={}, clientIp={}", username, nickname, clientIp);
        
        if (username == null || username.trim().isEmpty()) {
            return Result.error("用户名不能为空");
        }
        
        if (username.trim().length() < 3) {
            return Result.error("用户名长度不能少于3位");
        }
        
        if (password == null || password.trim().isEmpty()) {
            return Result.error("密码不能为空");
        }
        
        if (password.length() < 6) {
            return Result.error("密码长度不能少于6位");
        }
        
        try {
            User user = new User();
            user.setUsername(username);
            user.setPassword(password);
            user.setNickname(nickname != null ? nickname : username);
            user.setRole("user");
            
            boolean success = userService.register(user);
            
            if (success) {
                User registeredUser = userService.getByUsername(username);
                if (registeredUser != null) {
                    registeredUser.setPassword(null);
                }
                logger.info("注册成功: userId={}, username={}", registeredUser != null ? registeredUser.getId() : null, username);
                return Result.success("注册成功", registeredUser);
            } else {
                logger.warn("注册失败: 用户名已存在");
                return Result.error("用户名已存在");
            }
        } catch (Exception e) {
            logger.error("注册异常", e);
            return Result.error("注册失败，请稍后重试");
        }
    }

    @GetMapping("/current")
    public Result<User> getCurrentUser(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null) {
            logger.debug("获取当前用户: userId={}, username={}", user.getId(), user.getUsername());
            return Result.success(user);
        }
        logger.debug("获取当前用户失败: 未登录");
        return Result.error("未登录");
    }

    @PostMapping("/logout")
    public Result<Boolean> logout(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null) {
            logger.info("用户退出: userId={}, username={}", user.getId(), user.getUsername());
        }
        session.removeAttribute("user");
        return Result.success("退出成功", true);
    }

    @GetMapping("/logout")
    public Result<Boolean> logoutGet(HttpSession session) {
        return logout(session);
    }

    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody Map<String, String> data, HttpServletRequest request) {
        User currentUser = getCurrentUser(request);
        logger.info("更新用户信息: userId={}, data={}", id, data);
        
        if (currentUser == null || !currentUser.getId().equals(id)) {
            return Result.error("未登录或无权操作");
        }
        
        try {
            User user = new User();
            user.setId(id);
            user.setNickname(data.get("nickname"));
            
            boolean success = userService.updateUser(user);
            if (success) {
                User updatedUser = userService.getById(id);
                if (updatedUser != null) {
                    updatedUser.setPassword(null);
                    HttpSession session = request.getSession(false);
                    if (session != null) {
                        session.setAttribute("user", updatedUser);
                    }
                }
                logger.info("更新成功: userId={}", id);
            } else {
                logger.warn("更新失败: userId={}", id);
            }
            return success ? Result.success("更新成功", true) : Result.error("更新失败");
        } catch (Exception e) {
            logger.error("更新异常", e);
            return Result.error("更新失败");
        }
    }

    @PutMapping("/{id}/password")
    public Result<Boolean> updatePassword(@PathVariable Long id, @RequestBody Map<String, String> data, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        logger.info("修改密码请求: userId={}", id);
        
        if (currentUser == null || !currentUser.getId().equals(id)) {
            return Result.error("未登录或无权操作");
        }
        
        String oldPassword = data.get("oldPassword");
        String newPassword = data.get("newPassword");
        String confirmPassword = data.get("confirmPassword");
        
        if (oldPassword == null || newPassword == null || confirmPassword == null) {
            return Result.error("密码不能为空");
        }
        
        if (!newPassword.equals(confirmPassword)) {
            return Result.error("两次输入的新密码不一致");
        }
        
        if (newPassword.length() < 6) {
            return Result.error("新密码长度不能少于 6 位");
        }
        
        try {
            User user = userService.getById(id);
            if (user == null) {
                return Result.error("用户不存在");
            }
            
            if (!md5(oldPassword).equals(user.getPassword())) {
                return Result.error("旧密码错误");
            }
            
            User updateUser = new User();
            updateUser.setId(id);
            updateUser.setPassword(newPassword);
            boolean success = userService.updateUser(updateUser);
            
            if (success) {
                logger.info("密码修改成功: userId={}", id);
            } else {
                logger.warn("密码修改失败: userId={}", id);
            }
            return success ? Result.success("密码修改成功", true) : Result.error("密码修改失败");
        } catch (Exception e) {
            logger.error("修改密码异常", e);
            return Result.error("密码修改失败");
        }
    }

    @GetMapping("/admin/list")
    public Result<List<User>> getAllUsers(HttpServletRequest request) {
        logger.info("管理员获取所有用户列表");
        
        if (!isAdmin(request)) {
            return Result.error("无管理员权限");
        }
        try {
            List<User> users = userService.list();
            users.forEach(u -> u.setPassword(null));
            logger.info("获取用户列表成功: {}人", users.size());
            return Result.success(users);
        } catch (Exception e) {
            logger.error("获取用户列表失败", e);
            return Result.error("获取失败");
        }
    }

    @GetMapping("/admin/{id}")
    public Result<User> getUserById(@PathVariable Long id, HttpServletRequest request) {
        logger.info("管理员获取用户详情: userId={}", id);
        
        if (!isAdmin(request)) {
            return Result.error("无管理员权限");
        }
        try {
            User user = userService.getById(id);
            if (user != null) {
                user.setPassword(null);
                logger.info("获取用户成功: userId={}, username={}", id, user.getUsername());
                return Result.success(user);
            }
            logger.warn("用户不存在: userId={}", id);
            return Result.error("用户不存在");
        } catch (Exception e) {
            logger.error("获取用户详情失败", e);
            return Result.error("获取失败");
        }
    }

    @PutMapping("/admin/{id}/role")
    public Result<Boolean> updateUserRole(@PathVariable Long id, @RequestBody Map<String, String> data, HttpServletRequest request) {
        String role = data.get("role");
        logger.info("管理员更新用户角色: userId={}, role={}", id, role);
        
        if (!isAdmin(request)) {
            return Result.error("无管理员权限");
        }
        if (!"admin".equals(role) && !"user".equals(role)) {
            return Result.error("角色只能是 admin 或 user");
        }
        try {
            User user = new User();
            user.setId(id);
            user.setRole(role);
            boolean success = userService.updateUser(user);
            if (success) {
                logger.info("角色更新成功: userId={}, role={}", id, role);
            } else {
                logger.warn("角色更新失败: userId={}", id);
            }
            return success ? Result.success("角色更新成功", true) : Result.error("角色更新失败");
        } catch (Exception e) {
            logger.error("更新角色异常", e);
            return Result.error("角色更新失败");
        }
    }

    @DeleteMapping("/admin/{id}")
    public Result<Boolean> deleteUser(@PathVariable Long id, HttpServletRequest request) {
        logger.info("管理员删除用户: userId={}", id);
        
        if (!isAdmin(request)) {
            return Result.error("无管理员权限");
        }
        User currentUser = getCurrentUser(request);
        if (currentUser != null && currentUser.getId().equals(id)) {
            return Result.error("不能删除自己");
        }
        try {
            boolean success = userService.deleteUser(id);
            if (success) {
                logger.info("删除用户成功: userId={}", id);
            } else {
                logger.warn("删除用户失败: userId={}", id);
            }
            return success ? Result.success("删除成功", true) : Result.error("删除失败");
        } catch (Exception e) {
            logger.error("删除用户异常", e);
            return Result.error("删除失败");
        }
    }
}