package com.project.smart_intervention.util;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.smart_intervention.entity.constant.SystemConstant;
import com.project.smart_intervention.entity.request.InstructionRequest;
import com.project.smart_intervention.entity.response.Response;
import com.project.smart_intervention.ops.TraceContext;
import jakarta.annotation.Resource;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class HttpClientUtils {

    private static final Logger log = LoggerFactory.getLogger(HttpClientUtils.class);

    @Resource
    private ObjectMapper objectMapper;

    public <T, R> Response<R> postToAI(String url, T data, Class<R> clazz) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(url);
        long start = System.currentTimeMillis();

        try {
            applyTraceHeader(httpPost);
            httpPost.setHeader("Content-Type", "application/json");
            String jsonRequest = objectMapper.writeValueAsString(data);
            httpPost.setEntity(new StringEntity(jsonRequest, "UTF-8"));
            log.info("algorithm_call_start url={}", url);
            CloseableHttpResponse response = httpClient.execute(httpPost);
            int status = response.getStatusLine() == null ? -1 : response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String result = EntityUtils.toString(entity, "UTF-8");
                JavaType type = objectMapper.getTypeFactory()
                        .constructParametricType(Response.class, clazz);
                log.info("algorithm_call_end url={} status={} latencyMs={}",
                        url, status, System.currentTimeMillis() - start);
                return objectMapper.readValue(result, type);
            }
            log.warn("algorithm_call_empty url={} status={} latencyMs={}",
                    url, status, System.currentTimeMillis() - start);
            return new Response<>();
        } catch (IOException ex) {
            log.error("algorithm_call_error url={} latencyMs={} err={}",
                    url, System.currentTimeMillis() - start, ex.toString(), ex);
            throw ex;
        } finally {
            httpClient.close();
        }
    }

    public String getDecisionTree() throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String url = SystemConstant.ALGORITHM_URL_PREFIX_TWO + SystemConstant.GET_DECISION_TREE_END;
        HttpPost httpPost = new HttpPost(url);
        long start = System.currentTimeMillis();

        try {
            applyTraceHeader(httpPost);
            httpPost.setHeader("Content-Type", "application/json");
            log.info("algorithm_call_start url={}", url);
            CloseableHttpResponse response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String body = EntityUtils.toString(entity, "UTF-8");
                log.info("algorithm_call_end url={} latencyMs={}", url, System.currentTimeMillis() - start);
                return body;
            }
            return "";
        } catch (IOException e) {
            log.error("algorithm_call_error url={} err={}", url, e.toString(), e);
            throw new RuntimeException(e);
        } finally {
            httpClient.close();
        }
    }

    public void updateDecisionTree(String tree) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String url = SystemConstant.ALGORITHM_URL_PREFIX_TWO + SystemConstant.SAVE_DECISION_TREE_END;
        HttpPost httpPost = new HttpPost(url);

        try {
            applyTraceHeader(httpPost);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setEntity(new StringEntity(tree, "UTF-8"));
            log.info("algorithm_call_start url={}", url);
            CloseableHttpResponse response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                log.info("algorithm_call_end url={}", url);
                return;
            }
        } catch (IOException e) {
            log.error("algorithm_call_error url={} err={}", url, e.toString(), e);
            throw new RuntimeException(e);
        } finally {
            httpClient.close();
        }
    }

    public void sendInstruction(InstructionRequest request) {
        log.info("algorithm_instruction_noop");
    }

    private void applyTraceHeader(HttpPost httpPost) {
        String traceId = TraceContext.current();
        if (traceId != null && !traceId.isBlank()) {
            httpPost.setHeader(TraceContext.HEADER_NAME, traceId);
        }
    }
}
