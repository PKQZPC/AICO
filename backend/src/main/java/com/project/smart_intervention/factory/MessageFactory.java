package com.project.smart_intervention.factory;

import com.project.smart_intervention.entity.dto.ExpertsAIMessageDTO;
import com.project.smart_intervention.entity.dto.ExpertsMessagesDTO;
import com.project.smart_intervention.entity.dto.MessageDTO;
import com.project.smart_intervention.entity.dto.SendMessageDTO;
import com.project.smart_intervention.entity.pojo.ExpertsAIMessage;
import com.project.smart_intervention.entity.pojo.HistoryInstruction;
import com.project.smart_intervention.entity.pojo.Message;
import com.project.smart_intervention.entity.pojo.SimpleMessage;
import com.project.smart_intervention.entity.request.ExpertInstructionRequest;
import com.project.smart_intervention.entity.request.InstructionRequest;
import com.project.smart_intervention.entity.request.SendMessageRequest;
import com.project.smart_intervention.entity.response.AIResponse;
import com.project.smart_intervention.message.MessageConstant;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * @ClassName: MessageFactory
 * @Description:
 * @Date: 2025/4/11
 * @Version: 1.0
 */
public class MessageFactory {

    public static SendMessageDTO toSendMessageDTO(Message message) {
        return new SendMessageDTO(message.getId());
    }

    public static MessageDTO toMessageDTO(Message message) {
        Long createTimestamp = message.getCreateTimestamp(); // 鍋囪鏄绾ф椂闂存埑
        LocalDateTime dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(createTimestamp),
                ZoneId.systemDefault() // 浣跨敤绯荤粺榛樿鏃跺尯
        );
        return MessageDTO.builder()
                .messageId(message.getId())
                .chatId(message.getChatId())
                .senderId(message.getSenderId())
                .content(message.getMessageContent())
                .senderIdentity(message.getSenderIdentity())
                .createTimestamp(dateTime)
                .build();
    }

    public static ExpertsAIMessageDTO toExpertsMessagesDTO(ExpertsAIMessage expertsAIMessages) {
        Long createTimestamp = expertsAIMessages.getCreateTimestamp(); // 鍋囪鏄绾ф椂闂存埑
        LocalDateTime dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(createTimestamp),
                ZoneId.systemDefault() // 浣跨敤绯荤粺榛樿鏃跺尯
        );
        return ExpertsAIMessageDTO.builder()
                .id(expertsAIMessages.getId())
                .chatId(expertsAIMessages.getChatId())
                .senderIdentity(expertsAIMessages.getSenderIdentity())
                .content(expertsAIMessages.getMessageContent())
                .senderId(expertsAIMessages.getSenderId())
                .timestamp(dateTime)
                .build();
    }

    public static SimpleMessage toSimpleMessage(Message message) {
        Integer senderType = switch (message.getSenderIdentity()) {
            case MessageConstant.PARENT_IDENTITY -> 0;
            case MessageConstant.BOT_IDENTITY -> 2;
            case MessageConstant.EXPERT_IDENTITY -> 1;
            default -> null;
        };
        return SimpleMessage.builder()
                .senderIdentity(senderType)
                .messageContent(message.getMessageContent())
                .build();
    }

    public static Message createWithAIResponse(AIResponse aiResponse, Message lastMessage) {
        long timeStamp = System.currentTimeMillis() / 1000;
        return Message.builder()
                .chatId(lastMessage.getChatId())
                .createTimestamp(timeStamp)
                .messageType(MessageConstant.DEFAULT_MESSAGE_TYPE)
                .messageContent(aiResponse.getAllReply())
                .messageCategory(MessageConstant.DEFAULT_MESSAGE_CATEGORY)
                .senderIdentity(MessageConstant.BOT_IDENTITY)
                .receiverIdentity(MessageConstant.PARENT_IDENTITY)
                .machineScore(aiResponse.getScore().floatValue())
                .senderId(lastMessage.getReceiverId())
                .receiverId(lastMessage.getSenderId())
                .build();
    }

    public static Message createWithSendMessageRequest(SendMessageRequest request, String senderIdentity) {
        long timeStamp = System.currentTimeMillis() / 1000;
        return Message.builder()
                .chatId(request.getChatId())
                .createTimestamp(timeStamp)
                .messageType(MessageConstant.DEFAULT_MESSAGE_TYPE)
                .messageContent(request.getContent())
                .messageCategory(MessageConstant.DEFAULT_MESSAGE_CATEGORY)
                .senderIdentity(senderIdentity)
                .receiverIdentity(senderIdentity.equals("parent") ? "expert" : "parent")
                .senderId(request.getSenderId())
                .receiverId(request.getReceiverId())
                .build();
    }

    public static ExpertsMessagesDTO createExpertsAIMessage(List<Message> commonMessages, List<ExpertsAIMessage> expertsAIMessages) {
        List<MessageDTO> commons = commonMessages.stream()
                .map(MessageFactory::toMessageDTO)
                .toList();
        List<ExpertsAIMessageDTO> aiMessageDTOS = expertsAIMessages.stream()
                .map(MessageFactory::toExpertsMessagesDTO)
                .toList();
        return new ExpertsMessagesDTO(commons, aiMessageDTOS);
    }

    /**
     * 鍒涘缓涓撳鎸囩ず娑堟伅
     * @param request
     * @return
     */
    public static ExpertsAIMessage toExpertsAIMessages(Integer chatId, ExpertInstructionRequest request) {
        return ExpertsAIMessage.builder()
                .type(request.getType())
                .messageType(MessageConstant.DEFAULT_MESSAGE_TYPE)
                .messageCategory(MessageConstant.DEFAULT_MESSAGE_CATEGORY)
                .senderIdentity(MessageConstant.EXPERT_IDENTITY)
                .senderId(request.getExpertId())
                .messageContent(request.getContent())
                .chatId(chatId)
                .createTimestamp(System.currentTimeMillis())
                .build();
    }

    public static InstructionRequest toInstructionRequest(String knowledgeBaseId, ExpertsAIMessage message, List<HistoryInstruction> historyInstructions) {
        return InstructionRequest.builder()
                .currentInstruction(message.getMessageContent())
                .type(message.getType())
                .historyInstructions(historyInstructions)
                .build();
    }
}
