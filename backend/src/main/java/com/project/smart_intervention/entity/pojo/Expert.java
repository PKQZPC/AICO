package com.project.smart_intervention.entity.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName: Expert
 * @Description:
 * @Date: 2025/4/7
 * @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Expert {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private String name;
    private String username;
}
