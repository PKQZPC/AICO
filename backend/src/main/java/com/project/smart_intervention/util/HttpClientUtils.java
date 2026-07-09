package com.project.smart_intervention.util;

/**
 * @ClassName: HttpClientUtils
 * @Description:
 * @Date: 2025/4/8
 * @Version: 1.0
 */
import cn.hutool.core.lang.TypeReference;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.project.smart_intervention.entity.constant.RequestConstant;
import com.project.smart_intervention.entity.constant.SystemConstant;
import com.project.smart_intervention.entity.pojo.Result;
import com.project.smart_intervention.entity.request.AIRequest;
import com.project.smart_intervention.entity.request.GetModelRequest;
import com.project.smart_intervention.entity.request.InstructionRequest;
import com.project.smart_intervention.entity.response.AIResponse;
import com.project.smart_intervention.entity.response.ParentModelResponse;
import com.project.smart_intervention.entity.response.Response;
import jakarta.annotation.Resource;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

@Component
public class HttpClientUtils {

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 鍙戦€丳OST璇锋眰鍒癆I绔?     * @param url 璇锋眰鍦板潃
     * @param data 璇锋眰鏁版嵁
     * @return
     * @param <T>
     * @param <R>
     */
    public <T, R> Response<R> postToAI(String url, T data, Class<R> clazz) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(url);

        try {
            // 璁剧疆璇锋眰澶?            httpPost.setHeader("Content-Type", "application/json");

            // 璁剧疆璇锋眰浣?            String jsonRequest = objectMapper.writeValueAsString(data);
            httpPost.setEntity(new StringEntity(jsonRequest, "UTF-8"));

            // 鎵ц璇锋眰
            CloseableHttpResponse response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                String result = EntityUtils.toString(entity, "UTF-8");
                // 鍒涘缓鍙傛暟鍖栫被鍨?                JavaType type = objectMapper.getTypeFactory()
                        .constructParametricType(Response.class, clazz);

                return objectMapper.readValue(result, type);
            }

            return new Response<>(); // 杩斿洖绌哄搷搴?        } finally {
            httpClient.close();
        }
    }

    public String getDecisionTree() throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String url = SystemConstant.ALGORITHM_URL_PREFIX_TWO + SystemConstant.GET_DECISION_TREE_END;
        HttpPost httpPost = new HttpPost(url);

        try {
            // 璁剧疆璇锋眰澶?            httpPost.setHeader("Content-Type", "application/json");

            // 鎵ц璇锋眰
            CloseableHttpResponse response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                // 鍒涘缓鍙傛暟鍖栫被鍨?                return EntityUtils.toString(entity, "UTF-8");
            }

            return ""; // 杩斿洖绌哄搷搴?        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            httpClient.close();
        }
    }

    public void updateDecisionTree(String tree) throws IOException {
        // TODO 杩欓噷闇€瑕佽皟鐢ㄧ畻娉曠鎺ュ彛
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String url = SystemConstant.ALGORITHM_URL_PREFIX_TWO + SystemConstant.SAVE_DECISION_TREE_END;
        HttpPost httpPost = new HttpPost(url);

        try {
            // 璁剧疆璇锋眰澶?            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setEntity(new StringEntity(tree, "UTF-8"));
            // 鎵ц璇锋眰
            CloseableHttpResponse response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                // 鍒涘缓鍙傛暟鍖栫被鍨?                return;
            }

            return; // 杩斿洖绌哄搷搴?        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            httpClient.close();
        }
    }


    public void sendInstruction(InstructionRequest request) {
        // TODO 杩欓噷鍙渶瑕佹妸璇锋眰鍙戦€佽繃鍘诲嵆鍙?    }

}
