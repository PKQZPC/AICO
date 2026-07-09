package com.project.smart_intervention.util;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.project.smart_intervention.entity.pojo.Result;

/**
 * @ClassName: ParseResultUtils
 * @Description:
 * @Date: 2025/4/8
 * @Version: 1.0
 */
public class ParseResultUtils {

    public static <T> Result<T> parseResult(String json, Class<T> clazz) {
        JSONObject jsonObj = JSONUtil.parseObj(json);
        T data = JSONUtil.toBean(jsonObj.getStr("data"), clazz);

        Result<T> result = new Result<>();
        result.setData(data);
        return result;
    }
}
