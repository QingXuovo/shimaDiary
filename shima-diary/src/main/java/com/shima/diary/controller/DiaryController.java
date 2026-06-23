package com.shima.diary.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shima.diary.common.Result;
import com.shima.diary.entity.Diary;
import com.shima.diary.entity.User;
import com.shima.diary.service.DiaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.List;

/**
 * 日记控制器
 */
@RestController
@RequestMapping("/diary")
public class DiaryController {

    private static final Logger logger = LoggerFactory.getLogger(DiaryController.class);

    @Autowired
    private DiaryService diaryService;

    /**
     * 获取当前登录用户ID（支持Session和Header两种方式）
     */
    private Long getUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            User user = (User) session.getAttribute("user");
            if (user != null) {
                logger.debug("从Session获取用户ID: {}", user.getId());
                return user.getId();
            }
        }
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader != null && !userIdHeader.isEmpty()) {
            try {
                Long userId = Long.parseLong(userIdHeader);
                logger.debug("从Header获取用户ID: {}", userId);
                return userId;
            } catch (NumberFormatException e) {
                logger.warn("无效的用户ID: {}", userIdHeader);
            }
        }
        logger.debug("使用默认用户ID: 1");
        return 1L;
    }

    /**
     * 检查是否是管理员
     */
    private boolean isAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            User user = (User) session.getAttribute("user");
            if (user != null && "admin".equals(user.getRole())) {
                logger.debug("检查管理员权限: true");
                return true;
            }
        }
        logger.debug("检查管理员权限: false");
        return false;
    }

    /**
     * 获取所有日记列表（不分页）
     */
    @GetMapping
    public Result<List<Diary>> getAll(HttpServletRequest request) {
        logger.info("获取所有日记列表请求");
        Long userId = getUserId(request);
        boolean admin = isAdmin(request);
        try {
            List<Diary> diaries;
            if (admin) {
                diaries = diaryService.list(new LambdaQueryWrapper<Diary>()
                    .eq(Diary::getArchived, 0)
                    .orderByDesc(Diary::getDiaryDate)
                    .orderByDesc(Diary::getCreateTime));
            } else {
                diaries = diaryService.list(new LambdaQueryWrapper<Diary>()
                    .eq(Diary::getUserId, userId)
                    .eq(Diary::getArchived, 0)
                    .orderByDesc(Diary::getDiaryDate)
                    .orderByDesc(Diary::getCreateTime));
            }
            logger.info("获取日记成功，共{}条", diaries.size());
            return Result.success(diaries);
        } catch (Exception e) {
            logger.error("获取日记列表失败", e);
            return Result.error("获取失败");
        }
    }

    /**
     * 分页查询日记列表
     */
    @GetMapping("/list")
    public Result<Page<Diary>> list(@RequestParam(defaultValue = "1") int pageNum,
                                     @RequestParam(defaultValue = "10") int pageSize,
                                     HttpServletRequest request) {
        logger.info("分页查询日记: pageNum={}, pageSize={}", pageNum, pageSize);
        Long userId = getUserId(request);
        try {
            Page<Diary> page = diaryService.pageList(userId, pageNum, pageSize);
            logger.info("分页查询成功: 总数={}", page.getTotal());
            return Result.success(page);
        } catch (Exception e) {
            logger.error("分页查询日记失败: pageNum={}, pageSize={}", pageNum, pageSize, e);
            return Result.error("查询失败");
        }
    }

    /**
     * 根据ID获取日记详情
     */
    @GetMapping("/{id}")
    public Result<Diary> getById(@PathVariable Long id) {
        logger.info("获取日记详情: id={}", id);
        try {
            Diary diary = diaryService.getById(id);
            if (diary != null) {
                logger.info("获取日记成功: id={}, title={}", id, diary.getTitle());
            } else {
                logger.warn("日记不存在: id={}", id);
            }
            return Result.success(diary);
        } catch (Exception e) {
            logger.error("获取日记详情失败: id={}", id, e);
            return Result.error("获取失败");
        }
    }

    /**
     * 根据日期范围查询
     */
    @GetMapping("/range")
    public Result<List<Diary>> getByDateRange(@RequestParam LocalDate startDate,
                                               @RequestParam LocalDate endDate,
                                               HttpServletRequest request) {
        logger.info("日期范围查询日记: startDate={}, endDate={}", startDate, endDate);
        Long userId = getUserId(request);
        try {
            List<Diary> list = diaryService.getByDateRange(userId, startDate, endDate);
            logger.info("日期范围查询成功: {}条", list.size());
            return Result.success(list);
        } catch (Exception e) {
            logger.error("日期范围查询失败: startDate={}, endDate={}", startDate, endDate, e);
            return Result.error("查询失败");
        }
    }

    /**
     * 根据分类查询日记
     */
    @GetMapping("/category/{categoryId}")
    public Result<List<Diary>> getByCategory(@PathVariable Long categoryId,
                                              HttpServletRequest request) {
        logger.info("分类查询日记: categoryId={}", categoryId);
        Long userId = getUserId(request);
        try {
            List<Diary> list = diaryService.getByCategory(userId, categoryId);
            logger.info("分类查询成功: {}条", list.size());
            return Result.success(list);
        } catch (Exception e) {
            logger.error("分类查询失败: categoryId={}", categoryId, e);
            return Result.error("查询失败");
        }
    }

    /**
     * 搜索日记
     */
    @GetMapping("/search")
    public Result<List<Diary>> search(@RequestParam String keyword,
                                       HttpServletRequest request) {
        logger.info("搜索日记: keyword={}", keyword);
        Long userId = getUserId(request);
        try {
            List<Diary> list = diaryService.search(userId, keyword);
            logger.info("搜索成功: {}条", list.size());
            return Result.success(list);
        } catch (Exception e) {
            logger.error("搜索日记失败: keyword={}", keyword, e);
            return Result.error("搜索失败");
        }
    }

    /**
     * 根据心情查询
     */
    @GetMapping("/mood/{mood}")
    public Result<List<Diary>> getByMood(@PathVariable String mood,
                                          HttpServletRequest request) {
        logger.info("心情查询日记: mood={}", mood);
        Long userId = getUserId(request);
        try {
            List<Diary> list = diaryService.getByMood(userId, mood);
            logger.info("心情查询成功: {}条", list.size());
            return Result.success(list);
        } catch (Exception e) {
            logger.error("心情查询失败: mood={}", mood, e);
            return Result.error("查询失败");
        }
    }

    /**
     * 创建日记
     */
    @PostMapping
    public Result<Diary> create(@RequestBody Diary diary, HttpServletRequest request) {
        logger.info("创建日记请求: title={}, diaryDate={}, mood={}", 
            diary.getTitle(), diary.getDiaryDate(), diary.getMood());
        Long userId = getUserId(request);
        diary.setUserId(userId);
        try {
            boolean success = diaryService.createDiary(diary);
            if (success) {
                logger.info("创建日记成功: id={}, title={}", diary.getId(), diary.getTitle());
                return Result.success("创建成功", diary);
            } else {
                logger.warn("创建日记失败");
                return Result.error("创建失败");
            }
        } catch (Exception e) {
            logger.error("创建日记异常", e);
            return Result.error("创建失败");
        }
    }

    /**
     * 更新日记
     */
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody Diary diary, HttpServletRequest request) {
        logger.info("更新日记: id={}, title={}", id, diary.getTitle());
        diary.setId(id);
        
        Long userId = getUserId(request);
        boolean admin = isAdmin(request);
        
        try {
            Diary existing = diaryService.getById(id);
            if (existing == null) {
                return Result.error("日记不存在");
            }
            
            if (!admin && !existing.getUserId().equals(userId)) {
                logger.warn("更新日记失败: 无权操作他人日记, userId={}, diaryId={}", userId, id);
                return Result.error("无权操作");
            }
            
            boolean success = diaryService.updateDiary(diary);
            if (success) {
                logger.info("更新日记成功: id={}", id);
                return Result.success("更新成功", true);
            } else {
                logger.warn("更新日记失败: id={}", id);
                return Result.error("更新失败");
            }
        } catch (Exception e) {
            logger.error("更新日记异常: id={}", id, e);
            return Result.error("更新失败");
        }
    }

    /**
     * 删除日记
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id, HttpServletRequest request) {
        logger.info("删除日记请求: id={}", id);
        
        Long userId = getUserId(request);
        boolean admin = isAdmin(request);
        
        try {
            Diary existing = diaryService.getById(id);
            if (existing == null) {
                return Result.error("日记不存在");
            }
            
            if (!admin && !existing.getUserId().equals(userId)) {
                logger.warn("删除日记失败: 无权操作他人日记, userId={}, diaryId={}", userId, id);
                return Result.error("无权操作");
            }
            
            boolean success = diaryService.deleteDiary(id);
            if (success) {
                logger.info("删除日记成功: id={}", id);
                return Result.success("删除成功", true);
            } else {
                logger.warn("删除日记失败: id={}", id);
                return Result.error("删除失败");
            }
        } catch (Exception e) {
            logger.error("删除日记异常: id={}", id, e);
            return Result.error("删除失败");
        }
    }

    /**
     * 统计心情数量
     */
    @GetMapping("/mood/count/{mood}")
    public Result<Integer> countByMood(@PathVariable String mood,
                                        HttpServletRequest request) {
        logger.info("统计心情数量: mood={}", mood);
        Long userId = getUserId(request);
        try {
            int count = diaryService.countByMood(userId, mood);
            logger.info("心情统计成功: mood={}, count={}", mood, count);
            return Result.success(count);
        } catch (Exception e) {
            logger.error("心情统计失败: mood={}", mood, e);
            return Result.error("统计失败");
        }
    }

    /**
     * 获取日记数量
     */
    @GetMapping("/count")
    public Result<Integer> count(HttpServletRequest request) {
        logger.info("获取日记数量统计");
        Long userId = getUserId(request);
        try {
            int count = (int) diaryService.count(new LambdaQueryWrapper<Diary>().eq(Diary::getUserId, userId));
            logger.info("日记数量: {}", count);
            return Result.success(count);
        } catch (Exception e) {
            logger.error("获取日记数量失败", e);
            return Result.error("获取失败");
        }
    }
}