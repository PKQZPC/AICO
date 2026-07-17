package com.project.smart_intervention.entity.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.project.smart_intervention.entity.dto.MessageDTO;
import com.project.smart_intervention.entity.request.SendMessageRequest;
import com.project.smart_intervention.entity.response.AIResponse;
import com.project.smart_intervention.message.MessageConstant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * @ClassName: Message
 * @Description:
 * @Date: 2025/4/7
 * @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("message_info")
public class Message implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer chatId;
    private Long createTimestamp;
    private Integer messageType;
    private String messageContent;
    private Integer messageCategory;
    private String senderIdentity;
    private String receiverIdentity;
    private Long senderId;
    private Long receiverId;
    private Float machineScore;
    private String messageStatus;
}
