package com.mini.rpc.common.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RPC 请求消息
 * 封装远程调用的所有必要信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RpcRequest {
    /**
     * 请求ID，用于匹配请求和响应
     */
    private String requestId;

    /**
     * 服务接口名（全限定类名）
     */
    private String interfaceName;

    /**
     * 方法名
     */
    private String methodName;

    /**
     * 参数类型数组
     */
    private Class<?>[] parameterTypes;

    /**
     * 参数值数组
     */
    private Object[] parameters;
}
