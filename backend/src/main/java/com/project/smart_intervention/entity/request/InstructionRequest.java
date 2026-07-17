package com.project.smart_intervention.entity.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.project.smart_intervention.entity.pojo.HistoryInstruction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @ClassName: InstructionRequest
 * @Description:
 * @Date: 2025/4/14
 * @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstructionRequest {
    @JsonProperty("current_instruction")
    private String currentInstruction;
    @JsonProperty("type")
    private String type;
    @JsonProperty("history_instructions")
    private List<HistoryInstruction> historyInstructions;
}
