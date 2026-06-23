package com.shima.diary.controller;

import com.shima.diary.common.Result;
import com.shima.diary.entity.Category;
import com.shima.diary.entity.User;
import com.shima.diary.service.CategoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

/**
 * 日记分类控制器
 */
@RestController
@RequestMapping("/category")
public class CategoryController {

    private static final Logger logger = LoggerFactory.getLogger(CategoryController.class);

    @Autowired
    private CategoryService categoryService;

    /**
     * 获取当前用户的分类列表
     */
    @GetMapping("/list")
    public Result<List<Category>> getCategories(HttpSession session) {
        logger.info("获取分类列表请求");
        User user = (User) session.getAttribute("user");
        if (user == null) {
            logger.warn("获取分类失败: 未登录");
            return Result.error("未登录");
        }
        try {
            List<Category> categories = categoryService.getByUserId(user.getId());
            logger.info("获取分类成功: {}个", categories.size());
            return Result.success(categories);
        } catch (Exception e) {
            logger.error("获取分类失败", e);
            return Result.error("获取失败");
        }
    }

    /**
     * 创建分类
     */
    @PostMapping
    public Result<Category> create(@RequestBody Map<String, String> data, HttpSession session) {
        logger.info("创建分类请求: data={}", data);
        User user = (User) session.getAttribute("user");
        if (user == null) {
            logger.warn("创建分类失败: 未登录");
            return Result.error("未登录");
        }

        String name = data.get("name");
        if (name == null || name.trim().isEmpty()) {
            logger.warn("创建分类失败: 分类名称为空");
            return Result.error("分类名称不能为空");
        }

        try {
            if (categoryService.existsByName(user.getId(), name)) {
                logger.warn("创建分类失败: 分类名称已存在, name={}", name);
                return Result.error("分类名称已存在");
            }

            Category category = new Category();
            category.setUserId(user.getId());
            category.setName(name);
            category.setColor(data.getOrDefault("color", "#667eea"));
            category.setIcon(data.getOrDefault("icon", "📁"));
            category.setSortOrder(0);

            Category created = categoryService.create(category);
            logger.info("创建分类成功: id={}, name={}", created.getId(), created.getName());
            return Result.success("创建成功", created);
        } catch (Exception e) {
            logger.error("创建分类异常", e);
            return Result.error("创建失败");
        }
    }

    /**
     * 更新分类
     */
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody Map<String, String> data, HttpSession session) {
        logger.info("更新分类: id={}, data={}", id, data);
        User user = (User) session.getAttribute("user");
        if (user == null) {
            logger.warn("更新分类失败: 未登录");
            return Result.error("未登录");
        }

        try {
            Category category = categoryService.getById(id);
            if (category == null) {
                logger.warn("更新分类失败: 分类不存在, id={}", id);
                return Result.error("分类不存在");
            }

            if (!category.getUserId().equals(user.getId())) {
                logger.warn("更新分类失败: 无权操作, userId={}, categoryId={}", user.getId(), id);
                return Result.error("无权操作");
            }

            if (data.containsKey("name")) {
                String name = data.get("name");
                if (!name.isEmpty() && !name.equals(category.getName())) {
                    if (categoryService.existsByName(user.getId(), name)) {
                        logger.warn("更新分类失败: 分类名称已存在, name={}", name);
                        return Result.error("分类名称已存在");
                    }
                    category.setName(name);
                }
            }

            if (data.containsKey("color")) {
                category.setColor(data.get("color"));
            }

            if (data.containsKey("icon")) {
                category.setIcon(data.get("icon"));
            }

            boolean success = categoryService.updateCategory(category);
            if (success) {
                logger.info("更新分类成功: id={}", id);
            } else {
                logger.warn("更新分类失败: id={}", id);
            }
            return success ? Result.success("更新成功", true) : Result.error("更新失败");
        } catch (Exception e) {
            logger.error("更新分类异常: id={}", id, e);
            return Result.error("更新失败");
        }
    }

    /**
     * 删除分类
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id, HttpSession session) {
        logger.info("删除分类请求: id={}", id);
        User user = (User) session.getAttribute("user");
        if (user == null) {
            logger.warn("删除分类失败: 未登录");
            return Result.error("未登录");
        }

        try {
            Category category = categoryService.getById(id);
            if (category == null) {
                logger.warn("删除分类失败: 分类不存在, id={}", id);
                return Result.error("分类不存在");
            }

            if (!category.getUserId().equals(user.getId())) {
                logger.warn("删除分类失败: 无权操作, userId={}, categoryId={}", user.getId(), id);
                return Result.error("无权操作");
            }

            boolean success = categoryService.delete(id);
            if (success) {
                logger.info("删除分类成功: id={}", id);
            } else {
                logger.warn("删除分类失败: id={}", id);
            }
            return success ? Result.success("删除成功", true) : Result.error("删除失败");
        } catch (Exception e) {
            logger.error("删除分类异常: id={}", id, e);
            return Result.error("删除失败");
        }
    }
}