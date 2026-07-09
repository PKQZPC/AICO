package com.project.smart_intervention.factory;

import com.project.smart_intervention.entity.pojo.*;
import com.project.smart_intervention.entity.request.AIRequest;
import com.project.smart_intervention.entity.request.InstructionRequest;
import com.project.smart_intervention.message.ExpertsAIMessageService;
import com.project.smart_intervention.message.MessageService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * @ClassName: RequestFactory
 * @Description:
 * @Date: 2025/4/11
 * @Version: 1.0
 */
@Component
public class RequestFactory {

    @Resource
    private MessageService messageService;
    @Resource
    private ExpertsAIMessageService aiMessageService;

    public AIRequest createAIRequest(Chat chat, List<SimpleMessage> messages, String content) {
        AIRequest aiRequest = new AIRequest();
        aiRequest.setCurrentContent(content);
        aiRequest.setExpertType(0);
        aiRequest.setChatKnowledgeBaseId(chat.getChatKnowledgeBaseId());
        aiRequest.setKnowledgeBaseUuid(chat.getChatKnowledgeBaseId());

        aiRequest.setMessages(messages);

        // 鏌ヨ鎵€鏈堿I鎸囩ず淇℃伅
        List<ExpertsAIMessage> expertsAIMessages = aiMessageService.query().eq("chat_id", chat.getId()).list();
        if (expertsAIMessages == null || expertsAIMessages.isEmpty()) {
            expertsAIMessages = List.of();
        }
        List<HistoryInstruction> historyInstructions = expertsAIMessages.stream()
                .map(message -> new HistoryInstruction(message.getType(), message.getMessageContent()))
                .toList();
        HistoryInstruction currentInstruction = getCurrentInstruction(expertsAIMessages);
        if (currentInstruction.getInstruction() == null) {
            currentInstruction = new HistoryInstruction("", "");
        }
        InstructionRequest request = new InstructionRequest(currentInstruction.getInstruction(),
                currentInstruction.getType(),
                historyInstructions);
        aiRequest.setInstruction(request);

        return aiRequest;
    }

    private HistoryInstruction getCurrentInstruction(List<ExpertsAIMessage> expertsAIMessages) {
        Optional<ExpertsAIMessage> first = expertsAIMessages.stream()
                .min((m1, m2) -> Math.toIntExact(m2.getCreateTimestamp() - m1.getCreateTimestamp()));
        if (first.isPresent()) {
            ExpertsAIMessage expertsAIMessage = first.get();
            return new HistoryInstruction(expertsAIMessage.getType(), expertsAIMessage.getMessageContent());
        } else {
            return new HistoryInstruction("", "");
        }
    }

    /**
     * 鐢熸垚AIRequest
     * @param chat
     * @return
     */
    public AIRequest createAIRequest(Chat chat) {
        AIRequest aiRequest = new AIRequest();
        aiRequest.setCurrentContent("");
        aiRequest.setExpertType(0);
        aiRequest.setChatKnowledgeBaseId(chat.getChatKnowledgeBaseId());
        aiRequest.setKnowledgeBaseUuid(chat.getChatKnowledgeBaseId());
        List<Message> messages = messageService.query().eq("chat_id", chat.getId()).list();
        List<SimpleMessage> simpleList = messages.stream()
                .map(MessageFactory::toSimpleMessage)
                .toList();
        aiRequest.setMessages(simpleList);

        return aiRequest;
    }
}
