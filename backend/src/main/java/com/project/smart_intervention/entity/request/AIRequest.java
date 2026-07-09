package com.project.smart_intervention.entity.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.project.smart_intervention.entity.pojo.*;
import lombok.*;

import java.util.Collections;
import java.util.List;




/**
 * @ClassName: AIRequest
 * @Description:
 * @Date: 2025/4/8
 * @Version: 1.0
 */
@Data
@Builder
@AllArgsConstructor
// 涓绘秷鎭被
public class AIRequest {
    @JsonProperty("current_content")
    private String currentContent;
    private List<SimpleMessage> messages;
    @JsonProperty("parent_info")
    private ParentInfo parentInfo;
    @JsonProperty("children_info")
    private List<ChildInfo> childrenInfo;
    @JsonProperty("family_info")
    private FamilyInfo familyInfo;
    @JsonProperty("all_logic_keys")
    private List<LogicKey> allLogicKeys;
    @JsonProperty("expert_type")
    private Integer expertType;
    @JsonProperty("knowledge_base_uuid")
    private String knowledgeBaseUuid;
    @JsonProperty("chat_knowledge_base_id")
    private String chatKnowledgeBaseId;
    @JsonProperty("instruction")
    private InstructionRequest instruction;

    public AIRequest() {
        this.messages = Collections.emptyList();
        this.parentInfo = new ParentInfo();
        this.childrenInfo = Collections.emptyList();
        this.familyInfo = new FamilyInfo();
        this.allLogicKeys = Collections.emptyList();
    }
}
