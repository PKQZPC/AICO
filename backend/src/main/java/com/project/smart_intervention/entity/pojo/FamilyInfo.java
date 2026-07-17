package com.project.smart_intervention.entity.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @ClassName: FamilyInfo
 * @Description:
 * @Date: 2025/4/8
 * @Version: 1.0
 */
// 瀹跺涵淇℃伅绫?@Data
public class FamilyInfo {
    @JsonProperty("family_type")
    public String familyType;
    public String accompany;
    public String communicate;
    public String relationship;

    public FamilyInfo() {
        this.familyType = "";
        this.accompany = "";
        this.communicate = "";
        this.relationship = "";
    }
}
