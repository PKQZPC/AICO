package com.project.smart_intervention.parent;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.project.smart_intervention.chat.ChatConstant;
import com.project.smart_intervention.chat.IChatService;
import com.project.smart_intervention.config.RabbitConfig;
import com.project.smart_intervention.entity.dto.ChatDTO;
import com.project.smart_intervention.entity.dto.LoginDTO;
import com.project.smart_intervention.entity.dto.ParentDTO;
import com.project.smart_intervention.entity.dto.SendMessageDTO;
import com.project.smart_intervention.entity.pojo.*;
import com.project.smart_intervention.entity.request.LoginRequest;
import com.project.smart_intervention.entity.request.SendMessageRequest;
import com.project.smart_intervention.exceptions.ChatException;
import com.project.smart_intervention.exceptions.MessageException;
import com.project.smart_intervention.exceptions.ParentException;
import com.project.smart_intervention.expert.ExpertMapper;
import com.project.smart_intervention.factory.ChatFactory;
import com.project.smart_intervention.factory.MessageFactory;
import com.project.smart_intervention.factory.ParentFactory;
import com.project.smart_intervention.message.MessageConstant;
import com.project.smart_intervention.message.MessageService;
import com.project.smart_intervention.util.WebSocketUtils;
import jakarta.annotation.Resource;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @ClassName: ParentServiceImpl
 * @Description:
 * @Date: 2025/4/7
 * @Version: 1.0
 */
@Service
public class ParentServiceImpl extends ServiceImpl<ParentMapper, Parent> implements IParentService {

    @Resource
    private IChatService chatService;
    @Resource
    private MessageService messageService;
    @Resource
    private ExpertMapper expertMapper;
    @Resource
    private RabbitTemplate rabbitTemplate;
    @Resource
    private WebSocketUtils webSocketUtils;

    /**
     * 鐧诲綍
     * @param loginRequest
     * @return
     */
    public LoginDTO login(LoginRequest loginRequest) {
        String username = loginRequest.getUsername();
        if (username == null || username.isEmpty()) {
            throw new ParentException(ParentConstant.USERNAME_EMPTY_ERROR);
        }
        Parent parent = query().eq("username", username).one();
        if (parent == null) {
            throw new ParentException(ParentConstant.USER_NOT_EXIST_ERROR);
        }

        return ParentFactory.toLoginDTO(parent);
    }

    /**
     * 鑾峰彇娑堟伅鍒楄〃
     * @return
     */
    public List<ChatDTO> getChatList(Long parentId) {
        // 1. 杩涜鏍￠獙
        Parent parent = query().eq("id", parentId).one();
        if (parent == null) {
            throw new ParentException(ParentConstant.USER_NOT_EXIST_ERROR);
        }
        // 2. 鏍规嵁id鏌ヨ鎵€鏈変細璇?
        List<Chat> chats = chatService.query().eq("parent_id", parentId).list();
        // 濡傛灉涓虹┖锛岀洿鎺ヨ繑鍥?
        if (chats == null || chats.isEmpty()) {
            return List.of();
        }
        // 鏌ヨ鎵€鏈夌殑涓撳
        List<String> experts = chats.stream()
                .map(chat -> expertMapper.selectById(chat.getExpertId()))
                .map(Expert::getName)
                .toList();
        // 3. 鏍规嵁鎵€鏈変細璇濈殑鏃堕棿鎴筹紝鏌ヨ娑堟伅
        List<String> messages = chats.stream()
                .map(this::getLastMessage)
                .map(Message::getMessageContent)
                .toList();
        // 4. 灏佽杩斿洖
        return ChatFactory.toDTOs(chats, messages, experts);
    }

    /**
     * 鍙戦€佹秷鎭?     * @param request
     * @return
     */
    public SendMessageDTO sendMessage(SendMessageRequest request) {
        // 1. 鑾峰彇浼氳瘽锛屾煡璇㈡槸鍚﹀瓨鍦?
        Chat chat = chatService.query().eq("id", request.getChatId()).one();
        if (chat == null) {
            throw new ChatException(ChatConstant.CHAT_NOT_EXIST_ERROR);
        }
        // 鑾峰彇娑堟伅
        Message message = MessageFactory.createWithSendMessageRequest(request, MessageConstant.PARENT_IDENTITY);
        // 淇濆瓨娑堟伅
        boolean isSaved = messageService.save(message);
        if (!isSaved) {
            throw new MessageException(MessageConstant.SEND_MESSAGE_ERROR);
        }
        // 鍙戦€佹秷鎭埌娑堟伅闃熷垪
        rabbitTemplate.convertAndSend(RabbitConfig.SEND_MESSAGE_QUEUE, message);
        // 娑堟伅鎺ㄩ€佸埌鍓嶇
        webSocketUtils.sendMessage(message);
        // 杩斿洖缁撴灉
        return MessageFactory.toSendMessageDTO(message);
    }

    /**
     * 鑾峰彇鎵€鏈夌敤鎴?     * @return
     */
    public List<ParentDTO> listParents() {
        List<Parent> parents = query().list();
        if (parents == null || parents.isEmpty()) {
            return List.of();
        }
        return ParentFactory.toDTOs(parents);
    }

    private Message getLastMessage(Chat chat) {
        return getMessage(chat, messageService);
    }

    public static Message getMessage(Chat chat, MessageService messageService) {
        List<Message> messages = messageService.query().eq("chat_id", chat.getId()).list();
        if (messages == null || messages.isEmpty()) {
            Message message = new Message();
            message.setMessageContent("");
            return message;
        }
        messages =  messages.stream()
                .sorted(((o1, o2) -> Math.toIntExact(o2.getCreateTimestamp() - o1.getCreateTimestamp())))
                .limit(1)
                .toList();
        return messages.get(0);
    }
}
