package com.project.smart_intervention.message;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.project.smart_intervention.entity.pojo.ExpertsAIMessage;
import org.springframework.stereotype.Service;

/**
 * @ClassName: ExpertsAIMessageService
 * @Description:
 * @Date: 2025/4/14
 * @Version: 1.0
 */
@Service
public class ExpertsAIMessageService
        extends ServiceImpl<ExpertsAIMessageMapper, ExpertsAIMessage>
        implements IService<ExpertsAIMessage> {
}
