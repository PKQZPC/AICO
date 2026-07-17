package com.project.smart_intervention.message;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.project.smart_intervention.entity.pojo.AIRecommendMessage;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

/**
 * @InterfaceName: AIRecommendMessageMapper
 * @Description:
 * @Date: 2025/4/14
 * @Version: 1.0
 */
@Mapper
public interface AIRecommendMessageMapper extends BaseMapper<AIRecommendMessage> {

    @Delete("delete from smart_intervention.ai_recommend_message where chat_id = #{chatId}")
    void deleteByChatId(Integer chatId);
}
