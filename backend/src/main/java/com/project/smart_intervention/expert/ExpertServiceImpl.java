package com.project.smart_intervention.expert;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.project.smart_intervention.chat.ChatConstant;
import com.project.smart_intervention.chat.IChatService;
import com.project.smart_intervention.config.RabbitConfig;
import com.project.smart_intervention.entity.dto.ChatDTO;
import com.project.smart_intervention.entity.dto.ExpertDTO;
import com.project.smart_intervention.entity.dto.SendMessageDTO;
import com.project.smart_intervention.entity.pojo.Chat;
import com.project.smart_intervention.entity.pojo.Expert;
import com.project.smart_intervention.entity.pojo.Message;
import com.project.smart_intervention.entity.pojo.Parent;
import com.project.smart_intervention.entity.request.SendMessageRequest;
import com.project.smart_intervention.exceptions.ChatException;
import com.project.smart_intervention.exceptions.ExpertException;
import com.project.smart_intervention.exceptions.MessageException;
import com.project.smart_intervention.factory.ChatFactory;
import com.project.smart_intervention.factory.ExpertFactory;
import com.project.smart_intervention.factory.MessageFactory;
import com.project.smart_intervention.message.MessageConstant;
import com.project.smart_intervention.message.MessageService;
import com.project.smart_intervention.parent.ParentMapper;
import com.project.smart_intervention.util.WebSocketUtils;
import jakarta.annotation.Resource;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

import static com.project.smart_intervention.parent.ParentServiceImpl.getMessage;

/**
 * @ClassName: ExpertServiceImpl
 * @Description:
 * @Date: 2025/4/7
 * @Version: 1.0
 */
@Service
public class ExpertServiceImpl extends ServiceImpl<ExpertMapper, Expert> implements IExpertService {

    @Resource
    private IChatService chatService;
    @Resource
    private ParentMapper parentMapper;
    @Resource
    private MessageService messageService;
    @Resource
    private RabbitTemplate rabbitTemplate;
    @Resource
    private WebSocketUtils webSocketUtils;

    /**
     * 鑾峰彇鎵€鏈夌殑瀵硅瘽鍒楄〃
     * @param expertId
     * @return
     */
    public List<ChatDTO> getChatList(Long expertId) {
        // 1. 杩涜鏍￠獙
        Expert expert = query().eq("id", expertId).one();
        if (expert == null) {
            throw new ExpertException(ExpertConstant.NOT_EXIST_ERROR);
        }
        // 2. 鏍规嵁id鏌ヨ鎵€鏈変細璇?
        List<Chat> chats = chatService.query().eq("expert_id", expert.getId()).list();
        // 濡傛灉涓虹┖锛岀洿鎺ヨ繑鍥?
        if (chats == null || chats.isEmpty()) {
            return Collections.emptyList();
        }
        // 鏌ヨ鎵€鏈夌殑瀹堕暱鍚嶅瓧
        List<String> parents = chats.stream()
                .map(chat -> parentMapper.selectById(chat.getParentId()))
                .map(Parent::getName)
                .toList();
        // 3. 鏍规嵁鎵€鏈変細璇濈殑鏃堕棿鎴筹紝鏌ヨ娑堟伅
        List<String> messages = chats.stream()
                .map(this::getLastMessage)
                .map(Message::getMessageContent)
                .toList();
        // 4. 灏佽杩斿洖
        return ChatFactory.toDTOs(chats, messages, parents);
    }

    public SendMessageDTO sendMessage(SendMessageRequest request) {
        // 1. 鑾峰彇浼氳瘽锛屾煡璇㈡槸鍚﹀瓨鍦?
        Chat chat = chatService.query().eq("id", request.getChatId()).one();
        if (chat == null) {
            throw new ChatException(ChatConstant.CHAT_NOT_EXIST_ERROR);
        }
        // 鑾峰彇娑堟伅
        Message message = MessageFactory.createWithSendMessageRequest(request, MessageConstant.EXPERT_IDENTITY);
        // 淇濆瓨娑堟伅
        boolean isSaved = messageService.save(message);
        if (!isSaved) {
            throw new MessageException(MessageConstant.SEND_MESSAGE_ERROR);
        }
        // 鍙戦€佹秷鎭埌娑堟伅闃熷垪
        rabbitTemplate.convertAndSend(RabbitConfig.SEND_MESSAGE_QUEUE, message);
        // 鍙戦€佹秷鎭埌鍓嶇
        webSocketUtils.sendMessage(message);
        // 杩斿洖缁撴灉
        return MessageFactory.toSendMessageDTO(message);
    }

    /**
     * 鑾峰彇鎵€鏈夌殑涓撳
     * @return
     */
    public List<ExpertDTO> listExperts() {
        List<Expert> list = query().list();
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return ExpertFactory.toDTOs(list);
    }

    private Message getLastMessage(Chat chat) {
        return getMessage(chat, messageService);
    }
}
