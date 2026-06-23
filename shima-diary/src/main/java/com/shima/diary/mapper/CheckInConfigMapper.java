package com.shima.diary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shima.diary.entity.CheckInConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 打卡配置 Mapper 接口
 */
@Mapper
public interface CheckInConfigMapper extends BaseMapper<CheckInConfig> {
}
