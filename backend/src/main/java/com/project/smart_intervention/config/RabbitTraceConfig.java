package com.project.smart_intervention.config;

import com.project.smart_intervention.ops.TraceContext;
import jakarta.annotation.PostConstruct;
import org.aopalliance.intercept.MethodInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Propagate TraceId across RabbitMQ publish/consume so async workers share the same chain.
 */
@Configuration
public class RabbitTraceConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitTraceConfig.class);

    private final RabbitTemplate rabbitTemplate;

    public RabbitTraceConfig(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostConstruct
    public void attachPublishTrace() {
        rabbitTemplate.addBeforePublishPostProcessors(message -> {
            String traceId = TraceContext.current();
            if (traceId != null && !traceId.isBlank()) {
                message.getMessageProperties().setHeader(TraceContext.HEADER_NAME, traceId);
            }
            return message;
        });
        log.info("RabbitTemplate TraceId publish post-processor attached");
    }

    @Bean(name = "rabbitListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setAfterReceivePostProcessors(this::restoreTraceFromMessage);
        factory.setAdviceChain((MethodInterceptor) invocation -> {
            try {
                return invocation.proceed();
            } finally {
                TraceContext.clear();
            }
        });
        return factory;
    }

    private Message restoreTraceFromMessage(Message message) {
        Object header = message.getMessageProperties().getHeader(TraceContext.HEADER_NAME);
        if (header == null) {
            header = message.getMessageProperties().getHeader("traceId");
        }
        if (header != null && !header.toString().isBlank()) {
            TraceContext.set(header.toString());
            log.debug("restored TraceId from Rabbit message: {}", header);
        } else {
            TraceContext.ensure();
            log.debug("Rabbit message missing TraceId, generated {}", TraceContext.current());
        }
        return message;
    }
}
