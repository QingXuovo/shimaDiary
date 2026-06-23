package com.shima.diary.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 日记分享实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("diary_share")
public class DiaryShare {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("diary_id")
    private Long diaryId;

    @TableField("user_id")
    private Long userId;

    @TableField("share_token")
    private String shareToken;

    @TableField("expire_time")
    private LocalDateTime expireTime;

    @TableField("view_count")
    private Integer viewCount;

    @TableField("is_active")
    private Integer isActive;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}