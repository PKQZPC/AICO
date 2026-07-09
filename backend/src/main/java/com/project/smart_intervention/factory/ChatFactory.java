package com.project.smart_intervention.factory;

import com.project.smart_intervention.chat.ChatConstant;
import com.project.smart_intervention.entity.dto.ChatDTO;
import com.project.smart_intervention.entity.dto.ReadTimeDTO;
import com.project.smart_intervention.entity.pojo.Chat;
import com.project.smart_intervention.entity.response.ParentModelResponse;
import com.project.smart_intervention.parent_model.ParentModel;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: ChatFactory
 * @Description:
 * @Date: 2025/4/11
 * @Version: 1.0
 */
public class ChatFactory {
    public static ChatDTO toDTO(Chat chat) {
        long timestamp = chat.getCreateTimestamp();
        LocalDateTime createTime = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp),
                ZoneId.systemDefault()); // 浣跨敤绯荤粺榛樿鏃跺尯
        return ChatDTO.builder()
                .id(chat.getId())
                .title(chat.getChatTitle())
                .parentId(chat.getParentId())
                .expertId(chat.getExpertId())
                .createdAt(createTime)
                .status(chat.getChatState())
                .build();
    }

    public static ChatDTO toDTO(Chat chat, String lastMessage, String name) {
        long timestamp = chat.getCreateTimestamp();
        LocalDateTime createTime = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp),
                ZoneId.systemDefault()); // 浣跨敤绯荤粺榛樿鏃跺尯
        Long lastMessageTimestamp = chat.getLastMessageTimestamp();
        LocalDateTime lastMessageTime = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(lastMessageTimestamp),
                ZoneId.systemDefault());
        return ChatDTO.builder()
                .id(chat.getId())
                .title(chat.getChatTitle())
                .parentId(chat.getParentId())
                .expertId(chat.getExpertId())
                .name(name)
                .createdAt(createTime)
                .lastMessage(lastMessage)
                .lastMessageTimestamp(lastMessageTime)
                .status(chat.getChatState())
                .build();
    }

    public static List<ChatDTO> toDTOs(List<Chat> chats, List<String> lastMessages, List<String> names) {
        List<ChatDTO> chatDTOs = new ArrayList<>();
        for (int i = 0; i < chats.size(); i++) {
            ChatDTO dto = ChatFactory.toDTO(chats.get(i), lastMessages.get(i), names.get(i));
            chatDTOs.add(dto);
        }
        return chatDTOs;
    }

    public static ReadTimeDTO toReadTimeDTO(Chat chat) {
        ReadTimeDTO readTimeDTO = new ReadTimeDTO();
        readTimeDTO.setChatId(chat.getId());
        readTimeDTO.setLastReadTimestampParent(chat.getLastReadTimestampParent());
        readTimeDTO.setLastReadTimestampExpert(chat.getLastReadTimestampExpert());
        return readTimeDTO;
    }

    public static Chat createChat(Long parentId, Long expertId) {
        long currentTimeMillis = System.currentTimeMillis();
        long timeStamp = currentTimeMillis / 1000;
        Chat chat = new Chat();
        chat.setSenderIdentity(ChatConstant.DEFAULT_SENDER_IDENTITY);
        chat.setParentId(parentId);
        chat.setExpertId(expertId);
        chat.setChatState(ChatConstant.DEFAULT_STATUS);
        chat.setChatKnowledgeBaseId("" + parentId + currentTimeMillis);
        chat.setChatKeyword(ChatConstant.DEFAULT_CHAT_KEYWORD);
        chat.setChatTitle(ChatConstant.DEFAULT_CHAT_TITLE);
        chat.setCreateTimestamp(timeStamp);
        chat.setLastMessageTimestamp(timeStamp);
        chat.setLastReadTimestampExpert(timeStamp);
        chat.setLastReadTimestampParent(timeStamp);
        chat.setRounds(ChatConstant.DEFAULT_ROUND);
        chat.setFavoriteState(ChatConstant.DEFAULT_FAVORITE_STATE);

        return chat;
    }

    public static ParentModel toParentModel(Integer chatId, ParentModelResponse response, LocalDateTime latestMessageTime) {
        return ParentModel.builder()
                .chatId(chatId)
                .eventSummary(response.getEventSummary())
                .replyStrategy(response.getReplyStrategy())
                .tag(response.getTag())
                .profile(response.getProfile())
                .latestMessageTime(latestMessageTime)
                .build();
    }
}
