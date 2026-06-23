package com.shima.diary.interceptor;

import com.shima.diary.entity.User;
import com.shima.diary.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        
        String servletPath = request.getServletPath();
        if (servletPath.contains("/user/login") || servletPath.contains("/user/register")) {
            return true;
        }
        
        String requestURI = request.getRequestURI();
        if (requestURI.contains("/static/") || requestURI.contains("/index.html") || 
            requestURI.contains("/register.html") || requestURI.contains("/home.html") ||
            requestURI.contains("/admin.html") || requestURI.contains("/profile.html")) {
            return true;
        }
        
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            return true;
        }
        
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader != null && !userIdHeader.isEmpty()) {
            try {
                Long userId = Long.parseLong(userIdHeader);
                User user = userService.getById(userId);
                if (user != null) {
                    if (session == null) {
                        session = request.getSession();
                    }
                    session.setAttribute("user", user);
                    return true;
                }
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        Map<String, Object> result = new HashMap<>();
        result.put("code", 401);
        result.put("message", "请先登录");
        result.put("data", null);
        response.getWriter().write(objectMapper.writeValueAsString(result));
        return false;
    }
}