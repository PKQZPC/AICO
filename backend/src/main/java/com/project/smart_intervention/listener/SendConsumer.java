package com.project.smart_intervention.listener;

import com.project.smart_intervention.chat.ChatConstant;
import com.project.smart_intervention.chat.IChatService;
import com.project.smart_intervention.config.RabbitConfig;
import com.project.smart_intervention.entity.dto.MessageDTO;
import com.project.smart_intervention.entity.pojo.Chat;
import com.project.smart_intervention.entity.pojo.ExpertsAIMessage;
import com.project.smart_intervention.entity.pojo.Message;
import com.project.smart_intervention.entity.pojo.SimpleMessage;
import com.project.smart_intervention.entity.request.AIRequest;
import com.project.smart_intervention.entity.request.UpdateTreeRequest;
import com.project.smart_intervention.entity.response.AIResponse;
import com.project.smart_intervention.entity.response.Response;
import com.project.smart_intervention.exceptions.MessageException;
import com.project.smart_intervention.message.MessageConstant;
import com.project.smart_intervention.message.MessageService;
import com.project.smart_intervention.util.*;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @ClassName: SendConsumer
 * @Description:
 * @Date: 2025/4/7
 * @Version: 1.0
 */
@Component
@Slf4j
public class SendConsumer {

    @Resource
    private ListenerUtils listenerUtils;
    @Resource
    private WebSocketUtils webSocketUtils;


    @SneakyThrows
    @RabbitListener(queues = RabbitConfig.SEND_MESSAGE_QUEUE, concurrency = "5")
    public void handleMessage(Message message) {
        listenerUtils.sendAIMessage(message);
    }


    @RabbitListener(queues = RabbitConfig.DEAD_SEND_MESSAGE_QUEUE)
    public void handleDeadMessage(Message message) {
        // TODO 杩欓噷鏄淇℃秷鎭殑澶勭悊閫昏緫
        log.info("娑堟伅瓒呮椂澶勭悊: {}", message.getMessageContent());
        webSocketUtils.sendDeadMessage(MessageConstant.WEBSOCKET_URL_PREFIX + message.getChatId());
    }

    @RabbitListener(queues = RabbitConfig.LIST_DECISION_TREE_QUEUE)
    public void handleDecisionTree() {
        // TODO 杩欓噷闇€瑕佽皟鐢ㄦ帴鍙ｈ幏鍙栧喅绛栨爲
        listenerUtils.sendDecisionTrees();
    }

    @RabbitListener(queues = RabbitConfig.UPDATE_DECISION_TREE_QUEUE)
    public void handleUpdateDecisionTree(String tree) {
        // TODO 杩欓噷闇€瑕佽皟鐢ㄦ帴鍙ｄ慨鏀瑰喅绛栨爲
        listenerUtils.updateDecisionTrees(tree);
    }

    @RabbitListener(queues = RabbitConfig.SEND_INSTRUCTION_QUEUE)
    public void handleSendInstruction(ExpertsAIMessage message) {
        // TODO 杩欓噷闇€瑕佽皟鐢ㄦ帴鍙ｇ粰AI鍙戦€佹寚绀?
        listenerUtils.sendInstruction(message);
    }

    @SneakyThrows
    @RabbitListener(queues = RabbitConfig.GET_RECOMMEND_MESSAGE_QUEUE)
    public void handleGetRecommend(Chat chat) {
        listenerUtils.getRecommend(chat);
    }
}
