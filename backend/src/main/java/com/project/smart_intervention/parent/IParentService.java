package com.project.smart_intervention.parent;

import com.baomidou.mybatisplus.extension.service.IService;
import com.project.smart_intervention.entity.dto.ChatDTO;
import com.project.smart_intervention.entity.dto.LoginDTO;
import com.project.smart_intervention.entity.dto.ParentDTO;
import com.project.smart_intervention.entity.dto.SendMessageDTO;
import com.project.smart_intervention.entity.pojo.Parent;
import com.project.smart_intervention.entity.request.LoginRequest;
import com.project.smart_intervention.entity.request.SendMessageRequest;

import java.util.List;

/**
 * @InterfaceName: IParentSerivce
 * @Description:
 * @Date: 2025/4/7
 * @Version: 1.0
 */
public interface IParentService extends IService<Parent> {
    /**
     * 鐢ㄦ埛鐧诲綍
     * @param loginRequest
     * @return
     */
    LoginDTO login(LoginRequest loginRequest);

    /**
     * 鑾峰彇娑堟伅鍒楄〃
     * @return
     */
    List<ChatDTO> getChatList(Long parentId);

    /**
     * 鍙戦€佹秷鎭?     * @param request
     * @return
     */
    SendMessageDTO sendMessage(SendMessageRequest request);

    /**
     * 鑾峰彇瀹堕暱鎵€鏈変俊鎭?     * @return
     */
    List<ParentDTO> listParents();
}
