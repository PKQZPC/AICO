package com.project.smart_intervention.alignment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.project.smart_intervention.entity.pojo.PersonalAlignmentState;
import com.project.smart_intervention.entity.pojo.RelationshipAlignmentState;
import com.project.smart_intervention.entity.request.AlignmentFeedbackRequest;
import com.project.smart_intervention.entity.request.AlignmentTurnRequest;
import com.project.smart_intervention.entity.response.AlignmentTurnResponse;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Primary
@Service
public class AicoAlignmentServiceImpl implements AlignmentService {
    private final ConcurrentMap<String, PersonalAlignmentState> alignedSubjectStates = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, RelationshipAlignmentState> relationshipStates = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Map<String, Object>> expertProfiles = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Map<String, Object>> clientServiceProfiles = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final File storeFile = new File("data/aico-alignment-store.json");

    @PostConstruct
    public void loadPersistedState() {
        if (!storeFile.exists()) {
            return;
        }
        try {
            Map<String, Object> state = objectMapper.readValue(storeFile, new TypeReference<>() {});
            loadPojoMap(state, "alignedSubjectStates", alignedSubjectStates, PersonalAlignmentState.class);
            loadPojoMap(state, "relationshipStates", relationshipStates, RelationshipAlignmentState.class);
            loadRawMap(state, "expertProfiles", expertProfiles);
            loadRawMap(state, "clientServiceProfiles", clientServiceProfiles);
            if (clientServiceProfiles.isEmpty()) {
                loadRawMap(state, "clientContextProfiles", clientServiceProfiles);
            }
        } catch (IOException ignored) {
            alignedSubjectStates.clear();
            relationshipStates.clear();
            expertProfiles.clear();
            clientServiceProfiles.clear();
        }
    }

    @PreDestroy
    public void persistOnShutdown() {
        persist();
    }

    @Override
    public synchronized AlignmentTurnResponse recordTurn(AlignmentTurnRequest request) {
        String mode = normalize(request.getMode(), "personal").equalsIgnoreCase("expert") ? "expert" : "personal";
        String userId = normalize(request.getUserId(), "anonymous");
        String alignedSubjectId = resolveAlignedSubjectId(request, userId, mode);
        String alignedSubjectType = mode.equals("expert") ? "expert" : "self";
        String partnerId = resolvePartnerId(request, userId, mode);

        PersonalAlignmentState alignedState = alignedSubjectStates.computeIfAbsent(alignedSubjectId, this::newSubjectState);
        alignedState.setAlignedSubjectId(alignedSubjectId);
        alignedState.setAlignedSubjectType(alignedSubjectType);
        alignedState.setConfirmationPolicy(mode.equals("expert") ? "expert_confirmed_only" : "ai_confirmed_or_user_confirmed_with_source_visible");
        updateAlignedSubject(alignedState, request, mode);

        RelationshipAlignmentState relationship = null;
        if (mode.equals("personal") && StringUtils.hasText(partnerId)) {
            relationship = relationshipStates.computeIfAbsent(
                    relationshipKey(alignedSubjectId, partnerId),
                    key -> newRelationshipState(key, alignedSubjectId, partnerId)
            );
            updateRelationship(relationship, request);
        }

        Map<String, Object> clientContext = new LinkedHashMap<>();
        Map<String, Object> expertProfile = new LinkedHashMap<>();
        if (mode.equals("expert")) {
            clientContext = clientServiceProfiles.computeIfAbsent(userId, key -> new LinkedHashMap<>());
            updateClientContext(clientContext, request);
            expertProfile = expertProfiles.computeIfAbsent(alignedSubjectId, key -> new LinkedHashMap<>());
            updateExpertProfile(expertProfile, request, alignedSubjectId);
        }

        Map<String, Object> topicResult = mergeDynamicTopic(alignedState, request, mode);
        Map<String, Object> treeExecution = selectAndExecuteTree(alignedState, topicResult, request, mode);
        List<Map<String, Object>> rag = retrieveMultiSourceRag(alignedState, relationship, expertProfile, clientContext, request.getMessage());
        alignedState.getRagMemories().addAll(rag);
        trimList(alignedState.getRagMemories(), 30);

        AlignmentTurnResponse response = new AlignmentTurnResponse();
        response.setPersonalAlignment(alignedState);
        response.setRelationshipAlignment(relationship);
        response.setAlignmentContext(buildAlignmentContext(mode, alignedState, relationship, topicResult, treeExecution, rag));
        response.setTopicGraph(topicResult);
        response.setStrategyTree(safeMap(alignedState.getStrategyTreeState()));
        response.setStrategyTreeExecution(treeExecution);
        response.setExpertProfile(expertProfile);
        response.setClientContextProfile(clientContext);
        response.setClientServiceProfile(clientContext);
        response.setRelationshipGraph(relationship == null ? new LinkedHashMap<>() : relationshipGraphMap(relationship));
        persist();
        return response;
    }

    @Override
    public PersonalAlignmentState getPersonalState(String userId) {
        String safeUserId = normalize(userId, "anonymous");
        PersonalAlignmentState state = alignedSubjectStates.computeIfAbsent(safeUserId, this::newSubjectState);
        state.setAlignedSubjectId(safeUserId);
        return state;
    }

    @Override
    public RelationshipAlignmentState getRelationshipState(String userId, String counterpartId) {
        String safeUserId = normalize(userId, "anonymous");
        String safeCounterpartId = normalize(counterpartId, "unknown");
        String key = relationshipKey(safeUserId, safeCounterpartId);
        return relationshipStates.computeIfAbsent(key, value -> newRelationshipState(value, safeUserId, safeCounterpartId));
    }

    @Override
    public synchronized void recordFeedback(AlignmentFeedbackRequest request) {
        String mode = normalize(request.getMode(), "personal").equalsIgnoreCase("expert") ? "expert" : "personal";
        String userId = normalize(request.getUserId(), "anonymous");
        if (mode.equals("expert")) {
            String expertId = normalize(request.getSourceId(), normalize(request.getUserId(), "expert_1"));
            PersonalAlignmentState expertState = alignedSubjectStates.computeIfAbsent(expertId, this::newSubjectState);
            Map<String, Object> record = confirmationRecord("expert_confirmed", "expert", expertId, normalize(request.getContent(), "expert feedback"));
            expertState.getConfirmationRecords().add(record);
            expertState.getStrategyTreeState().put("lastExpertFeedback", record);
            appendLimited(expertState.getPreferenceSignals(), "expert:" + normalize(request.getContent(), ""), 30);
        } else {
            PersonalAlignmentState personal = getPersonalState(userId);
            Map<String, Object> evaluation = evaluatePersonalFeedback(request);
            personal.getConfirmationRecords().add(evaluation);
            appendLimited(personal.getPreferenceSignals(), evaluation.toString(), 30);
            if (StringUtils.hasText(request.getCounterpartId())) {
                RelationshipAlignmentState relationship = getRelationshipState(userId, request.getCounterpartId());
                relationship.getConfirmationRecords().add(evaluation);
                appendLimited(relationship.getCommunicationNotes(), normalize(request.getContent(), ""), 30);
            }
        }
        persist();
    }

    private void updateAlignedSubject(PersonalAlignmentState state, AlignmentTurnRequest request, String mode) {
        state.setInteractionCount(state.getInteractionCount() + 1);
        state.setLastSeenAt(LocalDateTime.now());
        if (request.getProfile() != null) {
            state.getStableProfile().putAll(request.getProfile());
        }
        Map<String, Object> subject = new LinkedHashMap<>();
        subject.put("mode", mode);
        subject.put("lastRole", request.getRole());
        subject.put("lastMessage", normalize(request.getMessage(), ""));
        subject.put("updatedAt", LocalDateTime.now().toString());
        state.getStableProfile().put("alignedSubject", subject);
        updateRelationshipEdgeDetails(state, request);
        appendLimited(state.getRecentObservations(), normalize(request.getMessage(), ""), 20);
    }

    private void updateRelationshipEdgeDetails(RelationshipAlignmentState state, AlignmentTurnRequest request) {
        Map<String, Object> profile = safeMap(request.getProfile());
        Map<String, Object> metadata = safeMap(request.getMetadata());
        String message = normalize(request.getMessage(), "");
        state.getContinuousDimensions().put("familiarity", inferDimension(message, Arrays.asList("again", "old friend", "long time", "很久", "多年"), 0.50));
        state.getContinuousDimensions().put("boundarySensitivity", inferDimension(message, Arrays.asList("money", "borrow", "privacy", "借钱", "隐私"), 0.35));

        state.getSourcePerson().put("personId", state.getUserId());
        state.getSourcePerson().put("role", "owner");
        state.getTargetPerson().put("personId", state.getCounterpartId());
        state.getTargetPerson().put("roleToOwner", firstText(profile, metadata, "relationshipType", "relationship_type", "roleToOwner"));

        Map<String, Object> edge = state.getEdgeDetails();
        edge.put("ownerId", state.getUserId());
        edge.put("sourcePersonId", state.getUserId());
        edge.put("targetPersonId", state.getCounterpartId());
        edge.put("relationshipLabel", firstNonEmpty(firstText(profile, metadata, "relationshipLabel", "relationshipType", "relationship_type"), "to_be_confirmed"));
        edge.put("relationshipSummary", combineSummary(Objects.toString(edge.get("relationshipSummary"), ""), message));
        edge.put("dimensions", state.getContinuousDimensions());
        edge.put("sensitiveTopicsBetweenUs", inferRelationshipSensitiveTopics(message));
        edge.put("interactionPatterns", inferRelationshipInteractionPatterns(message));
        edge.put("strategyImplication", inferRelationshipStrategyImplication(message));
        edge.put("confirmationStatus", edge.getOrDefault("confirmationStatus", "ai_inferred"));
        edge.put("updatedAt", LocalDateTime.now().toString());

        Map<String, Object> event = inferRelationshipEvent(message);
        if (!event.isEmpty()) {
            state.getRelationshipEvents().add(event);
            trimList(state.getRelationshipEvents(), 30);
        }
        state.getEvidenceMessages().add(Map.of(
                "message", message.length() > 240 ? message.substring(0, 240) : message,
                "conversationId", normalize(request.getConversationId(), "default"),
                "source", "relationship_patch",
                "at", LocalDateTime.now().toString()
        ));
        trimList(state.getEvidenceMessages(), 30);
        state.getActiveSubgraph().put("directEdge", edge);
        state.getActiveSubgraph().put("sourcePerson", state.getSourcePerson());
        state.getActiveSubgraph().put("targetPerson", state.getTargetPerson());
        state.getActiveSubgraph().put("retrievalPolicy", Map.of("radius", 2, "ownerPrivateGraph", true));
    }

    private void updateRelationship(RelationshipAlignmentState state, AlignmentTurnRequest request) {
        state.setInteractionCount(state.getInteractionCount() + 1);
        state.setLastSeenAt(LocalDateTime.now());
        state.setGraphRole("personal_relationship");
        state.setLlmDescription("基于连续关系维度更新：interactionCount=" + state.getInteractionCount() + "，latest=" + normalize(request.getMessage(), ""));
        state.getContinuousDimensions().put("closeness", inferDimension(request.getMessage(), Arrays.asList("熟", "亲密", "朋友"), 0.56));
        state.getContinuousDimensions().put("trust", inferDimension(request.getMessage(), Arrays.asList("信任", "帮", "支持"), 0.52));
        state.getContinuousDimensions().put("tension", inferDimension(request.getMessage(), Arrays.asList("吵", "生气", "拒绝", "冷战"), 0.22));
        appendLimited(state.getRecentObservations(), normalize(request.getMessage(), ""), 20);
    }

    private void updateClientContext(Map<String, Object> clientContext, AlignmentTurnRequest request) {
        Map<String, Object> profile = safeMap(request.getProfile());
        Map<String, Object> metadata = safeMap(request.getMetadata());
        String message = normalize(request.getMessage(), "");
        clientContext.put("profileType", "ClientServiceProfile");
        clientContext.put("alignmentTarget", false);
        clientContext.put("usedForExpertReply", true);
        clientContext.put("canModifyExpertTree", false);
        clientContext.put("clientId", normalize(request.getUserId(), "anonymous"));
        clientContext.put("lastMessage", message);
        clientContext.put("updatedAt", LocalDateTime.now().toString());
        clientContext.put("modelVersion", "aico-client-service-profile-v1");
        clientContext.put("confidence", inferClientProfileConfidence(message, profile, metadata));
        clientContext.put("profile", firstText(profile, metadata, "profile", "clientProfile", "parentProfile"));
        clientContext.put("replyStrategy", firstText(profile, metadata, "replyStrategy", "reply_strategy", "respondStrategy"));
        clientContext.put("eventSummary", firstText(profile, metadata, "eventSummary", "event_summary", "caseSummary"));
        clientContext.put("tag", firstText(profile, metadata, "tag", "caseTag", "riskTag"));
        clientContext.put("latestMessageTime", LocalDateTime.now().toString());
        clientContext.put("currentNeed", firstNonEmpty(firstText(profile, metadata, "currentNeed", "current_need"), inferCurrentNeed(message)));
        clientContext.put("presentingProblem", firstNonEmpty(firstText(profile, metadata, "presentingProblem", "currentIssue"), inferPresentingProblem(message)));
        clientContext.put("urgency", inferUrgency(message));
        clientContext.put("emotionState", inferEmotionState(message));
        clientContext.put("riskSignals", inferRiskSignals(message));
        clientContext.put("objectiveBackground", buildObjectiveBackground(profile, metadata));
        clientContext.put("subjectivePerception", buildSubjectivePerception(message, profile, metadata));
        clientContext.put("relationshipContext", firstText(profile, metadata, "relationshipContext", "familyInfo", "relationshipInfo"));
        clientContext.put("communicationStyle", firstNonEmpty(firstText(profile, metadata, "communicationStyle", "communication_style"), inferCommunicationStyle(message)));
        clientContext.put("cognitiveStyle", firstNonEmpty(firstText(profile, metadata, "cognitiveStyle", "cognitive_style"), inferCognitiveStyle(message)));
        clientContext.put("avoidancePattern", firstNonEmpty(firstText(profile, metadata, "avoidancePattern", "avoidance_pattern"), inferAvoidancePattern(message)));
        clientContext.put("sensitivityPoints", inferSensitivityPoints(message));
        clientContext.put("preferredTone", firstNonEmpty(firstText(profile, metadata, "preferredTone", "preferred_tone"), inferPreferredTone(message)));
        clientContext.put("questioningStrategy", firstNonEmpty(firstText(profile, metadata, "questioningStrategy", "questioning_strategy"), inferQuestioningStrategy(message)));
        clientContext.put("avoidanceGuidelines", inferAvoidanceGuidelines(message));
        clientContext.put("nextBestQuestion", firstNonEmpty(firstText(profile, metadata, "nextBestQuestion", "next_best_question"), inferNextBestQuestion(message)));
        incrementObjectCounter(clientContext, "interactionCount");
        appendMapList(clientContext, "caseObservations", message, 30);
        appendMapList(clientContext, "caseTimeline", Map.of("at", LocalDateTime.now().toString(), "message", message), 50);
        appendMapList(clientContext, "keyIncidents", inferKeyIncident(message), 30);
        appendMapList(clientContext, "triggerFactors", inferTriggerFactor(message), 30);
        appendMapList(clientContext, "repeatedPatterns", inferRepeatedPattern(message), 30);
        appendMapList(clientContext, "sourceMessages", Map.of("role", normalize(request.getRole(), "client"), "content", message), 80);
        appendMapList(clientContext, "historicalStrategies", clientContext.get("replyStrategy"), 30);
        appendMapList(clientContext, "expertNotes", firstText(profile, metadata, "expertNote", "expertNotes"), 30);
        appendMapList(clientContext, "relatedKnowledge", firstText(profile, metadata, "relatedKnowledge", "knowledge"), 30);
        appendMapList(clientContext, "similarCases", firstText(profile, metadata, "similarCase", "similarCases"), 30);
        clientContext.put("permissionBoundary", Map.of(
                "usedForExpertReply", true,
                "alignmentTarget", false,
                "canConfirmExpertTree", false,
                "canModifyExpertTree", false,
                "expertFeedbackAuthority", "expert_only"
        ));
    }

    private void updateExpertProfile(Map<String, Object> expertProfile, AlignmentTurnRequest request, String expertId) {
        expertProfile.put("expertId", expertId);
        expertProfile.putIfAbsent("alignmentTarget", "expert_logic_style_knowledge_structure");
        expertProfile.putIfAbsent("school", "to_be_confirmed_by_expert");
        appendMapList(expertProfile, "caseSignals", normalize(request.getMessage(), ""), 20);
        expertProfile.put("updatedAt", LocalDateTime.now().toString());
    }

    private Map<String, Object> mergeDynamicTopic(PersonalAlignmentState state, AlignmentTurnRequest request, String mode) {
        Map<String, Object> graph = state.getDynamicTopicGraph();
        Map<String, Object> topics = nestedMap(graph, "topics");
        List<Object> edges = nestedList(graph, "topicEdges");
        Map<String, Object> candidate = extractDynamicTopic(request.getMessage(), mode);
        List<Double> candidateEmbedding = embed(featureText(candidate));

        String bestId = null;
        double bestScore = 0.0;
        for (Map.Entry<String, Object> entry : topics.entrySet()) {
            Map<String, Object> topic = castMap(entry.getValue());
            double score = cosine(candidateEmbedding, castDoubleList(topic.get("embedding")));
            if (score > bestScore) {
                bestScore = score;
                bestId = entry.getKey();
            }
        }

        if (bestId != null && bestScore >= 0.84) {
            Map<String, Object> topic = castMap(topics.get(bestId));
            topic.put("observationCount", ((Number) topic.getOrDefault("observationCount", 0)).intValue() + 1);
            topic.put("lastSeenAt", LocalDateTime.now().toString());
            topic.put("status", mode.equals("expert") ? "candidate" : "ai_confirmed");
            topic.put("confirmation", confirmationRecord(topic.get("status").toString(), "ai", "aico-topic-extractor", "semantic topic reuse"));
            state.getConfirmationRecords().add(castMap(topic.get("confirmation")));
            incrementTopicCounter(state.getTopics(), topic.get("canonicalName").toString());
            return result("reuse_topic", topic, bestScore, "reuse_bound_tree");
        }

        String topicId = "topic_" + UUID.randomUUID().toString().substring(0, 8);
        candidate.put("topicId", topicId);
        candidate.put("embedding", candidateEmbedding);
        candidate.put("mode", mode);
        candidate.put("status", mode.equals("expert") ? "candidate" : "ai_confirmed");
        candidate.put("confirmation", confirmationRecord(candidate.get("status").toString(), "ai", "aico-topic-extractor", "dynamic topic extraction"));
        candidate.put("createdAt", LocalDateTime.now().toString());
        candidate.put("observationCount", 1);
        candidate.put("boundStrategyTrees", new ArrayList<>());
        topics.put(topicId, candidate);
        state.getConfirmationRecords().add(castMap(candidate.get("confirmation")));
        incrementTopicCounter(state.getTopics(), candidate.get("canonicalName").toString());
        if (bestId != null && bestScore >= 0.66) {
            edges.add(Map.of("source", bestId, "target", topicId, "relation", "semantic_related", "similarity", bestScore));
            return result("create_related_topic", candidate, bestScore, "extend_or_branch_tree");
        }
        return result("create_new_topic", candidate, bestScore, "create_candidate_tree");
    }

    private Map<String, Object> selectAndExecuteTree(PersonalAlignmentState state, Map<String, Object> topicResult, AlignmentTurnRequest request, String mode) {
        Map<String, Object> tree = state.getStrategyTreeState();
        if (tree.isEmpty() || "create_candidate_tree".equals(topicResult.get("treePolicy"))) {
            tree.put("treeId", "tree_" + UUID.randomUUID().toString().substring(0, 8));
            tree.put("mode", mode);
            tree.put("status", mode.equals("expert") ? "candidate" : "ai_confirmed");
            tree.put("confirmationAuthority", mode.equals("expert") ? "expert" : "user_and_ai");
            tree.put("currentNodeId", "orient");
            tree.put("version", 1);
            tree.put("nodes", defaultStrategyNodes(mode));
            tree.put("edges", defaultStrategyEdges());
        }
        String current = normalize(Objects.toString(tree.get("currentNodeId"), null), "orient");
        String next = chooseNextNode(current, request.getMessage());
        tree.put("currentNodeId", next);
        tree.put("lastExecutionAt", LocalDateTime.now().toString());
        Map<String, Object> execution = new LinkedHashMap<>();
        execution.put("treeId", tree.get("treeId"));
        execution.put("from", current);
        execution.put("to", next);
        execution.put("reason", transitionReason(current, next, request.getMessage()));
        execution.put("mode", mode);
        appendMapList(tree, "executionTrace", execution, 200);
        return execution;
    }

    private List<Map<String, Object>> retrieveMultiSourceRag(PersonalAlignmentState state, RelationshipAlignmentState relationship, Map<String, Object> expertProfile, Map<String, Object> clientContext, String query) {
        List<Map<String, Object>> fragments = new ArrayList<>();
        addFragment(fragments, "personal_memory", state.getRecentObservations().toString(), query);
        addFragment(fragments, "preference_memory", state.getPreferenceSignals().toString(), query);
        addFragment(fragments, "topic_graph", state.getDynamicTopicGraph().toString(), query);
        addFragment(fragments, "strategy_memory", state.getStrategyTreeState().toString(), query);
        if (relationship != null) addFragment(fragments, "relationship_memory", relationship.toString(), query);
        if (expertProfile != null) addFragment(fragments, "expert_profile", expertProfile.toString(), query);
        if (clientContext != null) addFragment(fragments, "client_service_profile", clientContext.toString(), query);
        fragments.sort((left, right) -> Double.compare((double) right.get("score"), (double) left.get("score")));
        return fragments.stream().limit(6).collect(Collectors.toList());
    }

    private void addFragment(List<Map<String, Object>> fragments, String source, String text, String query) {
        if (!StringUtils.hasText(text)) return;
        Map<String, Object> fragment = new LinkedHashMap<>();
        fragment.put("source", source);
        fragment.put("text", text.length() > 500 ? text.substring(0, 500) : text);
        fragment.put("score", cosine(embed(query), embed(text)));
        fragments.add(fragment);
    }

    private Map<String, Object> evaluatePersonalFeedback(AlignmentFeedbackRequest request) {
        List<Double> values = new ArrayList<>();
        if (request.getUserScore() != null) values.add(request.getUserScore());
        if (request.getPartnerSignal() != null) values.add(request.getPartnerSignal());
        if (request.getSystemScore() != null) values.add(request.getSystemScore());
        if (request.getLlmScore() != null) values.add(request.getLlmScore());
        double score = values.isEmpty() ? 0.0 : values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        String status = score >= 0.72 ? "ai_confirmed" : "candidate";
        return confirmationRecord(status, "ai", "aico-multi-source-feedback", "personal multi-source score=" + score);
    }

    private Map<String, Object> extractDynamicTopic(String message, String mode) {
        String text = normalize(message, "");
        List<String> tokens = Arrays.stream(text.split("[\\s，。！？,.!?]+"))
                .filter(StringUtils::hasText)
                .sorted(Comparator.comparingInt(String::length).reversed())
                .limit(4)
                .collect(Collectors.toList());
        if (tokens.isEmpty() && text.length() > 0) {
            tokens = List.of(text.substring(0, Math.min(12, text.length())));
        }
        String name = tokens.isEmpty() ? "未命名动态话题" : String.join(" / ", tokens);
        Map<String, Object> topic = new LinkedHashMap<>();
        topic.put("canonicalName", name);
        topic.put("description", text.length() > 220 ? text.substring(0, 220) : text);
        topic.put("intentSummary", inferIntent(text));
        topic.put("goalSummary", mode.equals("expert") ? "触发专家逻辑树并辅助专家回复" : "结合个人画像、关系状态和历史策略组织多轮对话");
        topic.put("confidence", tokens.isEmpty() ? 0.48 : 0.74);
        return topic;
    }

    private String inferIntent(String text) {
        if (text.contains("怎么") || text.contains("如何") || text.contains("怎么办")) return "寻求表达或行动策略";
        if (text.contains("为什么") || text.contains("原因")) return "理解原因和关系逻辑";
        if (text.contains("要不要") || text.contains("是否")) return "决策与风险权衡";
        return "组织当前多轮对话";
    }

    private String inferCurrentNeed(String text) {
        if (text.contains("怎么办") || text.contains("怎么")) return "需要具体沟通与行动策略";
        if (text.contains("担心") || text.contains("焦虑")) return "需要情绪承接与风险澄清";
        if (text.contains("冲突") || text.contains("吵")) return "需要关系修复与冲突降级";
        return "需要专家进一步澄清当前问题";
    }

    private String inferPresentingProblem(String text) {
        if (text.contains("学习") || text.contains("作业")) return "学习/作业相关困扰";
        if (text.contains("手机") || text.contains("游戏")) return "电子产品或游戏使用困扰";
        if (text.contains("沟通") || text.contains("冲突") || text.contains("吵")) return "家庭沟通与关系冲突";
        if (text.contains("焦虑") || text.contains("担心") || text.contains("压力")) return "情绪压力与担忧";
        return "待进一步澄清的问题";
    }

    private String inferUrgency(String text) {
        if (containsAny(text, "自伤", "伤害", "危险", "崩溃", "轻生")) return "high";
        if (containsAny(text, "严重", "失控", "天天", "总是", "无法")) return "medium";
        return "low";
    }

    private String inferEmotionState(String text) {
        if (containsAny(text, "焦虑", "担心", "害怕")) return "anxious";
        if (containsAny(text, "生气", "愤怒", "火大")) return "angry";
        if (containsAny(text, "难过", "低落", "无助")) return "sad";
        return "unclear_or_neutral";
    }

    private List<String> inferRiskSignals(String text) {
        List<String> risks = new ArrayList<>();
        if (containsAny(text, "自伤", "轻生", "伤害")) risks.add("self_harm_or_harm_signal");
        if (containsAny(text, "失控", "打", "暴力")) risks.add("conflict_escalation");
        if (containsAny(text, "不上学", "辍学", "逃学")) risks.add("school_function_impairment");
        if (risks.isEmpty()) risks.add("no_explicit_high_risk_signal");
        return risks;
    }

    private Map<String, Object> buildObjectiveBackground(Map<String, Object> profile, Map<String, Object> metadata) {
        Map<String, Object> background = new LinkedHashMap<>();
        background.put("familyInfo", firstText(profile, metadata, "familyInfo", "family_info"));
        background.put("childrenInfo", firstText(profile, metadata, "childrenInfo", "children_info"));
        background.put("educationOrWorkInfo", firstText(profile, metadata, "educationInfo", "workInfo", "occupation"));
        background.put("source", "profile_or_metadata");
        return background;
    }

    private Map<String, Object> buildSubjectivePerception(String message, Map<String, Object> profile, Map<String, Object> metadata) {
        Map<String, Object> perception = new LinkedHashMap<>();
        perception.put("personalityAndMood", firstNonEmpty(firstText(profile, metadata, "personalityAndMood", "profile"), inferEmotionState(message)));
        perception.put("languageFeature", inferCommunicationStyle(message));
        perception.put("topicAvoidance", inferAvoidancePattern(message));
        perception.put("llmDescription", "基于当前输入和既有 April ParentModel 字段形成的 client 服务画像，不作为专家对齐主体。");
        return perception;
    }

    private String inferCommunicationStyle(String text) {
        if (text.length() > 120) return "detail_rich_and_context_seeking";
        if (containsAny(text, "是不是", "对不对", "要不要")) return "uncertain_and_confirmation_seeking";
        return "concise_problem_statement";
    }

    private String inferCognitiveStyle(String text) {
        if (containsAny(text, "为什么", "原因", "到底")) return "cause_oriented";
        if (containsAny(text, "怎么办", "如何", "方法")) return "solution_oriented";
        return "mixed_or_unclear";
    }

    private String inferAvoidancePattern(String text) {
        if (containsAny(text, "不想说", "算了", "不知道")) return "avoidant_or_uncertain";
        if (containsAny(text, "怕", "担心")) return "risk_avoidant";
        return "not_obvious";
    }

    private List<String> inferSensitivityPoints(String text) {
        List<String> points = new ArrayList<>();
        if (containsAny(text, "责备", "批评", "骂")) points.add("avoid_blame");
        if (containsAny(text, "孩子", "亲子")) points.add("protect_parent_child_relationship");
        if (containsAny(text, "隐私", "丢脸")) points.add("protect_privacy_and_face");
        if (points.isEmpty()) points.add("avoid_over_generalization");
        return points;
    }

    private String inferPreferredTone(String text) {
        if (containsAny(text, "焦虑", "担心", "难过")) return "warm_supportive_and_reassuring";
        if (containsAny(text, "怎么办", "方法")) return "concrete_stepwise_but_not_mechanical";
        return "gentle_and_clarifying";
    }

    private String inferQuestioningStrategy(String text) {
        if (containsAny(text, "冲突", "吵", "生气")) return "先问最近一次具体冲突场景，再问双方说了什么和情绪变化";
        if (containsAny(text, "学习", "作业")) return "先问作业发生的具体时间、任务难度、孩子反应和家长应对";
        return "先澄清最近一次具体事件、当事人反应和用户期待";
    }

    private List<String> inferAvoidanceGuidelines(String text) {
        List<String> guidelines = new ArrayList<>();
        guidelines.add("不要过早下诊断或归因");
        guidelines.add("不要否定 client 的感受");
        if (containsAny(text, "孩子", "学习", "作业")) guidelines.add("不要直接把问题归咎于孩子懒或家长管教失败");
        return guidelines;
    }

    private String inferNextBestQuestion(String text) {
        if (containsAny(text, "学习", "作业")) return "最近一次作业卡住时，孩子具体说了什么、你当时怎么回应？";
        if (containsAny(text, "冲突", "吵")) return "最近一次冲突是从哪句话开始升级的？";
        return "能不能先讲一个最近发生的具体场景？";
    }

    private String inferKeyIncident(String text) {
        return text.length() > 120 ? text.substring(0, 120) : text;
    }

    private String inferTriggerFactor(String text) {
        if (containsAny(text, "作业", "学习")) return "learning_task_trigger";
        if (containsAny(text, "手机", "游戏")) return "screen_or_game_trigger";
        if (containsAny(text, "沟通", "吵")) return "communication_trigger";
        return "unclear_trigger";
    }

    private String inferRepeatedPattern(String text) {
        if (containsAny(text, "总是", "每次", "天天", "经常")) return "repeated_pattern_reported";
        return "single_or_unclear_pattern";
    }

    private double inferClientProfileConfidence(String text, Map<String, Object> profile, Map<String, Object> metadata) {
        double confidence = 0.45;
        if (StringUtils.hasText(text) && text.length() > 20) confidence += 0.15;
        if (StringUtils.hasText(firstText(profile, metadata, "profile", "clientProfile", "parentProfile"))) confidence += 0.15;
        if (StringUtils.hasText(firstText(profile, metadata, "replyStrategy", "reply_strategy", "respondStrategy"))) confidence += 0.1;
        if (StringUtils.hasText(firstText(profile, metadata, "eventSummary", "event_summary", "caseSummary"))) confidence += 0.1;
        return Math.min(0.95, confidence);
    }

    private List<Double> embed(String text) {
        double[] vector = new double[96];
        String normalized = normalize(text, "").toLowerCase();
        for (int i = 0; i < normalized.length(); i++) {
            String gram = normalized.substring(i, Math.min(i + 2, normalized.length()));
            int index = Math.abs(gram.hashCode()) % vector.length;
            vector[index] += 1.0;
        }
        double norm = Math.sqrt(Arrays.stream(vector).map(value -> value * value).sum());
        List<Double> result = new ArrayList<>();
        for (double value : vector) {
            result.add(norm == 0 ? 0.0 : value / norm);
        }
        return result;
    }

    private double cosine(List<Double> left, List<Double> right) {
        if (left.isEmpty() || right.isEmpty()) return 0.0;
        double dot = 0.0;
        double leftNorm = 0.0;
        double rightNorm = 0.0;
        for (int i = 0; i < Math.min(left.size(), right.size()); i++) {
            dot += left.get(i) * right.get(i);
            leftNorm += left.get(i) * left.get(i);
            rightNorm += right.get(i) * right.get(i);
        }
        return leftNorm == 0 || rightNorm == 0 ? 0.0 : dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private Map<String, Object> defaultStrategyNodes(String mode) {
        Map<String, Object> nodes = new LinkedHashMap<>();
        nodes.put("orient", Map.of("label", "识别话题、关系和边界", "status", mode.equals("expert") ? "candidate" : "ai_confirmed"));
        nodes.put("probe", Map.of("label", "低压力试探", "status", mode.equals("expert") ? "candidate" : "ai_confirmed"));
        nodes.put("advance", Map.of("label", "推进核心目标", "status", mode.equals("expert") ? "candidate" : "ai_confirmed"));
        nodes.put("close", Map.of("label", "修复或收束", "status", mode.equals("expert") ? "candidate" : "ai_confirmed"));
        return nodes;
    }

    private List<Map<String, Object>> defaultStrategyEdges() {
        return new ArrayList<>(List.of(
                Map.of("source", "orient", "target", "probe", "condition", "上下文初步明确"),
                Map.of("source", "probe", "target", "advance", "condition", "对方接受或信息充分"),
                Map.of("source", "probe", "target", "close", "condition", "对方回避、拒绝或风险升高"),
                Map.of("source", "advance", "target", "close", "condition", "核心目标已表达")
        ));
    }

    private String chooseNextNode(String current, String message) {
        String text = normalize(message, "");
        if (text.contains("不") || text.contains("拒绝") || text.contains("生气") || text.contains("算了")) return "close";
        if ("orient".equals(current)) return "probe";
        if ("probe".equals(current)) return "advance";
        return "close";
    }

    private String transitionReason(String current, String next, String message) {
        if ("close".equals(next) && !"advance".equals(current)) return "检测到拒绝、回避或风险信号";
        return "根据当前节点和对话反馈推进";
    }

    private String buildAlignmentContext(String mode, PersonalAlignmentState state, RelationshipAlignmentState relationship, Map<String, Object> topic, Map<String, Object> tree, List<Map<String, Object>> rag) {
        StringBuilder builder = new StringBuilder();
        builder.append("AICO mode=").append(mode)
                .append("; alignedSubject=").append(state.getAlignedSubjectType()).append(":").append(state.getAlignedSubjectId())
                .append("; interactions=").append(state.getInteractionCount())
                .append("; topic=").append(castMap(topic.get("topic")).get("canonicalName"))
                .append("; treeTransition=").append(tree.get("from")).append("->").append(tree.get("to"));
        if (relationship != null) {
            builder.append("\nrelationship=").append(relationship.getRelationshipId())
                    .append("; dimensions=").append(relationship.getContinuousDimensions())
                    .append("; description=").append(relationship.getLlmDescription());
        }
        builder.append("\nRAG sources=").append(rag.stream().map(item -> item.get("source").toString()).collect(Collectors.toList()));
        return builder.toString();
    }

    private PersonalAlignmentState newSubjectState(String userId) {
        PersonalAlignmentState state = new PersonalAlignmentState();
        state.setUserId(userId);
        state.setAlignedSubjectId(userId);
        return state;
    }

    private RelationshipAlignmentState newRelationshipState(String relationshipId, String userId, String counterpartId) {
        RelationshipAlignmentState state = new RelationshipAlignmentState();
        state.setRelationshipId(relationshipId);
        state.setUserId(userId);
        state.setCounterpartId(counterpartId);
        return state;
    }

    private String resolveAlignedSubjectId(AlignmentTurnRequest request, String userId, String mode) {
        if (StringUtils.hasText(request.getAlignedSubjectId())) return request.getAlignedSubjectId();
        if (mode.equals("expert")) return normalize(Objects.toString(safeMap(request.getMetadata()).get("expertId"), null), "expert_1");
        return userId;
    }

    private String resolvePartnerId(AlignmentTurnRequest request, String userId, String mode) {
        if (StringUtils.hasText(request.getInteractionPartnerId())) return request.getInteractionPartnerId();
        if (mode.equals("expert")) return userId;
        return normalize(request.getCounterpartId(), null);
    }

    private String relationshipKey(String userId, String counterpartId) {
        return Arrays.stream(new String[]{userId, counterpartId}).filter(Objects::nonNull).sorted().collect(Collectors.joining("__"));
    }

    private String normalize(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private void persist() {
        try {
            if (!storeFile.getParentFile().exists()) {
                storeFile.getParentFile().mkdirs();
            }
            Map<String, Object> state = new LinkedHashMap<>();
            state.put("alignedSubjectStates", alignedSubjectStates);
            state.put("relationshipStates", relationshipStates);
            state.put("expertProfiles", expertProfiles);
            state.put("clientServiceProfiles", clientServiceProfiles);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(storeFile, state);
        } catch (IOException ignored) {
        }
    }

    private <T> void loadPojoMap(Map<String, Object> state, String key, ConcurrentMap<String, T> target, Class<T> type) {
        Map<String, Object> raw = castMap(state.get(key));
        raw.forEach((id, value) -> target.put(id, objectMapper.convertValue(value, type)));
    }

    private void loadRawMap(Map<String, Object> state, String key, ConcurrentMap<String, Map<String, Object>> target) {
        Map<String, Object> raw = castMap(state.get(key));
        raw.forEach((id, value) -> target.put(id, castMap(value)));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    private List<Object> nestedList(Map<String, Object> map, String key) {
        return (List<Object>) map.computeIfAbsent(key, value -> new ArrayList<>());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> nestedMap(Map<String, Object> map, String key) {
        return (Map<String, Object>) map.computeIfAbsent(key, value -> new LinkedHashMap<>());
    }

    private List<Double> castDoubleList(Object value) {
        if (!(value instanceof List<?> list)) return new ArrayList<>();
        return list.stream().filter(Number.class::isInstance).map(item -> ((Number) item).doubleValue()).collect(Collectors.toList());
    }

    private Map<String, Object> result(String action, Map<String, Object> topic, double similarity, String treePolicy) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", action);
        result.put("topic", topic);
        result.put("similarity", similarity);
        result.put("treePolicy", treePolicy);
        return result;
    }

    private Map<String, Object> confirmationRecord(String status, String source, String sourceId, String reason) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("status", status);
        record.put("source", source);
        record.put("sourceId", sourceId);
        record.put("reason", reason);
        record.put("confirmedAt", LocalDateTime.now().toString());
        return record;
    }

    private String featureText(Map<String, Object> topic) {
        return topic.values().stream().map(String::valueOf).collect(Collectors.joining(" "));
    }

    private double inferDimension(String text, List<String> markers, double base) {
        long hits = markers.stream().filter(marker -> normalize(text, "").contains(marker)).count();
        return Math.min(1.0, base + hits * 0.16);
    }

    private boolean containsAny(String text, String... markers) {
        String safeText = normalize(text, "");
        return Arrays.stream(markers).anyMatch(safeText::contains);
    }

    private String firstText(Map<String, Object> first, Map<String, Object> second, String... keys) {
        for (String key : keys) {
            Object value = first.get(key);
            if (value == null) value = second.get(key);
            if (value != null && StringUtils.hasText(value.toString())) {
                return value.toString();
            }
        }
        return "";
    }

    private String firstNonEmpty(String first, String second) {
        return StringUtils.hasText(first) ? first : normalize(second, "");
    }

    private String combineSummary(String previous, String latest) {
        if (!StringUtils.hasText(latest)) return normalize(previous, "");
        if (!StringUtils.hasText(previous)) return latest.length() > 400 ? latest.substring(0, 400) : latest;
        String merged = previous.contains(latest) ? previous : previous + " Latest signal: " + latest;
        return merged.length() > 800 ? merged.substring(merged.length() - 800) : merged;
    }

    private List<String> inferRelationshipSensitiveTopics(String text) {
        List<String> topics = new ArrayList<>();
        if (containsAny(text, "money", "borrow", "借钱", "还钱")) topics.add("money");
        if (containsAny(text, "privacy", "secret", "隐私", "丢脸")) topics.add("privacy_or_face");
        if (topics.isEmpty()) topics.add("none_explicit");
        return topics;
    }

    private List<String> inferRelationshipInteractionPatterns(String text) {
        List<String> patterns = new ArrayList<>();
        if (containsAny(text, "slow reply", "late reply", "回得慢")) patterns.add("partner_may_reply_slowly");
        if (containsAny(text, "pressure", "push", "催", "压力")) patterns.add("avoid_high_pressure_followup");
        if (containsAny(text, "again", "contact", "主动", "联系")) patterns.add("owner_considers_initiating_contact");
        if (patterns.isEmpty()) patterns.add("not_enough_signal");
        return patterns;
    }

    private String inferRelationshipStrategyImplication(String text) {
        if (containsAny(text, "money", "borrow", "借钱")) {
            return "warm_up_then_test_receptiveness_then_leave_exit_option";
        }
        if (containsAny(text, "sorry", "apolog", "道歉")) {
            return "acknowledge_impact_before_explaining";
        }
        if (containsAny(text, "fight", "conflict", "吵", "争执")) {
            return "deescalate_before_discussing_facts";
        }
        return "use_direct_edge_and_recent_events_to_choose_tone";
    }

    private Map<String, Object> inferRelationshipEvent(String text) {
        if (!StringUtils.hasText(text)) return new LinkedHashMap<>();
        String eventType = "";
        if (containsAny(text, "money", "borrow", "借钱")) eventType = "money_request_or_concern";
        else if (containsAny(text, "sorry", "apolog", "道歉")) eventType = "apology_or_repair";
        else if (containsAny(text, "fight", "conflict", "吵", "争执")) eventType = "conflict_or_tension";
        else if (containsAny(text, "long time", "again", "很久")) eventType = "reconnection";
        if (!StringUtils.hasText(eventType)) return new LinkedHashMap<>();
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventType", eventType);
        event.put("summary", text.length() > 180 ? text.substring(0, 180) : text);
        event.put("source", "relationship_patch");
        event.put("at", LocalDateTime.now().toString());
        return event;
    }

    private Map<String, Object> relationshipGraphMap(RelationshipAlignmentState relationship) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("relationshipId", relationship.getRelationshipId());
        map.put("userId", relationship.getUserId());
        map.put("counterpartId", relationship.getCounterpartId());
        map.put("dimensions", relationship.getContinuousDimensions());
        map.put("description", relationship.getLlmDescription());
        map.put("sourcePerson", relationship.getSourcePerson());
        map.put("targetPerson", relationship.getTargetPerson());
        map.put("edgeDetails", relationship.getEdgeDetails());
        map.put("relationshipEvents", relationship.getRelationshipEvents());
        map.put("evidenceMessages", relationship.getEvidenceMessages());
        map.put("activeSubgraph", relationship.getActiveSubgraph());
        return map;
    }

    private Map<String, Object> safeMap(Map<String, Object> value) {
        return value == null ? new LinkedHashMap<>() : value;
    }

    private void appendLimited(List<String> items, String item, int limit) {
        if (!StringUtils.hasText(item)) return;
        items.add(item.length() > 240 ? item.substring(0, 240) : item);
        trimList(items, limit);
    }

    private <T> void trimList(List<T> items, int limit) {
        while (items.size() > limit) items.remove(0);
    }

    @SuppressWarnings("unchecked")
    private void appendMapList(Map<String, Object> map, String key, Object item, int limit) {
        List<Object> items = (List<Object>) map.computeIfAbsent(key, value -> new ArrayList<>());
        items.add(item);
        while (items.size() > limit) items.remove(0);
    }

    private void incrementTopicCounter(Map<String, Integer> map, String key) {
        if (StringUtils.hasText(key)) map.put(key, map.getOrDefault(key, 0) + 1);
    }

    private void incrementObjectCounter(Map<String, Object> map, String key) {
        Integer value = ((Number) map.getOrDefault(key, 0)).intValue();
        map.put(key, value + 1);
    }
}
