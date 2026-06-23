package com.shima.diary.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 用户操作记录实体类
 * 用于缓存和记录用户的操作行为
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserOperation {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 操作类型
     */
    private String operationType;

    /**
     * 操作描述
     */
    private String description;

    /**
     * 操作数据
     */
    private Map<String, Object> data;

    /**
     * 操作时间
     */
    private LocalDateTime operationTime;

    /**
     * 客户端IP
     */
    private String clientIp;

    /**
     * 设备信息
     */
    private String deviceInfo;
}
