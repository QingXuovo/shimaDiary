package com.shima.diary.controller;

import com.shima.diary.common.Result;
import com.shima.diary.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redis缓存测试控制器
 */
@Slf4j
@RestController
@RequestMapping("/redis")
public class RedisTestController {

    @Autowired
    private RedisService redisService;

    /**
     * 设置缓存
     */
    @RequestMapping(value = "/set", method = {RequestMethod.GET, RequestMethod.POST})
    public Result<Boolean> set(@RequestParam String key, @RequestParam String value, 
                               @RequestParam(required = false, defaultValue = "0") long expireSeconds) {
        log.info("设置缓存: key={}, value={}, expireSeconds={}", key, value, expireSeconds);
        try {
            if (expireSeconds > 0) {
                redisService.setEx(key, value, expireSeconds);
            } else {
                redisService.set(key, value);
            }
            return Result.success("缓存设置成功", true);
        } catch (Exception e) {
            log.error("设置缓存失败: {}", e.getMessage());
            return Result.error("设置缓存失败: " + e.getMessage());
        }
    }

    /**
     * 获取缓存
     */
    @GetMapping("/get")
    public Result<Object> get(@RequestParam String key) {
        log.info("获取缓存: key={}", key);
        try {
            Object value = redisService.get(key);
            if (value != null) {
                return Result.success("缓存获取成功", value);
            } else {
                return Result.error("缓存不存在");
            }
        } catch (Exception e) {
            log.error("获取缓存失败: {}", e.getMessage());
            return Result.error("获取缓存失败: " + e.getMessage());
        }
    }

    /**
     * 删除缓存
     */
    @DeleteMapping("/delete")
    public Result<Boolean> delete(@RequestParam String key) {
        log.info("删除缓存: key={}", key);
        try {
            Boolean result = redisService.delete(key);
            return Result.success("缓存删除成功", result);
        } catch (Exception e) {
            log.error("删除缓存失败: {}", e.getMessage());
            return Result.error("删除缓存失败: " + e.getMessage());
        }
    }

    /**
     * 判断缓存是否存在
     */
    @GetMapping("/exists")
    public Result<Boolean> exists(@RequestParam String key) {
        log.info("判断缓存是否存在: key={}", key);
        try {
            Boolean result = redisService.hasKey(key);
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("查询缓存失败: {}", e.getMessage());
            return Result.error("查询缓存失败: " + e.getMessage());
        }
    }

    /**
     * 自增操作
     */
    @RequestMapping(value = "/increment", method = {RequestMethod.GET, RequestMethod.POST})
    public Result<Long> increment(@RequestParam String key, 
                                  @RequestParam(required = false, defaultValue = "1") long delta) {
        log.info("自增操作: key={}, delta={}", key, delta);
        try {
            Long result = redisService.increment(key, delta);
            return Result.success("自增成功", result);
        } catch (Exception e) {
            log.error("自增失败: {}", e.getMessage());
            return Result.error("自增失败: " + e.getMessage());
        }
    }

    /**
     * Hash操作测试
     */
    @PostMapping("/hash/set")
    public Result<Boolean> hSet(@RequestParam String key, @RequestParam String field, @RequestParam String value) {
        log.info("Hash设置: key={}, field={}, value={}", key, field, value);
        try {
            redisService.hSet(key, field, value);
            return Result.success("Hash设置成功", true);
        } catch (Exception e) {
            log.error("Hash设置失败: {}", e.getMessage());
            return Result.error("Hash设置失败: " + e.getMessage());
        }
    }

    /**
     * Hash获取
     */
    @GetMapping("/hash/get")
    public Result<Object> hGet(@RequestParam String key, @RequestParam String field) {
        log.info("Hash获取: key={}, field={}", key, field);
        try {
            Object value = redisService.hGet(key, field);
            if (value != null) {
                return Result.success("Hash获取成功", value);
            } else {
                return Result.error("Hash字段不存在");
            }
        } catch (Exception e) {
            log.error("Hash获取失败: {}", e.getMessage());
            return Result.error("Hash获取失败: " + e.getMessage());
        }
    }

    /**
     * Redis状态测试
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        log.info("检查Redis状态");
        try {
            Map<String, Object> status = new HashMap<>();
            
            if (!redisService.isAvailable()) {
                status.put("connected", false);
                status.put("message", "Redis未安装或未启动，缓存功能已禁用，应用可正常运行");
                status.put("hint", "如需启用缓存，请安装Redis并启动服务");
                return new Result<>(500, "Redis未连接", status);
            }
            
            // 测试基本操作
            String testKey = "redis:test:status";
            redisService.set(testKey, "ok", 10, TimeUnit.SECONDS);
            Object value = redisService.get(testKey);
            boolean exists = redisService.hasKey(testKey);
            
            status.put("connected", true);
            status.put("testKey", testKey);
            status.put("testValue", value);
            status.put("exists", exists);
            status.put("message", "Redis连接正常");
            
            return Result.success("Redis状态检查成功", status);
        } catch (Exception e) {
            log.error("Redis状态检查失败: {}", e.getMessage());
            Map<String, Object> status = new HashMap<>();
            status.put("connected", false);
            status.put("error", e.getMessage());
            return Result.error("Redis连接失败: " + e.getMessage());
        }
    }

    /**
     * 缓存性能测试
     */
    @GetMapping("/performance")
    public Result<Map<String, Object>> performance(@RequestParam(required = false, defaultValue = "1000") int count) {
        log.info("缓存性能测试: count={}", count);
        try {
            Map<String, Object> result = new HashMap<>();
            
            // 写入测试
            long writeStart = System.currentTimeMillis();
            for (int i = 0; i < count; i++) {
                redisService.set("perf:test:" + i, "value" + i);
            }
            long writeTime = System.currentTimeMillis() - writeStart;
            
            // 读取测试
            long readStart = System.currentTimeMillis();
            for (int i = 0; i < count; i++) {
                redisService.get("perf:test:" + i);
            }
            long readTime = System.currentTimeMillis() - readStart;
            
            // 清理测试数据
            for (int i = 0; i < count; i++) {
                redisService.delete("perf:test:" + i);
            }
            
            result.put("count", count);
            result.put("writeTimeMs", writeTime);
            result.put("readTimeMs", readTime);
            result.put("avgWriteTimeMs", (double) writeTime / count);
            result.put("avgReadTimeMs", (double) readTime / count);
            result.put("writePerSecond", (double) count * 1000 / writeTime);
            result.put("readPerSecond", (double) count * 1000 / readTime);
            
            return Result.success("性能测试完成", result);
        } catch (Exception e) {
            log.error("性能测试失败: {}", e.getMessage());
            return Result.error("性能测试失败: " + e.getMessage());
        }
    }
}