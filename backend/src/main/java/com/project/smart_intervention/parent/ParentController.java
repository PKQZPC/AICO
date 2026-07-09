package com.project.smart_intervention.parent;

import com.project.smart_intervention.entity.dto.ChatDTO;
import com.project.smart_intervention.entity.dto.LoginDTO;
import com.project.smart_intervention.entity.dto.ParentDTO;
import com.project.smart_intervention.entity.dto.SendMessageDTO;
import com.project.smart_intervention.entity.pojo.Result;
import com.project.smart_intervention.entity.request.LoginRequest;
import com.project.smart_intervention.entity.request.SendMessageRequest;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @ClassName: ParentController
 * @Description:
 * @Date: 2025/4/7
 * @Version: 1.0
 */
@RestController
@RequestMapping("/parents")
@Slf4j
public class ParentController {

    @Resource
    private IParentService parentService;

    /**
     * 瀹堕暱鐧诲綍
     * @param loginRequest
     * @return
     */
    @PostMapping("/login")
    public Result<LoginDTO> login(@RequestBody LoginRequest loginRequest) {
        log.info("鐢ㄦ埛鐧诲綍: {}", loginRequest.getUsername());
        LoginDTO login = parentService.login(loginRequest);
        return Result.success(login, ParentConstant.PARENT_LOGIN_SUCCESS);
    }


    /**
     * 鑾峰彇浼氳瘽鍒楄〃
     * @param parentId
     * @return
     */
    @GetMapping("/{parentId}/get_chats")
    public Result<List<ChatDTO>> getChatList(@PathVariable("parentId") Long parentId) {
        List<ChatDTO> chatList = parentService.getChatList(parentId);
        return Result.success(chatList);
    }

    /**
     * 鍙戦€佹秷鎭?     * @param request 鍙戦€佹秷鎭姹傛牸寮?     * @return
     */
    @PostMapping("/send_message")
    public Result<SendMessageDTO> sendMessage(@RequestBody SendMessageRequest request) {
        log.info("瀹堕暱{}鍙戦€佹秷鎭?", request.getSenderId());
        SendMessageDTO sendMessage = parentService.sendMessage(request);
        return Result.success(sendMessage);
    }

    /**
     * 鍙戦€佽幏鍙栨墍鏈夊闀夸俊鎭?     * @return
     */
    @GetMapping
    public Result<List<ParentDTO>> listParents() {
        List<ParentDTO> parents = parentService.listParents();
        return Result.success(parents);
    }
}
