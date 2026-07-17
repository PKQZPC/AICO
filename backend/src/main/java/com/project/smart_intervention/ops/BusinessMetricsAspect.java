package com.project.smart_intervention.ops;

import jakarta.annotation.Resource;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Business-level dots for critical collaboration APIs.
 */
@Aspect
@Component
public class BusinessMetricsAspect {

    @Resource
    private MetricsCollector metricsCollector;

    @Around("execution(* com.project.smart_intervention.parent.ParentController.login(..))")
    public Object parentLogin(ProceedingJoinPoint pjp) throws Throwable {
        return track(pjp, "auth.parent_login");
    }

    @Around("execution(* com.project.smart_intervention.parent.ParentController.sendMessage(..))")
    public Object parentSend(ProceedingJoinPoint pjp) throws Throwable {
        return track(pjp, "chat.parent_send_message");
    }

    @Around("execution(* com.project.smart_intervention.expert.ExpertController.sendMessage(..))")
    public Object expertSend(ProceedingJoinPoint pjp) throws Throwable {
        return track(pjp, "chat.expert_send_message");
    }

    @Around("execution(* com.project.smart_intervention.chat.ChatController.createChat(..))")
    public Object createChat(ProceedingJoinPoint pjp) throws Throwable {
        return track(pjp, "chat.create");
    }

    @Around("execution(* com.project.smart_intervention.alignment.AlignmentController.recordTurn(..))")
    public Object alignmentTurn(ProceedingJoinPoint pjp) throws Throwable {
        return track(pjp, "alignment.record_turn");
    }

    @Around("execution(* com.project.smart_intervention.alignment.AlignmentController.recordFeedback(..))")
    public Object alignmentFeedback(ProceedingJoinPoint pjp) throws Throwable {
        return track(pjp, "alignment.feedback");
    }

    @Around("execution(* com.project.smart_intervention.decision_tree.DecisionTreeController.*(..))")
    public Object decisionTree(ProceedingJoinPoint pjp) throws Throwable {
        return track(pjp, "decision_tree." + pjp.getSignature().getName());
    }

    @Around("execution(* com.project.smart_intervention.expert_ai.ExpertAIController.*(..))")
    public Object expertAi(ProceedingJoinPoint pjp) throws Throwable {
        return track(pjp, "expert_ai." + pjp.getSignature().getName());
    }

    private Object track(ProceedingJoinPoint pjp, String event) throws Throwable {
        long start = System.currentTimeMillis();
        String traceId = TraceContext.ensure();
        org.slf4j.LoggerFactory.getLogger(BusinessMetricsAspect.class)
                .info("biz_event_start event={} traceId={}", event, traceId);
        try {
            Object result = pjp.proceed();
            long cost = System.currentTimeMillis() - start;
            metricsCollector.recordBusiness(event + ".ok", event + " latency=" + cost + "ms");
            org.slf4j.LoggerFactory.getLogger(BusinessMetricsAspect.class)
                    .info("biz_event_ok event={} latencyMs={}", event, cost);
            return result;
        } catch (Throwable ex) {
            metricsCollector.recordBusiness(event + ".error", ex.getClass().getSimpleName() + ": " + ex.getMessage());
            org.slf4j.LoggerFactory.getLogger(BusinessMetricsAspect.class)
                    .error("biz_event_error event={} err={}", event, ex.toString(), ex);
            throw ex;
        }
    }
}
