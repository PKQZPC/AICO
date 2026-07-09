package com.project.smart_intervention.entity.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.project.smart_intervention.entity.dto.MessageDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.ref.PhantomReference;
import java.util.List;

/**
 * @ClassName: AIResponse
 * @Description:
 * @Date: 2025/4/8
 * @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIResponse {
    @JsonProperty("all_reply")
    private String allReply; // AI鍥炲缁撴灉
    @JsonProperty("score")
    private Double score; // 鏈哄櫒鑷姩璇勫垎锛岀敤浜庡垽鏂槸鍚﹂渶瑕佷汉宸ヤ粙鍏?    @JsonProperty("knowledge_uuids")
    private List<String> knowledgeUuids; // 鐭ヨ瘑搴搃d
    @JsonProperty("chat_title")
    private String chatTitle; // 浼氳瘽鏍囬
}
