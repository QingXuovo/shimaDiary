package com.shima.diary.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shima.diary.common.Result;
import com.shima.diary.entity.Task;
import com.shima.diary.entity.TaskTag;
import com.shima.diary.entity.User;
import com.shima.diary.service.TaskService;
import com.shima.diary.service.TaskTagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务控制器
 */
@RestController
@RequestMapping("/task")
public class TaskController {

    private static final Logger logger = LoggerFactory.getLogger(TaskController.class);

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskTagService taskTagService;

    /**
     * 获取当前登录用户ID
     */
    private Long getUserId(HttpSession session) {
        User user = (User) session.getAttribute("user");
        Long userId = user != null ? user.getId() : 1L;
        logger.debug("获取用户ID: {}", userId);
        return userId;
    }

    /**
     * 检查是否是管理员
     */
    private boolean isAdmin(HttpSession session) {
        User user = (User) session.getAttribute("user");
        boolean admin = user != null && "admin".equals(user.getRole());
        logger.debug("检查管理员权限: {}", admin);
        return admin;
    }

    /**
     * 获取所有任务列表（不分页）
     */
    @GetMapping
    public Result<List<Task>> getAll(HttpSession session) {
        logger.info("获取所有任务列表请求");
        Long userId = getUserId(session);
        boolean admin = isAdmin(session);
        try {
            List<Task> tasks;
            if (admin) {
                // 管理员可以查看所有用户的任务
                tasks = taskService.list(new LambdaQueryWrapper<Task>()
                    .orderByDesc(Task::getCreateTime));
            } else {
                // 普通用户只能查看自己的任务
                tasks = taskService.list(new LambdaQueryWrapper<Task>()
                    .eq(Task::getUserId, userId)
                    .orderByDesc(Task::getCreateTime));
            }
            logger.info("获取任务成功，共{}条", tasks.size());
            return Result.success(tasks);
        } catch (Exception e) {
            logger.error("获取任务列表失败", e);
            return Result.error("获取失败");
        }
    }

    /**
     * 分页查询任务列表
     */
    @GetMapping("/list")
    public Result<Page<Task>> list(@RequestParam(required = false) Integer status,
                                    @RequestParam(defaultValue = "1") int pageNum,
                                    @RequestParam(defaultValue = "10") int pageSize,
                                    HttpSession session) {
        logger.info("分页查询任务: status={}, pageNum={}, pageSize={}", status, pageNum, pageSize);
        Long userId = getUserId(session);
        try {
            Page<Task> page = taskService.pageList(userId, status, pageNum, pageSize);
            logger.info("分页查询成功: 总数={}", page.getTotal());
            return Result.success(page);
        } catch (Exception e) {
            logger.error("分页查询任务失败", e);
            return Result.error("查询失败");
        }
    }

    /**
     * 根据ID获取任务详情
     */
    @GetMapping("/{id}")
    public Result<Task> getById(@PathVariable Long id) {
        logger.info("获取任务详情: id={}", id);
        try {
            Task task = taskService.getById(id);
            if (task != null) {
                logger.info("获取任务成功: id={}, title={}", id, task.getTitle());
            } else {
                logger.warn("任务不存在: id={}", id);
            }
            return Result.success(task);
        } catch (Exception e) {
            logger.error("获取任务详情失败: id={}", id, e);
            return Result.error("获取失败");
        }
    }

    /**
     * 创建任务
     */
    @PostMapping
    public Result<Task> create(@RequestBody Task task, HttpSession session) {
        logger.info("创建任务请求: title={}, status={}, priority={}", 
            task.getTitle(), task.getStatus(), task.getPriority());
        Long userId = getUserId(session);
        task.setUserId(userId);
        try {
            boolean success = taskService.createTask(task);
            if (success) {
                logger.info("创建任务成功: id={}, title={}", task.getId(), task.getTitle());
                return Result.success("创建成功", task);
            } else {
                logger.warn("创建任务失败");
                return Result.error("创建失败");
            }
        } catch (Exception e) {
            logger.error("创建任务异常", e);
            return Result.error("创建失败");
        }
    }

    /**
     * 更新任务
     */
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody Task task, HttpSession session) {
        logger.info("更新任务: id={}, title={}", id, task.getTitle());
        task.setId(id);
        
        Long userId = getUserId(session);
        boolean admin = isAdmin(session);
        
        try {
            // 检查权限：管理员可以更新任何用户的任务，普通用户只能更新自己的任务
            Task existing = taskService.getById(id);
            if (existing == null) {
                return Result.error("任务不存在");
            }
            
            if (!admin && !existing.getUserId().equals(userId)) {
                logger.warn("更新任务失败: 无权操作他人任务, userId={}, taskId={}", userId, id);
                return Result.error("无权操作");
            }
            
            boolean success = taskService.updateTask(task);
            if (success) {
                logger.info("更新任务成功: id={}", id);
                return Result.success("更新成功", true);
            } else {
                logger.warn("更新任务失败: id={}", id);
                return Result.error("更新失败");
            }
        } catch (Exception e) {
            logger.error("更新任务异常: id={}", id, e);
            return Result.error("更新失败");
        }
    }

    /**
     * 更新任务状态
     */
    @PutMapping("/{id}/status")
    @GetMapping("/{id}/status")
    public Result<Boolean> updateStatus(@PathVariable Long id, 
                                        @RequestParam(required = false) Integer status,
                                        @RequestBody(required = false) Map<String, Object> body) {
        logger.info("更新任务状态: id={}, status={}", id, status);
        // 支持两种方式：URL参数和JSON body
        Integer targetStatus = status;
        if (targetStatus == null && body != null) {
            Object completed = body.get("completed");
            if (completed != null) {
                targetStatus = Boolean.TRUE.equals(completed) ? 1 : 0;
            }
        }
        if (targetStatus == null) {
            targetStatus = 1;
        }
        try {
            boolean success = taskService.updateStatus(id, targetStatus);
            if (success) {
                logger.info("任务状态更新成功: id={}, status={}", id, targetStatus);
                return Result.success("状态更新成功", true);
            } else {
                logger.warn("任务状态更新失败: id={}", id);
                return Result.error("状态更新失败");
            }
        } catch (Exception e) {
            logger.error("任务状态更新异常: id={}", id, e);
            return Result.error("状态更新失败");
        }
    }

    /**
     * 获取任务数量
     */
    @GetMapping("/count")
    public Result<Integer> count(HttpSession session) {
        logger.info("获取任务数量统计");
        Long userId = getUserId(session);
        try {
            int count = (int) taskService.count(new LambdaQueryWrapper<Task>().eq(Task::getUserId, userId));
            logger.info("任务数量: {}", count);
            return Result.success(count);
        } catch (Exception e) {
            logger.error("获取任务数量失败", e);
            return Result.error("获取失败");
        }
    }

    /**
     * 更新任务进度
     */
    @PutMapping("/{id}/progress")
    public Result<Boolean> updateProgress(@PathVariable Long id, @RequestParam Integer progress) {
        logger.info("更新任务进度: id={}, progress={}", id, progress);
        try {
            boolean success = taskService.updateProgress(id, progress);
            if (success) {
                logger.info("任务进度更新成功: id={}", id);
                return Result.success("进度更新成功", true);
            } else {
                logger.warn("任务进度更新失败: id={}", id);
                return Result.error("进度更新失败");
            }
        } catch (Exception e) {
            logger.error("任务进度更新异常: id={}", id, e);
            return Result.error("进度更新失败");
        }
    }

    /**
     * 删除任务
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id, HttpSession session) {
        logger.info("删除任务请求: id={}", id);
        
        Long userId = getUserId(session);
        boolean admin = isAdmin(session);
        
        try {
            // 检查权限：管理员可以删除任何用户的任务，普通用户只能删除自己的任务
            Task existing = taskService.getById(id);
            if (existing == null) {
                return Result.error("任务不存在");
            }
            
            if (!admin && !existing.getUserId().equals(userId)) {
                logger.warn("删除任务失败: 无权操作他人任务, userId={}, taskId={}", userId, id);
                return Result.error("无权操作");
            }
            
            boolean success = taskService.deleteTask(id);
            if (success) {
                logger.info("删除任务成功: id={}", id);
                return Result.success("删除成功", true);
            } else {
                logger.warn("删除任务失败: id={}", id);
                return Result.error("删除失败");
            }
        } catch (Exception e) {
            logger.error("删除任务异常: id={}", id, e);
            return Result.error("删除失败");
        }
    }

    /**
     * 查询子任务
     */
    @GetMapping("/{id}/subtasks")
    public Result<List<Task>> getSubTasks(@PathVariable Long id) {
        logger.info("查询子任务: parentId={}", id);
        try {
            List<Task> subTasks = taskService.getSubTasks(id);
            logger.info("查询子任务成功: {}条", subTasks.size());
            return Result.success(subTasks);
        } catch (Exception e) {
            logger.error("查询子任务失败: parentId={}", id, e);
            return Result.error("查询失败");
        }
    }

    /**
     * 查询今日到期任务
     */
    @GetMapping("/today")
    public Result<List<Task>> getTodayDue(HttpSession session) {
        logger.info("查询今日到期任务");
        Long userId = getUserId(session);
        try {
            List<Task> tasks = taskService.getTodayDue(userId);
            logger.info("今日到期任务: {}条", tasks.size());
            return Result.success(tasks);
        } catch (Exception e) {
            logger.error("查询今日到期任务失败", e);
            return Result.error("查询失败");
        }
    }

    /**
     * 查询过期未完成任务
     */
    @GetMapping("/overdue")
    public Result<List<Task>> getOverdue(HttpSession session) {
        logger.info("查询过期未完成任务");
        Long userId = getUserId(session);
        try {
            List<Task> tasks = taskService.getOverdue(userId);
            logger.info("过期未完成任务: {}条", tasks.size());
            return Result.success(tasks);
        } catch (Exception e) {
            logger.error("查询过期任务失败", e);
            return Result.error("查询失败");
        }
    }

    /**
     * 按分类查询任务
     */
    @GetMapping("/category/{category}")
    public Result<List<Task>> getByCategory(@PathVariable String category, HttpSession session) {
        logger.info("按分类查询任务: category={}", category);
        Long userId = getUserId(session);
        try {
            List<Task> tasks = taskService.getByCategory(userId, category);
            logger.info("分类查询成功: {}条", tasks.size());
            return Result.success(tasks);
        } catch (Exception e) {
            logger.error("分类查询失败: category={}", category, e);
            return Result.error("查询失败");
        }
    }

    /**
     * 获取任务统计
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats(HttpSession session) {
        logger.info("获取任务统计");
        Long userId = getUserId(session);
        try {
            TaskService.TaskStats stats = taskService.getStats(userId);
            Map<String, Object> result = new HashMap<>();
            result.put("total", stats.getTotal());
            result.put("pending", stats.getPending());
            result.put("inProgress", stats.getInProgress());
            result.put("completed", stats.getCompleted());
            result.put("overdue", stats.getOverdue());
            result.put("completionRate", stats.getCompletionRate());
            logger.info("任务统计获取成功: total={}, completed={}", stats.getTotal(), stats.getCompleted());
            return Result.success(result);
        } catch (Exception e) {
            logger.error("获取任务统计失败", e);
            return Result.error("获取失败");
        }
    }

    /**
     * 按优先级查询任务
     */
    @GetMapping("/priority/{priority}")
    public Result<List<Task>> getByPriority(@PathVariable Integer priority, HttpSession session) {
        logger.info("按优先级查询任务: priority={}", priority);
        Long userId = getUserId(session);
        try {
            List<Task> tasks = taskService.getByPriority(userId, priority);
            logger.info("优先级查询成功: {}条", tasks.size());
            return Result.success(tasks);
        } catch (Exception e) {
            logger.error("优先级查询失败: priority={}", priority, e);
            return Result.error("查询失败");
        }
    }

    /**
     * 按标签搜索任务
     */
    @GetMapping("/tag/{tag}")
    public Result<List<Task>> searchByTag(@PathVariable String tag, HttpSession session) {
        logger.info("按标签搜索任务: tag={}", tag);
        Long userId = getUserId(session);
        try {
            List<Task> tasks = taskService.searchByTag(userId, tag);
            logger.info("标签搜索成功: {}条", tasks.size());
            return Result.success(tasks);
        } catch (Exception e) {
            logger.error("标签搜索失败: tag={}", tag, e);
            return Result.error("搜索失败");
        }
    }

    /**
     * 搜索任务
     */
    @GetMapping("/search")
    public Result<List<Task>> search(@RequestParam String keyword, HttpSession session) {
        logger.info("搜索任务: keyword={}", keyword);
        Long userId = getUserId(session);
        try {
            List<Task> tasks = taskService.search(userId, keyword);
            logger.info("搜索成功: {}条", tasks.size());
            return Result.success(tasks);
        } catch (Exception e) {
            logger.error("搜索任务失败: keyword={}", keyword, e);
            return Result.error("搜索失败");
        }
    }

    // ==================== 任务标签相关 ====================

    /**
     * 获取任务标签
     */
    @GetMapping("/{id}/tags")
    public Result<List<TaskTag>> getTags(@PathVariable Long id) {
        logger.info("获取任务标签: taskId={}", id);
        try {
            List<TaskTag> tags = taskTagService.getTagsByTaskId(id);
            logger.info("获取标签成功: {}条", tags.size());
            return Result.success(tags);
        } catch (Exception e) {
            logger.error("获取任务标签失败: taskId={}", id, e);
            return Result.error("获取失败");
        }
    }

    /**
     * 添加任务标签
     */
    @PostMapping("/{id}/tags")
    public Result<Boolean> addTag(@PathVariable Long id, @RequestBody TaskTag tag) {
        logger.info("添加任务标签: taskId={}, tagName={}", id, tag.getTagName());
        tag.setTaskId(id);
        try {
            boolean success = taskTagService.addTag(tag);
            if (success) {
                logger.info("添加标签成功: taskId={}", id);
                return Result.success("添加成功", true);
            } else {
                logger.warn("添加标签失败: taskId={}", id);
                return Result.error("添加失败");
            }
        } catch (Exception e) {
            logger.error("添加标签异常: taskId={}", id, e);
            return Result.error("添加失败");
        }
    }

    /**
     * 删除任务标签
     */
    @DeleteMapping("/tags/{tagId}")
    public Result<Boolean> deleteTag(@PathVariable Long tagId) {
        logger.info("删除任务标签: tagId={}", tagId);
        try {
            boolean success = taskTagService.deleteTag(tagId);
            if (success) {
                logger.info("删除标签成功: tagId={}", tagId);
                return Result.success("删除成功", true);
            } else {
                logger.warn("删除标签失败: tagId={}", tagId);
                return Result.error("删除失败");
            }
        } catch (Exception e) {
            logger.error("删除标签异常: tagId={}", tagId, e);
            return Result.error("删除失败");
        }
    }
}