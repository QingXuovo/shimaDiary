package com.shima.diary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shima.diary.entity.CheckIn;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 打卡记录 Mapper 接口
 */
@Mapper
public interface CheckInMapper extends BaseMapper<CheckIn> {
    
    /**
     * 查询用户某天的打卡记录
     */
    List<CheckIn> selectByDate(@Param("userId") Long userId, @Param("checkDate") LocalDate checkDate);
    
    /**
     * 统计某类型的连续打卡天数
     */
    int countContinuousDays(@Param("userId") Long userId, @Param("checkType") String checkType);
    
    /**
     * 获取打卡统计
     */
    List<Map<String, Object>> selectCheckInStats(@Param("userId") Long userId);
}
