package com.project.smart_intervention.entity.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.project.smart_intervention.chat.ChatConstant;
import lombok.Data;

/**
 * @ClassName: Chat
 * @Description:
 * @Date: 2025/4/7
 * @Version: 1.0
 */
@Data
@TableName("chat_info")
public class Chat {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String senderIdentity;
    private Long expertId;
    private Long parentId;
    private String chatKnowledgeBaseId;
    private Long createTimestamp;
    private String chatKeyword;
    private String chatTitle;
    private Integer chatState;
    private Long lastMessageTimestamp;
    private Long lastReadTimestampParent;
    private Long lastReadTimestampExpert;
    private Integer rounds;
    private Integer favoriteState;
}
