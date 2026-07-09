package com.project.smart_intervention.entity.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.project.smart_intervention.entity.pojo.SimpleMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @ClassName: GetModelRequest
 * @Description:
 * @Date: 2025/4/20
 * @Version: 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetModelRequest {
    @JsonProperty("parent_name")
    private String parentName;
    @JsonProperty("messages")
    private List<SimpleMessage> messages;
    @JsonProperty("current_chats_reply_basis")
    private List<ParentModelAlgorithmRequest> currentChatsReplyBasis;

}
