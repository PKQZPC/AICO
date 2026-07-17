package com.project.smart_intervention.chat;

import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.smart_intervention.entity.dto.ChatDTO;
import com.project.smart_intervention.entity.dto.ExpertsMessagesDTO;
import com.project.smart_intervention.entity.dto.MessageDTO;
import com.project.smart_intervention.entity.dto.ReadTimeDTO;
import com.project.smart_intervention.entity.pojo.Result;
import com.project.smart_intervention.entity.request.*;
import com.project.smart_intervention.entity.response.ParentModelResponse;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @ClassName: ChatController
 * @Description:
 * @Date: 2025/4/7
 * @Version: 1.0
 */
@RestController
@RequestMapping("/chats")
@Slf4j
public class ChatController {

    @Resource
    private ChatFacadeService chatService;

    /**
     * 鍒涘缓浼氳瘽
     * @param request 鍒涘缓璇锋眰
     * @return 浼氳瘽淇℃伅
     */
    @PostMapping("/create_chat")
    public Result<ChatDTO> createChat(@RequestBody CreateChatRequest request) {
        log.info("鍒涘缓浼氳瘽: {} -> {}", request.getExpertId(), request.getParentId());

        ChatDTO chat = chatService.createChat(request);
        return Result.success(chat);
    }

    /**
     * 鑾峰彇娑堟伅鍒楄〃
     * @param chatId 浼氳瘽id
     * @return 娑堟伅鍒楄〃
     */
    @GetMapping("/{chatId}")
    public Result<List<MessageDTO>> listMessage(@PathVariable("chatId") Integer chatId) {
        log.info("鑾峰彇鎵€鏈夌殑娑堟伅鍒楄〃: {}", chatId);
        List<MessageDTO> messageList = chatService.listMessage(chatId);
        return Result.success(messageList);
    }

    /**
     * 鏇存敼浼氳瘽鐘舵€?     * @param chatId 浼氳瘽id
     * @return
     */
    @PutMapping("/{chatId}/status")
    public Result<String> putStatus(@PathVariable("chatId") Integer chatId) {
        log.info("鏇存敼浼氳瘽鐘舵€?");
        chatService.putStatus(chatId);
        return Result.success(ChatConstant.PUT_STATUS_SUCCESS);
    }

    /**
     * 鏇存敼浼氳瘽鐨勯槄璇绘椂闂?     * @param request 鏇存敼鐨勯槄璇绘椂闂?     * @param chatId 浼氳瘽id
     * @return
     */
    @PutMapping("/{chatId}/read_time")
    public Result<ReadTimeDTO> updateReadTime(@RequestBody UpdateReadTimeRequest request, @PathVariable Integer chatId) {
        log.info("鏇存柊浼氳瘽鐨勯槄璇绘椂闂? {}", request);
        ReadTimeDTO readTime = chatService.updateReadTime(request, chatId);
        return Result.success(readTime);
    }

    /**
     * 鍒犻櫎浼氳瘽
     * @param chatId 浼氳瘽id
     * @return
     */
    @DeleteMapping("/{chatId}")
    public Result<String> deleteChat(@PathVariable Integer chatId) {
        log.info("鍒犻櫎浼氳瘽: {}", chatId);
        chatService.deleteChat(chatId);
        return Result.success(ChatConstant.DELETE_CHAT_SUCCESS);
    }

    /**
     * 鑾峰彇涓撳鐨勬秷鎭垪琛?     * @param chatId
     * @return
     */
    @GetMapping("/experts/{chatId}")
    public Result<ExpertsMessagesDTO> listExpertsMessages(@PathVariable("chatId") Integer chatId) {
        ExpertsMessagesDTO expertsMessages = chatService.listExpertsMessages(chatId);
        return Result.success(expertsMessages);
    }

    /** TODO 杩欓噷闇€瑕佹敼
     * 鑾峰彇瀹堕暱鐨勬ā鍨?     * @param chatId
     * @return
     */
    @GetMapping("/{chatId}/parent-model")
    private Result<ParentModelResponse> getModel(@PathVariable Integer chatId) {
        log.info("鑾峰彇鐢ㄦ埛寤烘ā: {}", chatId);
        ParentModelResponse model = chatService.getModel(chatId);

        return Result.success(model);
    }
}
