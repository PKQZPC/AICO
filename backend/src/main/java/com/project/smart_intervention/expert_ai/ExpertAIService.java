package com.project.smart_intervention.expert_ai;

import com.project.smart_intervention.entity.request.ExpertInstructionRequest;
import com.project.smart_intervention.entity.request.SendMessageRequest;

/**
 * @ClassName: ExpertAIService
 * @Description:
 * @Date: 2025/4/14
 * @Version: 1.0
 */
public interface ExpertAIService {

    /**
     * 涓撳鍙戦€佹寚绀?     * @param chatId
     * @param request
     */
    Long expertInstruction(Integer chatId, ExpertInstructionRequest request);

    /**
     * 鑾峰彇AI鎺ㄨ崘鍥炲
     * @param chatId
     * @return
     */
    void getRecommendMessage(Integer chatId);

    /**
     * 涓撳閲囩撼AI鍥炲
     * @param chatId
     * @param request
     */
    void adoptMessage(Integer chatId, SendMessageRequest request);
}
