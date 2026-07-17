package com.project.smart_intervention.entity.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @ClassName: LogicKey
 * @Description:
 * @Date: 2025/4/8
 * @Version: 1.0
 */
// 閫昏緫閿被
@Data
public class LogicKey {
    @JsonProperty("logic_key_id")
    public int logicKeyId;
    @JsonProperty("logic_key")
    public String logicKey;
}
