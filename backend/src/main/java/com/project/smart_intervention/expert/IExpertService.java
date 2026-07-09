package com.project.smart_intervention.expert;

import com.baomidou.mybatisplus.extension.service.IService;
import com.project.smart_intervention.entity.dto.ChatDTO;
import com.project.smart_intervention.entity.dto.ExpertDTO;
import com.project.smart_intervention.entity.dto.SendMessageDTO;
import com.project.smart_intervention.entity.pojo.Expert;
import com.project.smart_intervention.entity.request.SendMessageRequest;

import java.util.List;

/**
 * @InterfaceName: IExpertService
 * @Description:
 * @Date: 2025/4/7
 * @Version: 1.0
 */
public interface IExpertService extends IService<Expert> {
    /**
     * 鑾峰彇鎵€鏈夌殑瀵硅瘽鍒楄〃
     * @param expertId
     * @return
     */
    List<ChatDTO> getChatList(Long expertId);

    /**
     * 鍙戦€佹秷鎭?     * @param request
     * @return
     */
    SendMessageDTO sendMessage(SendMessageRequest request);

    /**
     * 鑾峰彇鎵€鏈夌殑涓撳鍒楄〃
     * @return
     */
    List<ExpertDTO> listExperts();
}
