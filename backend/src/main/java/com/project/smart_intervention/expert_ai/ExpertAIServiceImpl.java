package com.project.smart_intervention.expert_ai;

import com.project.smart_intervention.chat.ChatConstant;
import com.project.smart_intervention.chat.ChatServiceImpl;
import com.project.smart_intervention.chat.IChatService;
import com.project.smart_intervention.config.RabbitConfig;
import com.project.smart_intervention.entity.pojo.Chat;
import com.project.smart_intervention.entity.pojo.ExpertsAIMessage;
import com.project.smart_intervention.entity.request.ExpertInstructionRequest;
import com.project.smart_intervention.entity.request.SendMessageRequest;
import com.project.smart_intervention.exceptions.ChatException;
import com.project.smart_intervention.exceptions.MessageException;
import com.project.smart_intervention.expert.IExpertService;
import com.project.smart_intervention.factory.MessageFactory;
import com.project.smart_intervention.message.AIRecommendMessageMapper;
import com.project.smart_intervention.message.ExpertsAIMessageService;
import com.project.smart_intervention.message.MessageConstant;
import jakarta.annotation.Resource;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.SavepointManager;
import org.springframework.transaction.annotation.Transactional;

/**
 * @ClassName: ExpertAIServiceImpl
 * @Description:
 * @Date: 2025/4/14
 * @Version: 1.0
 */
@Service
public class ExpertAIServiceImpl implements ExpertAIService {

    @Resource
    private RabbitTemplate rabbitTemplate;
    @Resource
    private ExpertsAIMessageService aiMessageService;
    @Resource
    private IChatService chatService;
    @Resource
    private IExpertService expertService;
    @Resource
    private AIRecommendMessageMapper recommendMessageMapper;

    /**
     * 涓撳鍙戦€佹寚绀?     * @param chatId
     * @param request
     */
    public Long expertInstruction(Integer chatId, ExpertInstructionRequest request) {
        // 1. 灏佽娑堟伅
        ExpertsAIMessage expertsAIMessage = MessageFactory.toExpertsAIMessages(chatId, request);
        // 2. 淇濆瓨娑堟伅
        boolean isSaved = aiMessageService.save(expertsAIMessage);
        if (!isSaved) {
            throw new MessageException(MessageConstant.SEND_MESSAGE_ERROR);
        }
        // 3. 杩斿洖娑堟伅id
        return expertsAIMessage.getId();
    }

    /**
     * 鑾峰彇AI鎺ㄨ崘鍥炲
     * @param chatId
     * @return
     */
    public void getRecommendMessage(Integer chatId) {
        // 鏌ヨchatId鏄惁瀛樺湪
        Chat chat = chatService.query().eq("id", chatId).one();
        if (chat == null) {
            throw new ChatException(ChatConstant.CHAT_NOT_EXIST_ERROR);
        }
        // 杩炲悓娑堟伅涓€骞跺彂閫?
        rabbitTemplate.convertAndSend(RabbitConfig.GET_RECOMMEND_MESSAGE_QUEUE, chat);
    }

    /**
     * 涓撳閲囩撼AI鍥炲
     * @param chatId
     * @param request
     */
    @Transactional
    public void adoptMessage(Integer chatId, SendMessageRequest request) {
        // 涓撳鍙戦€佹秷鎭?
        expertService.sendMessage(request);
        // 鍚屾椂鍒犻櫎AI娑堟伅
        recommendMessageMapper.deleteByChatId(request.getChatId());
    }
}
