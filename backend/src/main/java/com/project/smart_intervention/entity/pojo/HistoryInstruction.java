package com.project.smart_intervention.entity.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @ClassName: HistoryInstruction
 * @Description:
 * @Date: 2025/4/22
 * @Version: 1.0
 */
@Data
@AllArgsConstructor
public class HistoryInstruction {
    private String type;
    private String instruction;
}
