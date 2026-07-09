package com.project.smart_intervention.entity.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @ClassName: ChildInfo
 * @Description:
 * @Date: 2025/4/8
 * @Version: 1.0
 */
// 瀛╁瓙淇℃伅绫?@Data
public class ChildInfo {
    @JsonProperty("child_name")
    public String childName;
    @JsonProperty("child_sex")
    public Integer childSex; // 0: 鐢凤紝1: 濂?    @JsonProperty("child_age")
    public Integer childAge;
    @JsonProperty("mental_health")
    public String mentalHealth;
    @JsonProperty("education_level")
    public String educationLevel;
    @JsonProperty("academic_record")
    public String academicRecord;
}
