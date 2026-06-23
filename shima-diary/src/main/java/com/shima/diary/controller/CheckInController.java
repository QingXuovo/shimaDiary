package com.shima.diary.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shima.diary.common.Result;
import com.shima.diary.entity.CheckIn;
import com.shima.diary.entity.CheckInConfig;
import com.shima.diary.entity.User;
import com.shima.diary.service.CheckInService;
import com.shima.diary.service.CheckInConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 打卡控制器
 */
@RestController
@RequestMapping("/checkin")
public class CheckInController {

    private static final Logger logger = LoggerFactory.getLogger(CheckInController.class);

    @Autowired
    private CheckInService checkInService;

    @Autowired
    private CheckInConfigService checkInConfigService;

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
     * 获取所有打卡记录
     */
    @GetMapping
    public Result<List<CheckIn>> getAll(HttpSession session) {
        logger.info("获取所有打卡记录请求");
        Long userId = getUserId(session);
        boolean admin = isAdmin(session);
        try {
            List<CheckIn> checkins;
            if (admin) {
                // 管理员可以查看所有用户的打卡记录
                checkins = checkInService.list(new LambdaQueryWrapper<CheckIn>()
                    .orderByDesc(CheckIn::getCheckDate)
                    .orderByDesc(CheckIn::getCreateTime));
            } else {
                // 普通用户只能查看自己的打卡记录
                checkins = checkInService.list(new LambdaQueryWrapper<CheckIn>()
                    .eq(CheckIn::getUserId, userId)
                    .orderByDesc(CheckIn::getCheckDate)
                    .orderByDesc(CheckIn::getCreateTime));
            }
            logger.info("获取打卡记录成功，共{}条", checkins.size());
            return Result.success(checkins);
        } catch (Exception e) {
            logger.error("获取打卡记录失败", e);
            return Result.error("获取打卡记录失败");
        }
    }

    /**
     * 打卡
     */
    @PostMapping
    public Result<Boolean> checkIn(@RequestBody CheckIn checkIn, HttpSession session) {
        logger.info("打卡请求: userId={}, checkDate={}, checkType={}, checkName={}", 
            checkIn.getUserId(), checkIn.getCheckDate(), checkIn.getCheckType(), checkIn.getCheckName());
        Long userId = getUserId(session);
        checkIn.setUserId(userId);
        if (checkIn.getCheckDate() == null) {
            checkIn.setCheckDate(LocalDate.now());
        }
        try {
            boolean success = checkInService.checkIn(checkIn);
            if (success) {
                logger.info("打卡成功: userId={}, date={}", userId, checkIn.getCheckDate());
                return Result.success("打卡成功", true);
            } else {
                logger.warn("打卡失败，今日已打卡: userId={}, date={}", userId, checkIn.getCheckDate());
                return Result.error("今日已打卡");
            }
        } catch (Exception e) {
            logger.error("打卡异常: userId={}", userId, e);
            return Result.error("打卡失败");
        }
    }

    /**
     * 查询某天的打卡记录
     */
    @GetMapping("/date/{date}")
    public Result<List<CheckIn>> getByDate(@PathVariable LocalDate date, HttpSession session) {
        logger.info("查询指定日期打卡记录: date={}", date);
        Long userId = getUserId(session);
        try {
            List<CheckIn> list = checkInService.getByDate(userId, date);
            logger.info("查询成功: date={}, 记录数={}", date, list.size());
            return Result.success(list);
        } catch (Exception e) {
            logger.error("查询打卡记录失败: date={}", date, e);
            return Result.error("查询失败");
        }
    }

    /**
     * 查询今日打卡
     */
    @GetMapping("/today")
    public Result<List<CheckIn>> getToday(HttpSession session) {
        logger.info("查询今日打卡记录");
        Long userId = getUserId(session);
        try {
            List<CheckIn> list = checkInService.getByDate(userId, LocalDate.now());
            logger.info("今日打卡记录: {}条", list.size());
            return Result.success(list);
        } catch (Exception e) {
            logger.error("查询今日打卡失败", e);
            return Result.error("查询失败");
        }
    }

    /**
     * 查询某类型的打卡记录
     */
    @GetMapping("/type/{type}")
    public Result<List<CheckIn>> getByType(@PathVariable String type, HttpSession session) {
        logger.info("查询打卡类型记录: type={}", type);
        Long userId = getUserId(session);
        try {
            List<CheckIn> list = checkInService.getByType(userId, type);
            logger.info("查询成功: type={}, 记录数={}", type, list.size());
            return Result.success(list);
        } catch (Exception e) {
            logger.error("查询打卡类型失败: type={}", type, e);
            return Result.error("查询失败");
        }
    }

    /**
     * 获取连续打卡天数
     */
    @GetMapping("/continuous/{type}")
    public Result<Integer> getContinuousDays(@PathVariable String type, HttpSession session) {
        logger.info("获取连续打卡天数: type={}", type);
        Long userId = getUserId(session);
        try {
            int days = checkInService.getContinuousDays(userId, type);
            logger.info("连续打卡天数: {}天", days);
            return Result.success(days);
        } catch (Exception e) {
            logger.error("获取连续打卡天数失败: type={}", type, e);
            return Result.error("获取失败");
        }
    }

    /**
     * 获取打卡统计
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats(HttpSession session) {
        logger.info("获取打卡统计");
        Long userId = getUserId(session);
        try {
            Map<String, Object> stats = checkInService.getCheckInStats(userId);
            logger.info("打卡统计获取成功");
            return Result.success(stats);
        } catch (Exception e) {
            logger.error("获取打卡统计失败", e);
            return Result.error("获取失败");
        }
    }

    /**
     * 获取打卡天数
     */
    @GetMapping("/count")
    public Result<Integer> count(HttpSession session) {
        logger.info("获取打卡天数统计");
        Long userId = getUserId(session);
        try {
            int count = (int) checkInService.count(new LambdaQueryWrapper<CheckIn>().eq(CheckIn::getUserId, userId));
            logger.info("打卡天数: {}天", count);
            return Result.success(count);
        } catch (Exception e) {
            logger.error("获取打卡天数失败", e);
            return Result.error("获取失败");
        }
    }

    /**
     * 查询最近N天的打卡记录
     */
    @GetMapping("/recent")
    public Result<List<CheckIn>> getRecentDays(@RequestParam(defaultValue = "7") int days,
                                                HttpSession session) {
        logger.info("查询最近{}天打卡记录", days);
        Long userId = getUserId(session);
        try {
            List<CheckIn> list = checkInService.getRecentDays(userId, days);
            logger.info("查询成功: {}天内{}条记录", days, list.size());
            return Result.success(list);
        } catch (Exception e) {
            logger.error("查询最近打卡记录失败: days={}", days, e);
            return Result.error("查询失败");
        }
    }

    /**
     * 取消打卡
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> cancel(@PathVariable Long id, HttpSession session) {
        logger.info("取消打卡请求: id={}", id);
        Long userId = getUserId(session);
        boolean admin = isAdmin(session);
        
        try {
            // 检查权限：管理员可以取消任何用户的打卡，普通用户只能取消自己的打卡
            CheckIn checkIn = checkInService.getById(id);
            if (checkIn == null) {
                return Result.error("打卡记录不存在");
            }
            
            if (!admin && !checkIn.getUserId().equals(userId)) {
                logger.warn("取消打卡失败: 无权操作他人打卡记录, userId={}, checkInId={}", userId, id);
                return Result.error("无权操作");
            }
            
            boolean success = checkInService.cancelCheckIn(id);
            if (success) {
                logger.info("取消打卡成功: id={}", id);
                return Result.success("取消成功", true);
            } else {
                logger.warn("取消打卡失败: id={}", id);
                return Result.error("取消失败");
            }
        } catch (Exception e) {
            logger.error("取消打卡异常: id={}", id, e);
            return Result.error("取消失败");
        }
    }

    // ==================== 打卡配置相关 ====================

    /**
     * 获取所有打卡配置
     */
    @GetMapping("/config")
    public Result<List<CheckInConfig>> getAllConfigs(HttpSession session) {
        logger.info("获取打卡配置");
        Long userId = getUserId(session);
        try {
            List<CheckInConfig> configs = checkInConfigService.getAllConfigs(userId);
            logger.info("获取打卡配置成功: {}条", configs.size());
            return Result.success(configs);
        } catch (Exception e) {
            logger.error("获取打卡配置失败", e);
            return Result.error("获取失败");
        }
    }

    /**
     * 添加打卡配置
     */
    @PostMapping("/config")
    public Result<Boolean> addConfig(@RequestBody CheckInConfig config, HttpSession session) {
        logger.info("添加打卡配置: checkName={}", config.getCheckName());
        Long userId = getUserId(session);
        config.setUserId(userId);
        try {
            boolean success = checkInConfigService.addConfig(config);
            if (success) {
                logger.info("添加打卡配置成功: id={}", config.getId());
                return Result.success("添加成功", true);
            } else {
                logger.warn("添加打卡配置失败");
                return Result.error("添加失败");
            }
        } catch (Exception e) {
            logger.error("添加打卡配置异常", e);
            return Result.error("添加失败");
        }
    }

    /**
     * 更新打卡配置
     */
    @PutMapping("/config")
    public Result<Boolean> updateConfig(@RequestBody CheckInConfig config) {
        logger.info("更新打卡配置: id={}, checkName={}", config.getId(), config.getCheckName());
        try {
            boolean success = checkInConfigService.updateConfig(config);
            if (success) {
                logger.info("更新打卡配置成功: id={}", config.getId());
                return Result.success("更新成功", true);
            } else {
                logger.warn("更新打卡配置失败: id={}", config.getId());
                return Result.error("更新失败");
            }
        } catch (Exception e) {
            logger.error("更新打卡配置异常: id={}", config.getId(), e);
            return Result.error("更新失败");
        }
    }

    /**
     * 删除打卡配置
     */
    @DeleteMapping("/config/{id}")
    public Result<Boolean> deleteConfig(@PathVariable Long id) {
        logger.info("删除打卡配置: id={}", id);
        try {
            boolean success = checkInConfigService.deleteConfig(id);
            if (success) {
                logger.info("删除打卡配置成功: id={}", id);
                return Result.success("删除成功", true);
            } else {
                logger.warn("删除打卡配置失败: id={}", id);
                return Result.error("删除失败");
            }
        } catch (Exception e) {
            logger.error("删除打卡配置异常: id={}", id, e);
            return Result.error("删除失败");
        }
    }
}