package com.project.smart_intervention.entity.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName: Parent
 * @Description:
 * @Date: 2025/4/7
 * @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("parent")
public class Parent {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String name;
    private String username;
}
