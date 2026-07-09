package com.project.smart_intervention.parent_model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @ClassName: ParentModel
 * @Description:
 * @Date: 2025/4/21
 * @Version: 1.0
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("parent_model")
public class ParentModel {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Integer chatId;
    private String profile;
    private String replyStrategy;
    private String eventSummary;
    private String tag;
    private LocalDateTime latestMessageTime;
}
