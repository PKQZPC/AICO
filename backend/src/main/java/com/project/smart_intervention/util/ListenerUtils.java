package com.project.smart_intervention.util;

import cn.hutool.core.bean.BeanUtil;
import com.project.smart_intervention.chat.ChatConstant;
import com.project.smart_intervention.chat.IChatService;
import com.project.smart_intervention.entity.constant.SystemConstant;
import com.project.smart_intervention.entity.dto.MessageDTO;
import com.project.smart_intervention.entity.pojo.*;
import com.project.smart_intervention.entity.request.*;
import com.project.smart_intervention.entity.response.AIResponse;
import com.project.smart_intervention.entity.response.ParentModelResponse;
import com.project.smart_intervention.entity.response.Response;
import com.project.smart_intervention.exceptions.MessageException;
import com.project.smart_intervention.factory.ChatFactory;
import com.project.smart_intervention.factory.MessageFactory;
import com.project.smart_intervention.factory.RequestFactory;
import com.project.smart_intervention.message.AIRecommendMessageMapper;
import com.project.smart_intervention.message.ExpertsAIMessageService;
import com.project.smart_intervention.message.MessageConstant;
import com.project.smart_intervention.message.MessageService;
import com.project.smart_intervention.parent.IParentService;
import com.project.smart_intervention.parent_model.ParentModel;
import com.project.smart_intervention.parent_model.ParentModelService;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

/**
 * @ClassName: ListenerUtils
 * @Description:
 * @Date: 2025/4/11
 * @Version: 1.0
 */
@Slf4j
@Component
public class ListenerUtils {

    @Resource
    private MessageService messageService;
    @Resource
    private IChatService chatService;
    @Resource
    private HttpClientUtils httpClientUtils;
    @Resource
    private WebSocketUtils webSocketUtils;
    @Resource
    private SimpMessagingTemplate messagingTemplate;
    @Resource
    private RequestFactory factory;
    @Resource
    private ExpertsAIMessageService aiMessageService;
    @Resource
    private AIRecommendMessageMapper recommendMessageMapper;
    @Resource
    private IParentService parentService;
    @Resource
    private ParentModelService parentModelService;

    public void sendAIMessage(Message message) throws Exception {
        String content = message.getMessageContent();
        // 1. 鏃ュ織杈撳嚭
        log.info("娑堟伅澶勭悊: {}", content);
        // 2. 灏佽娑堟伅
        Chat chat = chatService.query().eq("id", message.getChatId()).one();
        // 3. 濡傛灉涓嶆槸瀹堕暱锛屽苟涓斾笉鏄疉I鎵樼锛岀洿鎺ヨ繑鍥?        if (!message.getSenderIdentity().equals(MessageConstant.PARENT_IDENTITY)) {
            return;
        }
        if (!chat.getChatState().equals(ChatConstant.AI_HOSTING_STATUS)) {
            return;
        }
        // 4. 鑾峰彇AI鍥炲
        List<Message> messages = messageService.query().eq("chat_id", message.getChatId()).list();
        List<SimpleMessage> simpleList = messages.stream()
                .map(MessageFactory::toSimpleMessage)
                .toList();
        AIRequest aiRequest = factory.createAIRequest(chat, simpleList, content);
        Response<AIResponse> aiResponseResult = httpClientUtils.postToAI(
                SystemConstant.ALGORITHM_URL_PREFIX + SystemConstant.GET_AI_REPLY,
                    aiRequest,
                AIResponse.class);
        AIResponse aiResponse = aiResponseResult.getResult();
        // 淇濆瓨娑堟伅
        Message aiMessage = MessageFactory.createWithAIResponse(aiResponse, message);
        MessageDTO aiMessageDTO = MessageFactory.toMessageDTO(aiMessage);
        aiMessageDTO.setNeedExpert(aiResponse.getScore() < MessageConstant.NEED_EXPERT_SCORE);
        // 濡傛灉鑾峰彇鍒扮殑鍒嗘暟杈惧埌涓寸晫鍊硷紝闇€瑕佷慨鏀圭姸鎬?        if (aiResponse.getScore() < MessageConstant.NEED_EXPERT_SCORE) {
            chat.setChatState(ChatConstant.SPECIALIST_TO_BE_INTERVENED_STATUS);
        }
        boolean isSaved = messageService.save(aiMessage);
        if (!isSaved) {
            throw new MessageException(MessageConstant.SEND_MESSAGE_ERROR);
        }
        log.info("AI鍥炲锛?{}", aiResponse.getAllReply());
        // 5. 鎺ㄩ€佹秷鎭?        webSocketUtils.sendMessageDTO(aiMessageDTO);
        // 5.1 涓哄綋鍓嶅璇濊缃璇濊疆鏁?        Integer rounds = chat.getRounds() + 1;
        chatService.update().eq("id", chat.getId()).setSql("rounds=rounds+1").update();
        if (!(rounds % 5 == 0 && rounds > 0)) {
            return;
        }
        // 6. 鍐嶈幏鍙栧埌瀹堕暱鐨勫缓妯?        GetModelRequest modelRequest = new GetModelRequest();
        modelRequest.setMessages(aiRequest.getMessages());
        Long parentId = chat.getParentId();
        Parent parent = parentService.query().eq("id", parentId).one();
        modelRequest.setParentName(parent.getName());
        // 鏋勫缓current_chats_reply_basis
        List<ParentModel> parentModels = parentModelService.query().eq("chat_id", message.getChatId()).list();
        if (parentModels == null || parentModels.isEmpty()) {
            parentModels = List.of();
        }
        List<ParentModelAlgorithmRequest> parentModelAlgorithmRequests = parentModels.stream()
                .map(model -> BeanUtil.copyProperties(model, ParentModelAlgorithmRequest.class))
                .toList();
        modelRequest.setCurrentChatsReplyBasis(parentModelAlgorithmRequests);
        Response<ParentModelResponse> response = httpClientUtils.postToAI(SystemConstant.ALGORITHM_URL_PREFIX + SystemConstant.GET_MODEL_URL,
                modelRequest,
                ParentModelResponse.class);
        ParentModelResponse model = response.getResult();
        if (model != null) {
            // 7. 鎺ㄩ€佹秷鎭?            webSocketUtils.sendParentModel(chat.getId(), model);
            // 瀛樺叆鏁版嵁搴?            LocalDateTime latestMessageTime = getLastMessageTime(messages);
            ParentModel parentModel = ChatFactory.toParentModel(message.getChatId(), model, latestMessageTime);
            parentModelService.save(parentModel);
        }
    }

    private LocalDateTime getLastMessageTime(List<Message> messages) {
        List<Long> longList = messages.stream()
                .map(Message::getCreateTimestamp)
                .sorted(Comparator.reverseOrder())
                .toList();
        int size = longList.size();
        Long timestamp = longList.get(size - 1);
        // 鏂规硶1锛氭槑纭寚瀹氭椂鍖猴紙鎺ㄨ崘锛?        ZoneId zone = ZoneId.of("Asia/Shanghai"); // 鎴?ZoneId.systemDefault()
        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp),
                ZoneId.of("Asia/Shanghai")
        );
    }

    @SneakyThrows
    public void sendDecisionTrees() {
        // 1. 鍙戦€佽姹傦紝鑾峰彇杩斿洖娑堟伅
        String decisionTrees = httpClientUtils.getDecisionTree();
        // 2. 鍙戦€佹秷鎭?        webSocketUtils.sendDecisionTrees(decisionTrees);
    }

    /**
     * 鍙戦€?     */
    @SneakyThrows
    public void updateDecisionTrees(String tree) {

        httpClientUtils.updateDecisionTree(tree);
    }

    /**
     * 鍙戦€佷笓瀹舵寚浠?     */
    public void sendInstruction(ExpertsAIMessage message) {
        log.info("涓撳鎸囦护锛歿}", message.getMessageContent());
        // 鑾峰彇浼氳瘽id
        Integer chatId = message.getChatId();
        // 鍙戦€佹秷鎭埌鍓嶇
        webSocketUtils.sendInstruction(chatId, message);
        // 鑾峰彇鐭ヨ瘑搴?        Chat chat = chatService.query().eq("id", chatId).one();
        String knowledgeBaseId = chat.getChatKnowledgeBaseId();
        // 鑾峰彇鍘嗗彶鎸囩ず
        List<ExpertsAIMessage> expertsAIMessages = aiMessageService.query().eq("chat_id", chatId).list();
        List<HistoryInstruction> historyInstructions = expertsAIMessages.stream()
                .map(message1 -> new HistoryInstruction(message1.getType(), message1.getMessageContent()))
                .toList();
        InstructionRequest request = MessageFactory.toInstructionRequest(knowledgeBaseId, message, historyInstructions);
        // 鍙戦€佽姹?        httpClientUtils.sendInstruction(request);
    }

    /**
     * 鑾峰彇AI鍥炲
     * @param chat
     */
    public void getRecommend(Chat chat) throws Exception {
        log.info("鑾峰彇AI鍥炲");
        AIRequest request = factory.createAIRequest(chat);
        // 鑾峰彇AI鍥炲
        Response<AIResponse> aiResponseResult = httpClientUtils.postToAI(
                SystemConstant.ALGORITHM_URL_PREFIX + SystemConstant.GET_AI_REPLY,
                request,
                AIResponse.class);
        AIResponse aiResponse = aiResponseResult.getResult();
        // 淇濆瓨娑堟伅
        String content = aiResponse.getAllReply();
        AIRecommendMessage message = new AIRecommendMessage();
        message.setMessageContent(content);
        message.setChatId(chat.getId());
        message.setCreateTimestamp(System.currentTimeMillis());
        recommendMessageMapper.insert(message);
        // 鍙戦€佹秷鎭埌鍓嶇
        webSocketUtils.sendRecommendMessage(message);
    }
}
