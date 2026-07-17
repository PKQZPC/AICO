package com.project.smart_intervention.expert_ai;

import com.project.smart_intervention.entity.constant.SuccessConstant;
import com.project.smart_intervention.entity.pojo.Result;
import com.project.smart_intervention.entity.request.ExpertInstructionRequest;
import com.project.smart_intervention.entity.request.SendMessageRequest;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName: ExpertAIController
 * @Description:
 * @Date: 2025/4/14
 * @Version: 1.0
 */
@RestController
@RequestMapping("/expert_ai")
@Slf4j
public class ExpertAIController {

    @Resource
    private ExpertAIService expertAIService;

    /**
     * 涓撳缁橝I鎸囩ず
     * @param chatId
     * @param request
     * @return
     */
    @PostMapping("/chats/{chatId}/instructions")
    public Result<Long> expertInstruction(@PathVariable("chatId") Integer chatId, @RequestBody ExpertInstructionRequest request) {
        Long messageId = expertAIService.expertInstruction(chatId, request);
        return Result.success(messageId);
    }

    /**
     * 涓撳鑾峰彇AI鎺ㄨ崘鍥炲
     * @param chatId
     * @return
     */
    @GetMapping("/chats/{chatId}/recommend")
    public Result<String> getRecommendMessage(@PathVariable Integer chatId) {
        log.info("涓撳璇锋眰AI鍥炲: {}", chatId);
        expertAIService.getRecommendMessage(chatId);

        return Result.success(SuccessConstant.SUCCESS);
    }

    @PostMapping("/chats/{chatId}/adopt")
    public Result<String> adoptMessage(@PathVariable Integer chatId, @RequestBody SendMessageRequest request) {
        log.info("涓撳閲囩撼AI鍥炲: {}", request.getContent());
        expertAIService.adoptMessage(chatId, request);

        return Result.success(SuccessConstant.SUCCESS);
    }
}
