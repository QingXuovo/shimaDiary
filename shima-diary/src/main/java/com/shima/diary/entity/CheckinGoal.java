package com.shima.diary.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 打卡目标实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("checkin_goal")
public class CheckinGoal {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("title")
    private String title;

    @TableField("description")
    private String description;

    @TableField("goal_type")
    private String goalType; // daily, weekly, monthly

    @TableField("target_count")
    private Integer targetCount;

    @TableField("current_count")
    private Integer currentCount;

    @TableField("start_date")
    private LocalDateTime startDate;

    @TableField("end_date")
    private LocalDateTime endDate;

    @TableField("is_active")
    private Integer isActive;

    @TableField("color")
    private String color;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField("deleted")
    @TableLogic
    private Integer deleted;
}