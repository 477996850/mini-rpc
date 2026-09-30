package com.mini.rpc.common.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RPC 响应消息
 * 封装远程调用的返回结果或异常信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RpcResponse {
    /**
     * 响应状态码：成功
     */
    public static final int SUCCESS_CODE = 0;

    /**
     * 响应状态码：失败
     */
    public static final int FAIL_CODE = -1;

    /**
     * 请求ID，与请求的 requestId 对应
     */
    private String requestId;

    /**
     * 响应状态码：0-成功，非0-失败
     */
    private int code;

    /**
     * 响应消息（成功或错误描述）
     */
    private String message;

    /**
     * 返回结果（成功时有值）
     */
    private Object result;

    /**
     * 创建成功响应
     */
    public static RpcResponse success(String requestId, Object result) {
        return new RpcResponse(requestId, SUCCESS_CODE, "success", result);
    }

    /**
     * 创建失败响应
     */
    public static RpcResponse fail(String requestId, String message) {
        return new RpcResponse(requestId, FAIL_CODE, message, null);
    }
}
