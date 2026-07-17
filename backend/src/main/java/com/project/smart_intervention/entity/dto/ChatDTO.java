package com.project.smart_intervention.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @ClassName: ChatDTO
 * @Description:
 * @Date: 2025/4/7
 * @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatDTO {
    private Integer id;                // 浼氳瘽ID
    private String title;           // 浼氳瘽鏍囬
    private Long parentId;          // 瀹堕暱ID
    private Long expertId;          // 涓撳ID
    private String name;          // 涓撳ID
    private LocalDateTime createdAt; // 浼氳瘽鍒涘缓鏃堕棿
    private String lastMessage; // 鏈€鍚庝竴鏉′細璇?
    private LocalDateTime lastMessageTimestamp; // 鏈€鍚庝竴鏉℃秷鎭椂闂?
    private Integer status;         // 瀵硅瘽鐘舵€侊紙0-AI鎵樼锛?-涓撳寰呬粙鍏ワ紝2-涓撳宸蹭粙鍏ワ級
}
