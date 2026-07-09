package com.project.smart_intervention.chat;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.project.smart_intervention.entity.dto.ChatDTO;
import com.project.smart_intervention.entity.pojo.Chat;
import com.project.smart_intervention.entity.pojo.Expert;
import com.project.smart_intervention.entity.pojo.Parent;
import com.project.smart_intervention.entity.request.CreateChatRequest;
import com.project.smart_intervention.exceptions.ChatException;
import com.project.smart_intervention.exceptions.ExpertException;
import com.project.smart_intervention.exceptions.ParentException;
import com.project.smart_intervention.expert.ExpertConstant;
import com.project.smart_intervention.expert.IExpertService;
import com.project.smart_intervention.parent.IParentService;
import com.project.smart_intervention.parent.ParentConstant;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * @ClassName: ChatServiceImpl
 * @Description:
 * @Date: 2025/4/7
 * @Version: 1.0
 */
@Service
public class ChatServiceImpl extends ServiceImpl<ChatMapper, Chat> implements IChatService {

}
