package com.shima.diary.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shima.diary.entity.Diary;
import com.shima.diary.entity.DiaryShare;
import com.shima.diary.entity.User;
import com.shima.diary.mapper.DiaryShareMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 日记分享服务
 */
@Service
public class DiaryShareService extends ServiceImpl<DiaryShareMapper, DiaryShare> {

    @Autowired
    private DiaryService diaryService;

    /**
     * 创建分享链接
     */
    public DiaryShare createShare(Long diaryId, Long userId, int days) {
        // 检查日记是否存在
        Diary diary = diaryService.getById(diaryId);
        if (diary == null || !diary.getUserId().equals(userId)) {
            return null;
        }

        // 检查是否已有分享链接
        DiaryShare existing = getExistingShare(diaryId);
        if (existing != null) {
            // 更新过期时间
            existing.setExpireTime(LocalDateTime.now().plusDays(days));
            existing.setIsActive(1);
            updateById(existing);
            return existing;
        }

        // 创建新的分享记录
        DiaryShare share = new DiaryShare();
        share.setDiaryId(diaryId);
        share.setUserId(userId);
        share.setShareToken(UUID.randomUUID().toString().replace("-", ""));
        share.setExpireTime(LocalDateTime.now().plusDays(days));
        share.setViewCount(0);
        share.setIsActive(1);

        save(share);
        return share;
    }

    /**
     * 获取已有分享记录
     */
    private DiaryShare getExistingShare(Long diaryId) {
        LambdaQueryWrapper<DiaryShare> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DiaryShare::getDiaryId, diaryId)
               .eq(DiaryShare::getIsActive, 1);
        return getOne(wrapper);
    }

    /**
     * 根据token获取分享记录
     */
    public DiaryShare getByToken(String token) {
        DiaryShare share = baseMapper.selectByToken(token);
        if (share != null && share.getIsActive() == 1 && 
            share.getExpireTime().isAfter(LocalDateTime.now())) {
            return share;
        }
        return null;
    }

    /**
     * 增加浏览次数
     */
    public void incrementViewCount(Long id) {
        DiaryShare share = getById(id);
        if (share != null) {
            share.setViewCount(share.getViewCount() + 1);
            updateById(share);
        }
    }

    /**
     * 取消分享
     */
    public boolean cancelShare(Long diaryId, Long userId) {
        LambdaQueryWrapper<DiaryShare> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DiaryShare::getDiaryId, diaryId)
               .eq(DiaryShare::getUserId, userId)
               .eq(DiaryShare::getIsActive, 1);
        
        DiaryShare share = getOne(wrapper);
        if (share != null) {
            share.setIsActive(0);
            return updateById(share);
        }
        return false;
    }

    /**
     * 获取日记通过分享token
     */
    public Diary getDiaryByShareToken(String token) {
        DiaryShare share = getByToken(token);
        if (share != null) {
            incrementViewCount(share.getId());
            return diaryService.getById(share.getDiaryId());
        }
        return null;
    }
}