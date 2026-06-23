package com.shima.diary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shima.diary.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 日记分类Mapper
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

    /**
     * 根据用户ID查询分类列表
     */
    List<Category> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID和分类名称查询
     */
    Category selectByUserIdAndName(@Param("userId") Long userId, @Param("name") String name);
}