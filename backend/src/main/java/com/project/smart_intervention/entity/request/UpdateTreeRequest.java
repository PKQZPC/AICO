package com.project.smart_intervention.entity.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName: UpdateTreeRequest
 * @Description:
 * @Date: 2025/4/14
 * @Version: 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTreeRequest {
    @JsonProperty("tree_id")
    private String treeId;
    private String tree;
}
