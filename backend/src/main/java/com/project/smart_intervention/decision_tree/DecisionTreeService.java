package com.project.smart_intervention.decision_tree;

import com.project.smart_intervention.config.RabbitConfig;
import com.project.smart_intervention.entity.request.UpdateTreeRequest;
import com.project.smart_intervention.util.HttpClientUtils;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @ClassName: DecisionTreeService
 * @Description:
 * @Date: 2025/4/14
 * @Version: 1.0
 */
@Service
public class DecisionTreeService {

    @Resource
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private HttpClientUtils httpClientUtils;

    @SneakyThrows
    public String listDecisionTree() {
        return httpClientUtils.getDecisionTree();
    }

    public void updateDecisionTree(String treeId, String tree) {
        UpdateTreeRequest request = new UpdateTreeRequest(treeId, tree);
        rabbitTemplate.convertAndSend(RabbitConfig.UPDATE_DECISION_TREE_QUEUE, tree);
    }
}
