package com.project.smart_intervention.decision_tree;

import com.project.smart_intervention.entity.constant.SuccessConstant;
import com.project.smart_intervention.entity.pojo.Result;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName: DecisionTreeController
 * @Description:
 * @Date: 2025/4/14
 * @Version: 1.0
 */
@RestController
@RequestMapping("/decision_trees")
@Slf4j
public class DecisionTreeController {

    @Resource
    private DecisionTreeService decisionTreeService;

    /**
     * 鑾峰彇鍐崇瓥鏍?     * @return
     */
    @GetMapping
    public String listDecisionTree() {
        return decisionTreeService.listDecisionTree();
    }

    @PutMapping
    public Result<String> updateDecisionTree(@RequestBody String jsonString) {
        decisionTreeService.updateDecisionTree(null, jsonString);
        return Result.success(SuccessConstant.UPDATE_DECISION_TREE_SUCCESS);
    }
}
