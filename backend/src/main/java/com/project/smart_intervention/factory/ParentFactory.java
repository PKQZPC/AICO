package com.project.smart_intervention.factory;

import com.project.smart_intervention.entity.dto.LoginDTO;
import com.project.smart_intervention.entity.dto.ParentDTO;
import com.project.smart_intervention.entity.pojo.Parent;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: ParentFactory
 * @Description:
 * @Date: 2025/4/11
 * @Version: 1.0
 */
public class ParentFactory {
    public static LoginDTO toLoginDTO(Parent parent) {
        return new LoginDTO(parent.getId(), parent.getName());
    }

    public static ParentDTO toDTO(Parent parent) {
        return new ParentDTO(parent.getId(), parent.getName());
    }

    public static List<ParentDTO> toDTOs(List<Parent> parents) {
        return parents.stream()
                .map(ParentFactory::toDTO)
                .toList();
    }
}
