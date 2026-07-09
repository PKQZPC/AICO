package com.project.smart_intervention.entity.constant;

/**
 * @ClassName: SystemConstant
 * @Description:
 * @Date: 2025/4/14
 * @Version: 1.0
 */
public class SystemConstant {
    public static final String ALGORITHM_URL_PREFIX = env("AICO_ALGORITHM_URL_PREFIX", "http://localhost:22500/mal/");
    public static final String GET_AI_REPLY = "get_ai_reply";
    public static final String GET_MODEL_URL = "get_parent_reply_basis";
    public static final String ALGORITHM_URL_PREFIX_TWO = env("AICO_ALGORITHM_URL_PREFIX_TWO", "http://localhost:22500/");
    public static final String GET_DECISION_TREE_END = "all_instruction";
    public static final String SAVE_DECISION_TREE_END = "save_tree";

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
