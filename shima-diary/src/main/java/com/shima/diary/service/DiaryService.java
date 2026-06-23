package com.shima.diary.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shima.diary.entity.Diary;
import com.shima.diary.mapper.DiaryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 日记服务类
 */
@Slf4j
@Service
public class DiaryService extends ServiceImpl<DiaryMapper, Diary> {

    @Autowired
    private SearchService searchService;

    /**
     * 分页查询日记
     */
    public Page<Diary> pageList(Long userId, int pageNum, int pageSize) {
        LambdaQueryWrapper<Diary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Diary::getUserId, userId)
               .orderByDesc(Diary::getDiaryDate)
               .orderByDesc(Diary::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    /**
     * 根据日期范围查询
     */
    @Cacheable(value = "diary:dateRange", key = "#userId + ':' + #startDate + ':' + #endDate")
    public List<Diary> getByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return baseMapper.selectByDateRange(userId, startDate, endDate);
    }

    /**
     * 根据分类查询日记
     */
    @Cacheable(value = "diary:category", key = "#userId + ':' + #categoryId")
    public List<Diary> getByCategory(Long userId, Long categoryId) {
        LambdaQueryWrapper<Diary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Diary::getUserId, userId)
               .eq(categoryId != null, Diary::getCategoryId, categoryId)
               .orderByDesc(Diary::getDiaryDate);
        return list(wrapper);
    }

    /**
     * 搜索日记
     */
    public List<Diary> search(Long userId, String keyword) {
        LambdaQueryWrapper<Diary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Diary::getUserId, userId)
               .and(w -> w.like(Diary::getTitle, keyword)
                          .or()
                          .like(Diary::getContent, keyword)
                          .or()
                          .like(Diary::getTags, keyword))
               .orderByDesc(Diary::getDiaryDate);
        return list(wrapper);
    }

    /**
     * 根据心情查询
     */
    @Cacheable(value = "diary:mood", key = "#userId + ':' + #mood")
    public List<Diary> getByMood(Long userId, String mood) {
        LambdaQueryWrapper<Diary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Diary::getUserId, userId)
               .eq(Diary::getMood, mood)
               .orderByDesc(Diary::getDiaryDate);
        return list(wrapper);
    }

    /**
     * 根据日期查询
     */
    @Cacheable(value = "diary:date", key = "#userId + ':' + #diaryDate")
    public Diary getByDate(Long userId, LocalDate diaryDate) {
        LambdaQueryWrapper<Diary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Diary::getUserId, userId)
               .eq(Diary::getDiaryDate, diaryDate)
               .last("LIMIT 1");
        return getOne(wrapper);
    }

    /**
     * 统计心情数量
     */
    @Cacheable(value = "diary:stats", key = "'mood:' + #userId + ':' + #mood")
    public int countByMood(Long userId, String mood) {
        return baseMapper.countByMood(userId, mood);
    }

    /**
     * 创建日记
     */
    @CacheEvict(value = {"diary:dateRange", "diary:category", "diary:mood", "diary:date", "diary:stats"}, allEntries = true)
    public boolean createDiary(Diary diary) {
        if (diary.getUserId() == null) {
            diary.setUserId(1L); // 默认用户
        }
        boolean result = save(diary);
        if (result) {
            // 异步同步到Elasticsearch
            asyncIndexDiary(diary);
        }
        return result;
    }

    /**
     * 更新日记
     */
    @CacheEvict(value = {"diary:dateRange", "diary:category", "diary:mood", "diary:date", "diary:stats"}, allEntries = true)
    public boolean updateDiary(Diary diary) {
        boolean result = updateById(diary);
        if (result) {
            // 异步同步到Elasticsearch
            asyncIndexDiary(diary);
        }
        return result;
    }

    /**
     * 删除日记
     */
    @CacheEvict(value = {"diary:dateRange", "diary:category", "diary:mood", "diary:date", "diary:stats"}, allEntries = true)
    public boolean deleteDiary(Long id) {
        boolean result = removeById(id);
        if (result) {
            // 删除Elasticsearch索引
            asyncDeleteDiaryIndex(id);
        }
        return result;
    }

    /**
     * 异步索引日记到Elasticsearch
     */
    @Async
    public void asyncIndexDiary(Diary diary) {
        try {
            searchService.indexDiary(diary);
            log.info("日记已同步到Elasticsearch: id={}", diary.getId());
        } catch (Exception e) {
            log.error("日记同步到Elasticsearch失败: id={}, error={}", diary.getId(), e.getMessage());
        }
    }

    /**
     * 异步删除Elasticsearch索引
     */
    @Async
    public void asyncDeleteDiaryIndex(Long diaryId) {
        try {
            searchService.deleteDiaryIndex(diaryId);
            log.info("日记索引已删除: id={}", diaryId);
        } catch (Exception e) {
            log.error("删除日记索引失败: id={}, error={}", diaryId, e.getMessage());
        }
    }

    /**
     * 重建用户所有日记的索引
     */
    public void rebuildUserIndex(Long userId) {
        LambdaQueryWrapper<Diary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Diary::getUserId, userId);
        List<Diary> diaries = list(wrapper);
        searchService.indexDiaries(diaries);
        log.info("用户日记索引重建完成: userId={}, count={}", userId, diaries.size());
    }
}
