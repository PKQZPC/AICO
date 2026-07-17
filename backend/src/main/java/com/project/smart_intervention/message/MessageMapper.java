package com.project.smart_intervention.message;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.project.smart_intervention.entity.pojo.Message;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

/**
 * @InterfaceName: MessageMapper
 * @Description:
 * @Date: 2025/4/7
 * @Version: 1.0
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    @Delete("delete from message_info where chat_id = #{chatId}")
    boolean deleteByChatId(Integer chatId);
}
