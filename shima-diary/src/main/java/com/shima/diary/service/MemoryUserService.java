package com.shima.diary.service;

import com.shima.diary.entity.User;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MemoryUserService {

    private final ConcurrentHashMap<Long, User> users = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, User> usersByUsername = new ConcurrentHashMap<>();
    private long nextId = 1;

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @PostConstruct
    public void init() {
        // 创建默认管理员用户
        User admin = new User();
        admin.setId(nextId++);
        admin.setUsername("admin");
        admin.setPassword(md5("123456"));
        admin.setNickname("管理员");
        admin.setRole("admin");
        admin.setDeleted(0);
        users.put(admin.getId(), admin);
        usersByUsername.put(admin.getUsername(), admin);

        // 创建默认普通用户
        User user = new User();
        user.setId(nextId++);
        user.setUsername("user");
        user.setPassword(md5("123456"));
        user.setNickname("普通用户");
        user.setRole("user");
        user.setDeleted(0);
        users.put(user.getId(), user);
        usersByUsername.put(user.getUsername(), user);
    }

    public User login(String username, String password) {
        User user = usersByUsername.get(username);
        if (user != null && user.getDeleted() != null && user.getDeleted() == 0) {
            if (md5(password).equals(user.getPassword())) {
                User result = new User();
                result.setId(user.getId());
                result.setUsername(user.getUsername());
                result.setNickname(user.getNickname());
                result.setRole(user.getRole());
                return result;
            }
        }
        return null;
    }

    public User register(String username, String password, String nickname) {
        if (usersByUsername.containsKey(username)) {
            return null;
        }

        User user = new User();
        user.setId(nextId++);
        user.setUsername(username);
        user.setPassword(md5(password));
        user.setNickname(nickname != null ? nickname : username);
        user.setRole("user");
        user.setDeleted(0);

        users.put(user.getId(), user);
        usersByUsername.put(user.getUsername(), user);

        User result = new User();
        result.setId(user.getId());
        result.setUsername(user.getUsername());
        result.setNickname(user.getNickname());
        result.setRole(user.getRole());
        return result;
    }

    public User getById(Long id) {
        User user = users.get(id);
        if (user != null && user.getDeleted() != null && user.getDeleted() == 0) {
            User result = new User();
            result.setId(user.getId());
            result.setUsername(user.getUsername());
            result.setNickname(user.getNickname());
            result.setRole(user.getRole());
            return result;
        }
        return null;
    }

    public User getByUsername(String username) {
        User user = usersByUsername.get(username);
        if (user != null && user.getDeleted() != null && user.getDeleted() == 0) {
            User result = new User();
            result.setId(user.getId());
            result.setUsername(user.getUsername());
            result.setNickname(user.getNickname());
            result.setRole(user.getRole());
            return result;
        }
        return null;
    }

    public List<User> listAll() {
        List<User> result = new ArrayList<>();
        users.values().forEach(user -> {
            if (user.getDeleted() != null && user.getDeleted() == 0) {
                User u = new User();
                u.setId(user.getId());
                u.setUsername(user.getUsername());
                u.setNickname(user.getNickname());
                u.setRole(user.getRole());
                result.add(u);
            }
        });
        return result;
    }

    public boolean update(User user) {
        User existing = users.get(user.getId());
        if (existing != null && existing.getDeleted() != null && existing.getDeleted() == 0) {
            if (user.getNickname() != null) {
                existing.setNickname(user.getNickname());
            }
            if (user.getRole() != null) {
                existing.setRole(user.getRole());
            }
            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                existing.setPassword(md5(user.getPassword()));
            }
            return true;
        }
        return false;
    }

    public boolean delete(Long id) {
        User user = users.get(id);
        if (user != null && user.getDeleted() != null && user.getDeleted() == 0) {
            user.setDeleted(1);
            return true;
        }
        return false;
    }

    /**
     * 验证密码
     */
    public boolean validatePassword(Long userId, String password) {
        User user = users.get(userId);
        if (user != null) {
            return md5(password).equals(user.getPassword());
        }
        return false;
    }
}