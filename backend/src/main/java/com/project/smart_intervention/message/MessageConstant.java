package com.project.smart_intervention.message;

/**
 * @ClassName: MessageConstant
 * @Description:
 * @Date: 2025/4/7
 * @Version: 1.0
 */
public class MessageConstant {
    public static final Double NEED_EXPERT_SCORE = 0.5;
    public static final String WEBSOCKET_URL_PREFIX = "/topic/chat/";
    public static final String DECISION_TREES_URL = "/topic/decision_trees";
    public static final String EXPERT_AI_URL = "/expert_ai";
    public static final String PARENT_MODEL_URL = "/parent-model";
    public static final String PARENT_IDENTITY = "parent";
    public static final String BOT_IDENTITY = "bot";
    public static final String EXPERT_IDENTITY = "expert";
    public static final String SEND_MESSAGE_ERROR = "鍙戦€佹秷鎭け璐?";
    // TODO 杩欓噷閮借鏀?
    public static final Integer DEFAULT_MESSAGE_TYPE = 0;
    public static final Integer DEFAULT_MESSAGE_CATEGORY = 0;
}
