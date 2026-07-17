package com.project.smart_intervention.entity.dto;

import com.project.smart_intervention.entity.pojo.ExpertsAIMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @ClassName: ExpertsMessagesDTO
 * @Description:
 * @Date: 2025/4/14
 * @Version: 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpertsMessagesDTO {
    private List<MessageDTO> commonMessages;
    private List<ExpertsAIMessageDTO> expertsAIMessages;
}
