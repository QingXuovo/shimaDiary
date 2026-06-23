package com.shima.diary.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shima.diary.entity.CheckInConfig;
import com.shima.diary.mapper.CheckInConfigMapper;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 打卡配置服务类
 */
@Service
public class CheckInConfigService extends ServiceImpl<CheckInConfigMapper, CheckInConfig> {

    /**
     * 获取用户所有打卡配置
     */
    public List<CheckInConfig> getAllConfigs(Long userId) {
        LambdaQueryWrapper<CheckInConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CheckInConfig::getUserId, userId)
               .eq(CheckInConfig::getIsActive, 1)
               .orderByAsc(CheckInConfig::getId);
        return list(wrapper);
    }

    /**
     * 添加打卡配置
     */
    public boolean addConfig(CheckInConfig config) {
        config.setUserId(1L); // 默认用户
        if (config.getIsActive() == null) {
            config.setIsActive(1);
        }
        if (config.getTargetDays() == null) {
            config.setTargetDays(30);
        }
        return save(config);
    }

    /**
     * 更新打卡配置
     */
    public boolean updateConfig(CheckInConfig config) {
        return updateById(config);
    }

    /**
     * 删除打卡配置
     */
    public boolean deleteConfig(Long id) {
        return removeById(id);
    }

    /**
     * 根据类型获取配置
     */
    public CheckInConfig getByType(Long userId, String checkType) {
        LambdaQueryWrapper<CheckInConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CheckInConfig::getUserId, userId)
               .eq(CheckInConfig::getCheckType, checkType)
               .last("LIMIT 1");
        return getOne(wrapper);
    }
}
