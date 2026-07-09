package com.project.smart_intervention.chat;

import cn.hutool.core.bean.BeanUtil;
import com.project.smart_intervention.entity.constant.SystemConstant;
import com.project.smart_intervention.entity.dto.ChatDTO;
import com.project.smart_intervention.entity.dto.ExpertsMessagesDTO;
import com.project.smart_intervention.entity.dto.MessageDTO;
import com.project.smart_intervention.entity.dto.ReadTimeDTO;
import com.project.smart_intervention.entity.pojo.*;
import com.project.smart_intervention.entity.request.*;
import com.project.smart_intervention.entity.response.ParentModelResponse;
import com.project.smart_intervention.entity.response.Response;
import com.project.smart_intervention.exceptions.ChatException;
import com.project.smart_intervention.exceptions.ExpertException;
import com.project.smart_intervention.exceptions.ParentException;
import com.project.smart_intervention.expert.ExpertConstant;
import com.project.smart_intervention.expert.IExpertService;
import com.project.smart_intervention.factory.ChatFactory;
import com.project.smart_intervention.factory.MessageFactory;
import com.project.smart_intervention.factory.RequestFactory;
import com.project.smart_intervention.message.ExpertsAIMessageService;
import com.project.smart_intervention.message.MessageMapper;
import com.project.smart_intervention.message.MessageService;
import com.project.smart_intervention.parent.IParentService;
import com.project.smart_intervention.parent.ParentConstant;
import com.project.smart_intervention.parent_model.ParentModel;
import com.project.smart_intervention.parent_model.ParentModelService;
import com.project.smart_intervention.util.HttpClientUtils;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * @ClassName: ChatFacadeService
 * @Description:
 * @Date: 2025/4/7
 * @Version: 1.0
 */
@Service
public class ChatFacadeService {
    @Resource
    private IParentService parentService;
    @Resource
    private IExpertService expertService;
    @Resource
    private IChatService chatService;
    @Resource
    private MessageService messageService;
    @Resource
    private MessageMapper messageMapper;
    @Resource
    private ExpertsAIMessageService expertsAIMessageService;
    @Resource
    private HttpClientUtils httpClientUtils;
    @Resource
    private RequestFactory requestFactory;
    @Resource
    private ParentModelService parentModelService;

    /**
     * 鍒涘缓浼氳瘽
     * @param request
     * @return
     */
    public ChatDTO createChat(CreateChatRequest request) {
        Long expertId = request.getExpertId();
        Long parentId = request.getParentId();
        // 1. 鏌ヨ瀹堕暱淇℃伅
        Parent parent = parentService.query().eq("id", parentId).one();
        if (parent == null) {
            throw new ParentException(ParentConstant.PARENT_NOT_EXIST_ERROR);
        }
        // 2. 鏌ヨ涓撳淇℃伅
        Expert expert = expertService.query().eq("id", expertId).one();
        if (expert == null) {
            throw new ExpertException(ExpertConstant.NOT_EXIST_ERROR);
        }
        // 3. 鏌ヨ鎴愬姛锛岃繘琛屽皝瑁?        Chat chat = ChatFactory.createChat(parentId, expertId);
        boolean isSaved = chatService.save(chat);
        if (!isSaved) {
            throw new ChatException(ChatConstant.SAVE_CHAT_ERROR);
        }
        // 4. 灏佽涓篋TO杩斿洖
        return ChatFactory.toDTO(chat);
    }

    public List<MessageDTO> listMessage(Integer chatId) {
        // 1. 鏌ヨ浼氳瘽鏄惁瀛樺湪
        Chat chat = chatService.query().eq("id", chatId).one();
        if (chat == null) {
            throw new ChatException(ChatConstant.CHAT_NOT_EXIST_ERROR);
        }
        // 1. 鏌ヨ浼氳瘽淇℃伅
        List<Message> messages = messageService.query().eq("chat_id", chatId).list();
        // 2. 濡傛灉涓虹┖锛岀洿鎺ヨ繑鍥炵┖闆嗗悎
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        // 3. 灏佽锛岃繑鍥?        return messages.stream()
                .map(MessageFactory::toMessageDTO)
                .toList();
    }

    /**
     * 淇敼浼氳瘽鐘舵€?     * @param chatId
     */
    public void putStatus(Integer chatId) {
        // 1. 鏌ヨ浼氳瘽id
        Chat chat = chatService.query().eq("id", chatId).one();
        if (chat == null) {
            throw new ChatException(ChatConstant.CHAT_NOT_EXIST_ERROR);
        }
        // 2. 鏌ヨ鐘舵€?        Integer chatState = chat.getChatState();
        // 3. 鏍规嵁鐘舵€佷笉鍚岋紝淇敼鐘舵€?        // 濡傛灉鏄疉I鎵樼鎴栬€呮槸涓撳寰呬粙鍏ワ紝鍒欎慨鏀逛负浼氳瘽杩涜涓?        if (chatState.equals(ChatConstant.AI_HOSTING_STATUS)
                || chatState.equals(ChatConstant.SPECIALIST_TO_BE_INTERVENED_STATUS)) {
            chat.setChatState(ChatConstant.LIVE_CONVERSATIONS_STATUS);
        } else if (chatState.equals(ChatConstant.LIVE_CONVERSATIONS_STATUS)) {
            chat.setChatState(ChatConstant.AI_HOSTING_STATUS);
        } else {
            throw new ChatException(ChatConstant.PUT_STATUS_ERROR);
        }
        // 4. 鏇存柊浼氳瘽鐘舵€?        boolean isUpdated = chatService.updateById(chat);
        if (!isUpdated) {
            throw new ChatException(ChatConstant.PUT_STATUS_ERROR);
        }
    }

    /**
     * 鏇存柊闃呰鏃堕棿
     * @param request
     * @return
     */
    public ReadTimeDTO updateReadTime(UpdateReadTimeRequest request, Integer chatId) {
        Long readTimestampExpert = request.getLastReadTimestampExpert();
        Long readTimestampParent = request.getLastReadTimestampParent();
        if (readTimestampExpert == null && readTimestampParent == null) {
            throw new ChatException(ChatConstant.READ_TIME_NULL_ERROR);
        }
        // 1. 鏌ヨ浼氳瘽
        Chat chat = chatService.query().eq("id", chatId).one();
        // 2. 淇敼鍊间笉涓虹┖鐨勫€?        if (readTimestampExpert != null) {
            chat.setLastReadTimestampExpert(readTimestampExpert);
        } else {
            chat.setLastReadTimestampParent(readTimestampParent);
        }
        // 3. 淇濆瓨鍒版暟鎹簱
        boolean isUpdated = chatService.updateById(chat);
        // 4. 灏佽杩斿洖
        if (!isUpdated) {
            throw new ChatException(ChatConstant.UPDATE_READ_TIME_ERROR);
        }
        return ChatFactory.toReadTimeDTO(chat);
    }

    /**
     * 鍒犻櫎浼氳瘽
     * @param chatId
     */
    @Transactional
    public void deleteChat(Integer chatId) {
        // 1. 鍒犻櫎浼氳瘽
        boolean isDeleted = chatService.removeById(chatId);
        if (!isDeleted) {
            throw new ChatException(ChatConstant.DELETE_CHAT_ERROR);
        }
        // 2. 鏍规嵁浼氳瘽鍒犻櫎浼氳瘽璁板綍
        messageMapper.deleteByChatId(chatId);
    }

    /**
     * 鑾峰彇涓撳鐨勪細璇濊褰?     * @param chatId
     * @return
     */
    public ExpertsMessagesDTO listExpertsMessages(Integer chatId) {
        // 1. 鏌ヨ浼氳瘽鏄惁瀛樺湪
        Chat chat = chatService.query().eq("id", chatId).one();
        if (chat == null) {
            throw new ChatException(ChatConstant.CHAT_NOT_EXIST_ERROR);
        }
        // 2. 鏌ヨ鏅€氱殑浼氳瘽璁板綍
        List<Message> messages = messageService.query().eq("chat_id", chatId).list();
        if (messages == null || messages.isEmpty()) {
            messages = List.of();
        }
        // 3. 鏌ヨ涓撳涓嶢I鐨勪細璇濊褰?        List<ExpertsAIMessage> expertsAIMessages = expertsAIMessageService.query().eq("chat_id", chatId).list();
        if (expertsAIMessages == null || expertsAIMessages.isEmpty()) {
            expertsAIMessages = List.of();
        }
        // 4. 灏佽杩斿洖
        return MessageFactory.createExpertsAIMessage(messages, expertsAIMessages);
    }

    /**
     * 鑾峰彇瀹堕暱寤烘ā
     *
     * @param chatId
     * @return
     */
    @SneakyThrows
    public ParentModelResponse getModel(Integer chatId) {
        // 1. 鑾峰彇鑱婂ぉ
        Chat chat = chatService.query().eq("id", chatId).one();
        // 2. 楠岀┖
        if (chat == null) {
            throw new ChatException(ChatConstant.CHAT_NOT_EXIST_ERROR);
        }
        // 3. 鐩存帴鏌ヨ
        List<ParentModel> parentModels = parentModelService.query().eq("chat_id", chatId).list();
        if (parentModels == null || parentModels.isEmpty()) {
           return new ParentModelResponse();
        }
        List<ParentModel> list = parentModels.stream()
                .sorted(Comparator.comparing(ParentModel::getLatestMessageTime))
                .toList();
        ParentModel parentModel = list.get(list.size() - 1);
        ParentModelResponse result = new ParentModelResponse();
        result.setTag(parentModel.getTag());
        result.setEventSummary(parentModel.getEventSummary());
        result.setProfile(parentModel.getProfile());
        result.setReplyStrategy(parentModel.getReplyStrategy());
        enrichParentModelResponse(result);
        return result;
    }

    private void enrichParentModelResponse(ParentModelResponse result) {
        String featureText = String.join("锛?,
                safeText(result.getProfile()),
                safeText(result.getEventSummary()),
                safeText(result.getReplyStrategy())
        );
        result.setCurrentNeed(inferCurrentNeed(featureText));
        result.setPresentingProblem(inferPresentingProblem(featureText));
        result.setEmotionState(inferEmotionState(featureText));
        result.setRiskSignals(inferRiskSignals(featureText));
        result.setObjectiveBackground(Map.of(
                "source", "april_parent_model",
                "familyInfo", "",
                "childrenInfo", "",
                "educationOrWorkInfo", ""
        ));
        result.setSubjectivePerception(Map.of(
                "personalityAndMood", safeText(result.getProfile()),
                "replyStrategy", safeText(result.getReplyStrategy()),
                "eventSummary", safeText(result.getEventSummary())
        ));
        result.setRelationshipContext("鐢?April ParentModel 鐨勪簨浠舵€荤粨鍜岀敾鍍忓瓧娈佃緟鍔╁垽鏂?);
        result.setCommunicationStyle(featureText.length() > 120 ? "detail_rich_and_context_seeking" : "concise_problem_statement");
        result.setCognitiveStyle(featureText.contains("涓轰粈涔?) || featureText.contains("鍘熷洜") ? "cause_oriented" : "solution_oriented");
        result.setAvoidancePattern(featureText.contains("鎷呭績") || featureText.contains("瀹虫€?) ? "risk_avoidant" : "not_obvious");
        result.setSensitivityPoints(List.of("avoid_blame", "protect_parent_child_relationship", "avoid_over_generalization"));
        result.setPreferredTone(featureText.contains("鐒﹁檻") || featureText.contains("鎷呭績") ? "warm_supportive_and_reassuring" : "gentle_and_clarifying");
        result.setQuestioningStrategy(inferQuestioningStrategy(featureText));
        result.setAvoidanceGuidelines(List.of("涓嶈杩囨棭涓嬭瘖鏂垨褰掑洜", "涓嶈鍚﹀畾 client 鐨勬劅鍙?, "涓嶈鐩存帴鎶婇棶棰樺綊鍜庝簬瀛╁瓙鎴栧闀?));
        result.setNextBestQuestion(inferNextBestQuestion(featureText));
        result.setPermissionBoundary(Map.of(
                "usedForExpertReply", true,
                "alignmentTarget", false,
                "canConfirmExpertTree", false,
                "canModifyExpertTree", false,
                "expertFeedbackAuthority", "expert_only"
        ));
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private String inferCurrentNeed(String text) {
        if (text.contains("鎬庝箞鍔?) || text.contains("鎬庝箞")) return "闇€瑕佸叿浣撴矡閫氫笌琛屽姩绛栫暐";
        if (text.contains("鎷呭績") || text.contains("鐒﹁檻")) return "闇€瑕佹儏缁壙鎺ヤ笌椋庨櫓婢勬竻";
        if (text.contains("鍐茬獊") || text.contains("鍚?)) return "闇€瑕佸叧绯讳慨澶嶄笌鍐茬獊闄嶇骇";
        return "闇€瑕佷笓瀹惰繘涓€姝ユ緞娓呭綋鍓嶉棶棰?;
    }

    private String inferPresentingProblem(String text) {
        if (text.contains("瀛︿範") || text.contains("浣滀笟")) return "瀛︿範/浣滀笟鐩稿叧鍥版壈";
        if (text.contains("鎵嬫満") || text.contains("娓告垙")) return "鐢靛瓙浜у搧鎴栨父鎴忎娇鐢ㄥ洶鎵?;
        if (text.contains("娌熼€?) || text.contains("鍐茬獊") || text.contains("鍚?)) return "瀹跺涵娌熼€氫笌鍏崇郴鍐茬獊";
        if (text.contains("鐒﹁檻") || text.contains("鎷呭績") || text.contains("鍘嬪姏")) return "鎯呯华鍘嬪姏涓庢媴蹇?;
        return "寰呰繘涓€姝ユ緞娓呯殑闂";
    }

    private String inferEmotionState(String text) {
        if (text.contains("鐒﹁檻") || text.contains("鎷呭績") || text.contains("瀹虫€?)) return "anxious";
        if (text.contains("鐢熸皵") || text.contains("鎰ゆ€?)) return "angry";
        if (text.contains("闅捐繃") || text.contains("浣庤惤") || text.contains("鏃犲姪")) return "sad";
        return "unclear_or_neutral";
    }

    private List<String> inferRiskSignals(String text) {
        if (text.contains("鑷激") || text.contains("杞荤敓") || text.contains("浼ゅ")) return List.of("self_harm_or_harm_signal");
        if (text.contains("澶辨帶") || text.contains("鏆村姏")) return List.of("conflict_escalation");
        if (text.contains("涓嶄笂瀛?) || text.contains("閫冨")) return List.of("school_function_impairment");
        return List.of("no_explicit_high_risk_signal");
    }

    private String inferQuestioningStrategy(String text) {
        if (text.contains("鍐茬獊") || text.contains("鍚?)) return "鍏堥棶鏈€杩戜竴娆″叿浣撳啿绐佸満鏅紝鍐嶉棶鍙屾柟璇翠簡浠€涔堝拰鎯呯华鍙樺寲";
        if (text.contains("瀛︿範") || text.contains("浣滀笟")) return "鍏堥棶浣滀笟鍙戠敓鐨勫叿浣撴椂闂淬€佷换鍔￠毦搴︺€佸瀛愬弽搴斿拰瀹堕暱搴斿";
        return "鍏堟緞娓呮渶杩戜竴娆″叿浣撲簨浠躲€佸綋浜嬩汉鍙嶅簲鍜岀敤鎴锋湡寰?;
    }

    private String inferNextBestQuestion(String text) {
        if (text.contains("瀛︿範") || text.contains("浣滀笟")) return "鏈€杩戜竴娆′綔涓氬崱浣忔椂锛屽瀛愬叿浣撹浜嗕粈涔堛€佷綘褰撴椂鎬庝箞鍥炲簲锛?;
        if (text.contains("鍐茬獊") || text.contains("鍚?)) return "鏈€杩戜竴娆″啿绐佹槸浠庡摢鍙ヨ瘽寮€濮嬪崌绾х殑锛?;
        return "鑳戒笉鑳藉厛璁蹭竴涓渶杩戝彂鐢熺殑鍏蜂綋鍦烘櫙锛?;
    }

    private LocalDateTime getLastMessageTime(List<Message> messages) {
        List<Long> longList = messages.stream()
                .map(Message::getCreateTimestamp)
                .sorted(Comparator.reverseOrder())
                .toList();
        int size = longList.size();
        Long timestamp = longList.get(size - 1);
        // 鏂规硶1锛氭槑纭寚瀹氭椂鍖猴紙鎺ㄨ崘锛?        ZoneId zone = ZoneId.of("Asia/Shanghai"); // 鎴?ZoneId.systemDefault()
        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp),
                ZoneId.of("Asia/Shanghai")
        );
    }
}
