package com.shima.diary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shima.diary.entity.DiaryShare;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 日记分享Mapper
 */
@Mapper
public interface DiaryShareMapper extends BaseMapper<DiaryShare> {

    /**
     * 根据分享token查询
     */
    DiaryShare selectByToken(@Param("shareToken") String shareToken);
}