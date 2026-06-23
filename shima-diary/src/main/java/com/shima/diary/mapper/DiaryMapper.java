package com.shima.diary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shima.diary.entity.Diary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDate;
import java.util.List;

/**
 * 日记 Mapper 接口
 */
@Mapper
public interface DiaryMapper extends BaseMapper<Diary> {
    
    /**
     * 根据日期范围查询日记
     */
    List<Diary> selectByDateRange(@Param("userId") Long userId, 
                                   @Param("startDate") LocalDate startDate, 
                                   @Param("endDate") LocalDate endDate);
    
    /**
     * 根据心情统计
     */
    int countByMood(@Param("userId") Long userId, @Param("mood") String mood);
}
