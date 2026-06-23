package com.shima.diary.controller;

import com.shima.diary.common.Result;
import com.shima.diary.entity.CheckinGoal;
import com.shima.diary.entity.User;
import com.shima.diary.service.CheckinGoalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 打卡目标控制器
 */
@RestController
@RequestMapping("/checkin/goal")
public class CheckinGoalController {

    private static final Logger logger = LoggerFactory.getLogger(CheckinGoalController.class);

    @Autowired
    private CheckinGoalService checkinGoalService;

    /**
     * 获取用户的打卡目标列表
     */
    @GetMapping("/list")
    public Result<List<CheckinGoal>> getGoals(HttpSession session) {
        logger.info("获取打卡目标列表");
        User user = (User) session.getAttribute("user");
        if (user == null) {
            logger.warn("获取打卡目标失败: 未登录");
            return Result.error("未登录");
        }
        try {
            List<CheckinGoal> goals = checkinGoalService.getByUserId(user.getId());
            logger.info("获取打卡目标成功: {}个", goals.size());
            return Result.success(goals);
        } catch (Exception e) {
            logger.error("获取打卡目标失败", e);
            return Result.error("获取失败");
        }
    }

    /**
     * 获取用户的活跃目标
     */
    @GetMapping("/active")
    public Result<List<CheckinGoal>> getActiveGoals(HttpSession session) {
        logger.info("获取活跃打卡目标");
        User user = (User) session.getAttribute("user");
        if (user == null) {
            logger.warn("获取活跃目标失败: 未登录");
            return Result.error("未登录");
        }
        try {
            List<CheckinGoal> goals = checkinGoalService.getActiveGoals(user.getId());
            logger.info("获取活跃目标成功: {}个", goals.size());
            return Result.success(goals);
        } catch (Exception e) {
            logger.error("获取活跃目标失败", e);
            return Result.error("获取失败");
        }
    }

    /**
     * 创建打卡目标
     */
    @PostMapping
    public Result<CheckinGoal> create(@RequestBody Map<String, Object> data, HttpSession session) {
        logger.info("创建打卡目标: data={}", data);
        User user = (User) session.getAttribute("user");
        if (user == null) {
            logger.warn("创建打卡目标失败: 未登录");
            return Result.error("未登录");
        }

        String title = (String) data.get("title");
        if (title == null || title.trim().isEmpty()) {
            logger.warn("创建打卡目标失败: 目标标题为空");
            return Result.error("目标标题不能为空");
        }

        try {
            CheckinGoal goal = new CheckinGoal();
            goal.setUserId(user.getId());
            goal.setTitle(title);
            goal.setDescription((String) data.get("description"));
            goal.setGoalType((String) data.getOrDefault("goalType", "daily"));
            goal.setTargetCount(((Number) data.getOrDefault("targetCount", 30)).intValue());
            goal.setColor((String) data.getOrDefault("color", "#667eea"));

            CheckinGoal created = checkinGoalService.create(goal);
            logger.info("创建打卡目标成功: id={}, title={}", created.getId(), created.getTitle());
            return Result.success("创建成功", created);
        } catch (Exception e) {
            logger.error("创建打卡目标异常", e);
            return Result.error("创建失败");
        }
    }

    /**
     * 更新打卡目标
     */
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody Map<String, Object> data, HttpSession session) {
        logger.info("更新打卡目标: id={}, data={}", id, data);
        User user = (User) session.getAttribute("user");
        if (user == null) {
            logger.warn("更新打卡目标失败: 未登录");
            return Result.error("未登录");
        }

        try {
            CheckinGoal goal = checkinGoalService.getById(id);
            if (goal == null) {
                logger.warn("更新打卡目标失败: 目标不存在, id={}", id);
                return Result.error("目标不存在");
            }

            if (!goal.getUserId().equals(user.getId())) {
                logger.warn("更新打卡目标失败: 无权操作, userId={}, goalId={}", user.getId(), id);
                return Result.error("无权操作");
            }

            if (data.containsKey("title")) {
                goal.setTitle((String) data.get("title"));
            }
            if (data.containsKey("description")) {
                goal.setDescription((String) data.get("description"));
            }
            if (data.containsKey("goalType")) {
                goal.setGoalType((String) data.get("goalType"));
            }
            if (data.containsKey("targetCount")) {
                goal.setTargetCount(((Number) data.get("targetCount")).intValue());
            }
            if (data.containsKey("color")) {
                goal.setColor((String) data.get("color"));
            }

            boolean success = checkinGoalService.updateGoal(goal);
            if (success) {
                logger.info("更新打卡目标成功: id={}", id);
            } else {
                logger.warn("更新打卡目标失败: id={}", id);
            }
            return success ? Result.success("更新成功", true) : Result.error("更新失败");
        } catch (Exception e) {
            logger.error("更新打卡目标异常: id={}", id, e);
            return Result.error("更新失败");
        }
    }

    /**
     * 更新目标进度（打卡）
     */
    @PostMapping("/{id}/checkin")
    public Result<Boolean> checkin(@PathVariable Long id, HttpSession session) {
        logger.info("打卡目标进度: id={}", id);
        User user = (User) session.getAttribute("user");
        if (user == null) {
            logger.warn("打卡失败: 未登录");
            return Result.error("未登录");
        }

        try {
            CheckinGoal goal = checkinGoalService.getById(id);
            if (goal == null) {
                logger.warn("打卡失败: 目标不存在, id={}", id);
                return Result.error("目标不存在");
            }

            if (!goal.getUserId().equals(user.getId())) {
                logger.warn("打卡失败: 无权操作, userId={}, goalId={}", user.getId(), id);
                return Result.error("无权操作");
            }

            boolean success = checkinGoalService.updateProgress(id);
            if (success) {
                logger.info("打卡成功: id={}", id);
            } else {
                logger.warn("打卡失败: id={}", id);
            }
            return success ? Result.success("打卡成功", true) : Result.error("打卡失败");
        } catch (Exception e) {
            logger.error("打卡异常: id={}", id, e);
            return Result.error("打卡失败");
        }
    }

    /**
     * 重置目标进度
     */
    @PostMapping("/{id}/reset")
    public Result<Boolean> reset(@PathVariable Long id, HttpSession session) {
        logger.info("重置目标进度: id={}", id);
        User user = (User) session.getAttribute("user");
        if (user == null) {
            logger.warn("重置失败: 未登录");
            return Result.error("未登录");
        }

        try {
            CheckinGoal goal = checkinGoalService.getById(id);
            if (goal == null) {
                logger.warn("重置失败: 目标不存在, id={}", id);
                return Result.error("目标不存在");
            }

            if (!goal.getUserId().equals(user.getId())) {
                logger.warn("重置失败: 无权操作, userId={}, goalId={}", user.getId(), id);
                return Result.error("无权操作");
            }

            boolean success = checkinGoalService.resetProgress(id);
            if (success) {
                logger.info("重置成功: id={}", id);
            } else {
                logger.warn("重置失败: id={}", id);
            }
            return success ? Result.success("重置成功", true) : Result.error("重置失败");
        } catch (Exception e) {
            logger.error("重置异常: id={}", id, e);
            return Result.error("重置失败");
        }
    }

    /**
     * 获取目标进度
     */
    @GetMapping("/{id}/progress")
    public Result<Map<String, Object>> getProgress(@PathVariable Long id, HttpSession session) {
        logger.info("获取目标进度: id={}", id);
        User user = (User) session.getAttribute("user");
        if (user == null) {
            logger.warn("获取进度失败: 未登录");
            return Result.error("未登录");
        }

        try {
            CheckinGoal goal = checkinGoalService.getById(id);
            if (goal == null) {
                logger.warn("获取进度失败: 目标不存在, id={}", id);
                return Result.error("目标不存在");
            }

            double percent = checkinGoalService.getProgressPercent(id);
            Map<String, Object> result = new HashMap<>();
            result.put("goal", goal);
            result.put("progressPercent", percent);
            
            logger.info("获取进度成功: id={}, percent={}", id, percent);
            return Result.success(result);
        } catch (Exception e) {
            logger.error("获取进度异常: id={}", id, e);
            return Result.error("获取失败");
        }
    }

    /**
     * 删除目标
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id, HttpSession session) {
        logger.info("删除打卡目标: id={}", id);
        User user = (User) session.getAttribute("user");
        if (user == null) {
            logger.warn("删除失败: 未登录");
            return Result.error("未登录");
        }

        try {
            CheckinGoal goal = checkinGoalService.getById(id);
            if (goal == null) {
                logger.warn("删除失败: 目标不存在, id={}", id);
                return Result.error("目标不存在");
            }

            if (!goal.getUserId().equals(user.getId())) {
                logger.warn("删除失败: 无权操作, userId={}, goalId={}", user.getId(), id);
                return Result.error("无权操作");
            }

            boolean success = checkinGoalService.deleteGoal(id);
            if (success) {
                logger.info("删除成功: id={}", id);
            } else {
                logger.warn("删除失败: id={}", id);
            }
            return success ? Result.success("删除成功", true) : Result.error("删除失败");
        } catch (Exception e) {
            logger.error("删除异常: id={}", id, e);
            return Result.error("删除失败");
        }
    }
}