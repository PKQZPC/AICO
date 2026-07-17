package com.project.smart_intervention.entity.pojo;

/**
 * @ClassName: Result
 * @Description:
 * @Date: 2025/4/7
 * @Version: 1.0
 */
import lombok.Data;

@Data
public class Result<T> {
    private Integer code;   // 鐘舵€佺爜
    private String msg;     // 娑堟伅
    private T data;         // 杩斿洖鏁版嵁

    // 鎴愬姛鍝嶅簲锛堟棤鏁版嵁锛?
    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg("success");
        return result;
    }

    // 鎴愬姛鍝嶅簲锛堝甫鏁版嵁锛?
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg("success");
        result.setData(data);
        return result;
    }
    // 鎴愬姛鍝嶅簲锛堝甫鏁版嵁锛?
    public static <T> Result<T> success(T data, String msg) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg(msg);
        result.setData(data);
        return result;
    }

    // 澶辫触鍝嶅簲
    public static <T> Result<T> error(Integer code, String msg) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }

    // 澶辫触鍝嶅簲
    public static <T> Result<T> error(String msg) {
        Result<T> result = new Result<>();
        result.setCode(400);
        result.setMsg(msg);
        return result;
    }

    // 澶辫触鍝嶅簲锛堝甫鏁版嵁锛?
    public static <T> Result<T> error(Integer code, String msg, T data) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMsg(msg);
        result.setData(data);
        return result;
    }
}
