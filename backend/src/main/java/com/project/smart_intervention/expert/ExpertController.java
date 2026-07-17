package com.project.smart_intervention.expert;

import com.project.smart_intervention.entity.dto.ChatDTO;
import com.project.smart_intervention.entity.dto.ExpertDTO;
import com.project.smart_intervention.entity.dto.SendMessageDTO;
import com.project.smart_intervention.entity.pojo.Result;
import com.project.smart_intervention.entity.request.SendMessageRequest;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @ClassName: ExpertController
 * @Description:
 * @Date: 2025/4/7
 * @Version: 1.0
 */
@RestController
@RequestMapping("/experts")
@Slf4j
public class ExpertController {

    @Resource
    private IExpertService expertService;

    @GetMapping("/{expertId}/get_chats")
    public Result<List<ChatDTO>> getChatList(@PathVariable("expertId") Long expertId) {
        List<ChatDTO> chatList = expertService.getChatList(expertId);
        return Result.success(chatList);
    }

    @PostMapping("/send_message")
    public Result<SendMessageDTO> sendMessage(@RequestBody SendMessageRequest request) {
        log.info("涓撳{}鍙戦€佹秷鎭?", request.getSenderId());
        SendMessageDTO sendMessage = expertService.sendMessage(request);
        return Result.success(sendMessage);
    }

    @GetMapping
    public Result<List<ExpertDTO>> listExperts() {
        List<ExpertDTO> experts = expertService.listExperts();
        return Result.success(experts);
    }
}
