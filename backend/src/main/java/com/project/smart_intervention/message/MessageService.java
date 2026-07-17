package com.project.smart_intervention.message;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.project.smart_intervention.entity.pojo.Message;
import org.springframework.stereotype.Service;

/**
 * @ClassName: MessageService
 * @Description:
 * @Date: 2025/4/7
 * @Version: 1.0
 */
@Service
public class MessageService extends ServiceImpl<MessageMapper, Message> implements IService<Message> {
}
