package com.shima.diary.config;

import com.shima.diary.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/api/vue/**")
                .addResourceLocations("classpath:/static/dist/");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/api/vue")
                .setViewName("forward:/api/vue/index.html");
        registry.addViewController("/api/vue/")
                .setViewName("forward:/api/vue/index.html");
        registry.addViewController("/api/vue/dashboard")
                .setViewName("forward:/api/vue/index.html");
        registry.addViewController("/api/vue/diary")
                .setViewName("forward:/api/vue/index.html");
        registry.addViewController("/api/vue/diary/*")
                .setViewName("forward:/api/vue/index.html");
        registry.addViewController("/api/vue/task")
                .setViewName("forward:/api/vue/index.html");
        registry.addViewController("/api/vue/task/*")
                .setViewName("forward:/api/vue/index.html");
        registry.addViewController("/api/vue/checkin")
                .setViewName("forward:/api/vue/index.html");
        registry.addViewController("/api/vue/friends")
                .setViewName("forward:/api/vue/index.html");
        registry.addViewController("/api/vue/categories")
                .setViewName("forward:/api/vue/index.html");
        registry.addViewController("/api/vue/profile")
                .setViewName("forward:/api/vue/index.html");
        registry.addViewController("/api/vue/register")
                .setViewName("forward:/api/vue/index.html");
        registry.addViewController("/api/vue/search")
                .setViewName("forward:/api/vue/index.html");
        registry.addViewController("/api/vue/stats")
                .setViewName("forward:/api/vue/index.html");
        registry.addViewController("/api/vue/admin")
                .setViewName("forward:/api/vue/index.html");
        registry.addViewController("/api/vue/archive")
                .setViewName("forward:/api/vue/index.html");
        registry.addViewController("/api/vue/mood-analysis")
                .setViewName("forward:/api/vue/index.html");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/api/vue/**")
                .excludePathPatterns("/user/login", "/user/register", "/user/current")
                .excludePathPatterns("/api/user/login", "/api/user/register", "/api/user/current")
                .excludePathPatterns("/ai/companion/**", "/api/ai/companion/**");
    }
}