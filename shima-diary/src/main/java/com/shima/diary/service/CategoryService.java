package com.shima.diary.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shima.diary.entity.Category;
import com.shima.diary.mapper.CategoryMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 日记分类服务
 */
@Service
public class CategoryService extends ServiceImpl<CategoryMapper, Category> {

    /**
     * 根据用户ID查询分类列表
     */
    public List<Category> getByUserId(Long userId) {
        return baseMapper.selectByUserId(userId);
    }

    /**
     * 创建分类
     */
    public Category create(Category category) {
        save(category);
        return category;
    }

    /**
     * 更新分类
     */
    public boolean updateCategory(Category category) {
        return updateById(category);
    }

    /**
     * 删除分类
     */
    public boolean delete(Long id) {
        return removeById(id);
    }

    /**
     * 检查分类名称是否已存在
     */
    public boolean existsByName(Long userId, String name) {
        Category category = baseMapper.selectByUserIdAndName(userId, name);
        return category != null;
    }
}