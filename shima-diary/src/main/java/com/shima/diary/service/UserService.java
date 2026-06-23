package com.shima.diary.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shima.diary.entity.User;
import com.shima.diary.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class UserService extends ServiceImpl<UserMapper, User> {

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

    public User login(String username, String password) {
        User user = baseMapper.selectByUsername(username);
        if (user != null) {
            String encodedPassword = md5(password);
            if (encodedPassword.equals(user.getPassword())) {
                user.setPassword(null);
                return user;
            }
        }
        return null;
    }

    public boolean register(User user) {
        User existing = baseMapper.selectByUsername(user.getUsername());
        if (existing != null) {
            return false;
        }
        user.setPassword(md5(user.getPassword()));
        return save(user);
    }

    public boolean updateUser(User user) {
        return updateById(user);
    }

    public boolean deleteUser(Long id) {
        return removeById(id);
    }

    public User getByUsername(String username) {
        return baseMapper.selectByUsername(username);
    }
}