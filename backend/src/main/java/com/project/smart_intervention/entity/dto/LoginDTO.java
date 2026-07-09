package com.project.smart_intervention.entity.dto;

import com.project.smart_intervention.entity.pojo.Parent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName: LoginDTO
 * @Description:
 * @Date: 2025/4/7
 * @Version: 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginDTO {
    private Long userId;
    private String name;
}
