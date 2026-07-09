package com.project.smart_intervention.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * @ClassName: RabbitConfig
 * @Description:
 * @Date: 2025/4/7
 * @Version: 1.0
 */
@Configuration
public class RabbitConfig {

    public static final String SEND_MESSAGE_QUEUE = "sendMessageQueue";
    public static final String DEAD_SEND_MESSAGE_QUEUE = "deadSendMessageQueue";
    public static final String LIST_DECISION_TREE_QUEUE = "listDecisionTreeQueue";
    public static final String UPDATE_DECISION_TREE_QUEUE = "updateDecisionTreeQueue";
    public static final String SEND_INSTRUCTION_QUEUE = "sendInstructionQueue";
    public static final String GET_RECOMMEND_MESSAGE_QUEUE = "getRecommendMessageQueue";

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public Queue listDecisionTreeQueue() {
        return QueueBuilder.durable(RabbitConfig.LIST_DECISION_TREE_QUEUE)
                .build();
    }

    @Bean
    public Queue sendMessageQueue() {
        return QueueBuilder.durable(RabbitConfig.SEND_MESSAGE_QUEUE)
                .withArgument("x-message-ttl", 60000) // 璁剧疆娑堟伅杩囨湡鏃堕棿锛屽崟浣嶆绉?                .withArgument("x-dead-letter-exchange", "") // 绌哄瓧绗︿覆琛ㄧず榛樿浜ゆ崲鏈?                .withArgument("x-dead-letter-routing-key", "deadSendMessageQueue")  // 鐩存帴璺敱鍒版淇￠槦鍒?                .build();
    }

    @Bean
    public Queue deadSendMessageQueue() {
        return QueueBuilder.durable(RabbitConfig.DEAD_SEND_MESSAGE_QUEUE)
                .build();
    }

    @Bean
    public Queue updateDecisionTreeQueue() {
        return QueueBuilder.durable(RabbitConfig.UPDATE_DECISION_TREE_QUEUE)
                .build();
    }

    @Bean
    public Queue sendInstructionQueue() {
        return QueueBuilder.durable(RabbitConfig.SEND_INSTRUCTION_QUEUE)
                .build();
    }

    @Bean
    public Queue getRecommendMessageQueue() {
        return QueueBuilder.durable(RabbitConfig.GET_RECOMMEND_MESSAGE_QUEUE)
                .build();
    }
}
