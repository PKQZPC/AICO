package com.project.smart_intervention.util;

import com.project.smart_intervention.entity.dto.ExpertsAIMessageDTO;
import com.project.smart_intervention.entity.dto.MessageDTO;
import com.project.smart_intervention.entity.pojo.AIRecommendMessage;
import com.project.smart_intervention.entity.pojo.ExpertsAIMessage;
import com.project.smart_intervention.entity.pojo.Message;
import com.project.smart_intervention.entity.pojo.Result;
import com.project.smart_intervention.entity.response.ParentModelResponse;
import com.project.smart_intervention.factory.MessageFactory;
import com.project.smart_intervention.message.MessageConstant;
import jakarta.annotation.Resource;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @ClassName: WebSocketUtils
 * @Description:
 * @Date: 2025/4/8
 * @Version: 1.0
 */
@Component
public class WebSocketUtils {
    @Resource
    private SimpMessagingTemplate messagingTemplate;

    public void sendMessage(Message message) {
        MessageDTO messageDTO = MessageFactory.toMessageDTO(message);

        Result<MessageDTO> success = Result.success(messageDTO);

        messagingTemplate.convertAndSend( MessageConstant.WEBSOCKET_URL_PREFIX + message.getChatId(), success);
    }

    public void sendMessageDTO(MessageDTO aiMessageDTO) {
        Result<MessageDTO> success = Result.success(aiMessageDTO);
        messagingTemplate.convertAndSend( MessageConstant.WEBSOCKET_URL_PREFIX + aiMessageDTO.getChatId(), success);
    }

    public void sendDeadMessage(String url) {
        Result<String> error = Result.error(MessageConstant.SEND_MESSAGE_ERROR);
        messagingTemplate.convertAndSend(url, error);
    }

    /**
     * 鍙戦€佸喅绛栨爲娑堟伅
     * @param decisionTrees
     */
    public void sendDecisionTrees(String decisionTrees) {
        messagingTemplate.convertAndSend(MessageConstant.DECISION_TREES_URL, decisionTrees);
    }

    /**
     * 鍙戦€佹秷鎭埌鍓嶇
     * @param chatId
     * @param message
     */
    public void sendInstruction(Integer chatId, ExpertsAIMessage message) {
        ExpertsAIMessageDTO messageDTO = MessageFactory.toExpertsMessagesDTO(message);
        Result<ExpertsAIMessageDTO> success = Result.success(messageDTO);
        messagingTemplate.convertAndSend(
                MessageConstant.WEBSOCKET_URL_PREFIX + chatId + MessageConstant.EXPERT_AI_URL,
                success);
    }

    /**
     * 鑾峰彇鎺ㄨ崘娑堟伅
     * @param message
     */
    public void sendRecommendMessage(AIRecommendMessage message) {
        Result<String> success = Result.success(message.getMessageContent());
        messagingTemplate.convertAndSend(
        MessageConstant.WEBSOCKET_URL_PREFIX + message.getChatId() + MessageConstant.EXPERT_AI_URL,
                success);
    }

    /**
     * 鑾峰彇瀹堕暱鍥炲绛栫暐
     * @param model
     */
    public void sendParentModel(Integer chatId, ParentModelResponse model) {
        Result<ParentModelResponse> success = Result.success(model);
        messagingTemplate.convertAndSend(
                MessageConstant.WEBSOCKET_URL_PREFIX + chatId + MessageConstant.PARENT_MODEL_URL,
                success);
    }
}
