package com.project.smart_intervention.factory;

import com.project.smart_intervention.entity.dto.ExpertDTO;
import com.project.smart_intervention.entity.pojo.Expert;

import java.util.List;

/**
 * @ClassName: ExpertFactory
 * @Description:
 * @Date: 2025/4/14
 * @Version: 1.0
 */
public class ExpertFactory {
    public static ExpertDTO toDTO(Expert expert) {
        return new ExpertDTO(expert.getId(), expert.getName());
    }

    public static List<ExpertDTO> toDTOs(List<Expert> experts) {
        return experts.stream().map(ExpertFactory::toDTO).toList();
    }
}
