package com.project.smart_intervention.entity.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName: AIRecommendMessage
 * @Description:
 * @Date: 2025/4/14
 * @Version: 1.0
 */
@TableName("ai_recommend_message")
@Data
public class AIRecommendMessage {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Integer chatId;
    private String messageContent;
    private Long createTimestamp;
}
