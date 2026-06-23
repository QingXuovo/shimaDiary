package com.shima.diary.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Redis缓存服务
 * 提供统一的缓存操作接口
 * 当Redis不可用时，所有操作会静默失败，不影响业务
 */
@Slf4j
@Service
public class RedisService {

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 检查Redis是否可用
     */
    public boolean isAvailable() {
        return redisTemplate != null;
    }

    /**
     * 设置缓存
     */
    public void set(String key, Object value) {
        if (!isAvailable()) return;
        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            log.warn("Redis set operation failed: {}", e.getMessage());
        }
    }

    /**
     * 设置缓存并指定过期时间
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        if (!isAvailable()) return;
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
        } catch (Exception e) {
            log.warn("Redis set operation failed: {}", e.getMessage());
        }
    }

    /**
     * 设置缓存并指定过期时间（秒）
     */
    public void setEx(String key, Object value, long seconds) {
        if (!isAvailable()) return;
        try {
            redisTemplate.opsForValue().set(key, value, seconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis setEx operation failed: {}", e.getMessage());
        }
    }

    /**
     * 获取缓存
     */
    public Object get(String key) {
        if (!isAvailable()) return null;
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Redis get operation failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取缓存并转换为指定类型
     */
    public <T> T get(String key, Class<T> clazz) {
        if (!isAvailable()) return null;
        Object value = get(key);
        if (value != null && clazz.isInstance(value)) {
            return clazz.cast(value);
        }
        return null;
    }

    /**
     * 删除缓存
     */
    public Boolean delete(String key) {
        if (!isAvailable()) return false;
        try {
            return redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Redis delete operation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 批量删除缓存
     */
    public Long delete(Collection<String> keys) {
        if (!isAvailable()) return 0L;
        try {
            return redisTemplate.delete(keys);
        } catch (Exception e) {
            log.warn("Redis batch delete operation failed: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * 判断缓存是否存在
     */
    public Boolean hasKey(String key) {
        if (!isAvailable()) return false;
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.warn("Redis hasKey operation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 设置过期时间
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        if (!isAvailable()) return false;
        try {
            return redisTemplate.expire(key, timeout, unit);
        } catch (Exception e) {
            log.warn("Redis expire operation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取过期时间
     */
    public Long getExpire(String key) {
        if (!isAvailable()) return -1L;
        try {
            return redisTemplate.getExpire(key);
        } catch (Exception e) {
            log.warn("Redis getExpire operation failed: {}", e.getMessage());
            return -1L;
        }
    }

    /**
     * 自增
     */
    public Long increment(String key) {
        if (!isAvailable()) return 0L;
        try {
            return redisTemplate.opsForValue().increment(key);
        } catch (Exception e) {
            log.warn("Redis increment operation failed: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * 自增指定值
     */
    public Long increment(String key, long delta) {
        if (!isAvailable()) return 0L;
        try {
            return redisTemplate.opsForValue().increment(key, delta);
        } catch (Exception e) {
            log.warn("Redis increment operation failed: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * 自减
     */
    public Long decrement(String key) {
        if (!isAvailable()) return 0L;
        try {
            return redisTemplate.opsForValue().decrement(key);
        } catch (Exception e) {
            log.warn("Redis decrement operation failed: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * 自减指定值
     */
    public Long decrement(String key, long delta) {
        if (!isAvailable()) return 0L;
        try {
            return redisTemplate.opsForValue().decrement(key, delta);
        } catch (Exception e) {
            log.warn("Redis decrement operation failed: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * Hash操作 - 设置
     */
    public void hSet(String key, String hashKey, Object value) {
        if (!isAvailable()) return;
        try {
            redisTemplate.opsForHash().put(key, hashKey, value);
        } catch (Exception e) {
            log.warn("Redis hSet operation failed: {}", e.getMessage());
        }
    }

    /**
     * Hash操作 - 获取
     */
    public Object hGet(String key, String hashKey) {
        if (!isAvailable()) return null;
        try {
            return redisTemplate.opsForHash().get(key, hashKey);
        } catch (Exception e) {
            log.warn("Redis hGet operation failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Hash操作 - 删除
     */
    public Long hDelete(String key, Object... hashKeys) {
        if (!isAvailable()) return 0L;
        try {
            return redisTemplate.opsForHash().delete(key, hashKeys);
        } catch (Exception e) {
            log.warn("Redis hDelete operation failed: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * Hash操作 - 判断是否存在
     */
    public Boolean hHasKey(String key, String hashKey) {
        if (!isAvailable()) return false;
        try {
            return redisTemplate.opsForHash().hasKey(key, hashKey);
        } catch (Exception e) {
            log.warn("Redis hHasKey operation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * List操作 - 左推入
     */
    public Long lPush(String key, Object value) {
        if (!isAvailable()) return 0L;
        try {
            return redisTemplate.opsForList().leftPush(key, value);
        } catch (Exception e) {
            log.warn("Redis lPush operation failed: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * List操作 - 右推入
     */
    public Long rPush(String key, Object value) {
        if (!isAvailable()) return 0L;
        try {
            return redisTemplate.opsForList().rightPush(key, value);
        } catch (Exception e) {
            log.warn("Redis rPush operation failed: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * List操作 - 左弹出
     */
    public Object lPop(String key) {
        if (!isAvailable()) return null;
        try {
            return redisTemplate.opsForList().leftPop(key);
        } catch (Exception e) {
            log.warn("Redis lPop operation failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * List操作 - 右弹出
     */
    public Object rPop(String key) {
        if (!isAvailable()) return null;
        try {
            return redisTemplate.opsForList().rightPop(key);
        } catch (Exception e) {
            log.warn("Redis rPop operation failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * List操作 - 获取范围
     */
    public java.util.List<Object> lRange(String key, long start, long end) {
        if (!isAvailable()) return null;
        try {
            return redisTemplate.opsForList().range(key, start, end);
        } catch (Exception e) {
            log.warn("Redis lRange operation failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Set操作 - 添加
     */
    public Long sAdd(String key, Object... values) {
        if (!isAvailable()) return 0L;
        try {
            return redisTemplate.opsForSet().add(key, values);
        } catch (Exception e) {
            log.warn("Redis sAdd operation failed: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * Set操作 - 获取所有成员
     */
    public java.util.Set<Object> sMembers(String key) {
        if (!isAvailable()) return null;
        try {
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            log.warn("Redis sMembers operation failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Set操作 - 判断是否是成员
     */
    public Boolean sIsMember(String key, Object value) {
        if (!isAvailable()) return false;
        try {
            return redisTemplate.opsForSet().isMember(key, value);
        } catch (Exception e) {
            log.warn("Redis sIsMember operation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Set操作 - 删除
     */
    public Long sRemove(String key, Object... values) {
        if (!isAvailable()) return 0L;
        try {
            return redisTemplate.opsForSet().remove(key, values);
        } catch (Exception e) {
            log.warn("Redis sRemove operation failed: {}", e.getMessage());
            return 0L;
        }
    }
}