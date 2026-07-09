package com.project.smart_intervention.entity.pojo;

import cn.hutool.db.meta.TableType;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName: ExpertsAIMessage
 * @Description:
 * @Date: 2025/4/14
 * @Version: 1.0
 */
@TableName("experts_ai_message_info")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpertsAIMessage {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Integer chatId;
    private Long createTimestamp;
    private Integer messageType;
    private String type;
    private String messageContent;
    private Integer messageCategory;
    private String senderIdentity;
    private Integer senderId;
}
