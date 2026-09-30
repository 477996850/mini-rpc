package com.mini.rpc.common.exception;

/**
 * RPC 框架自定义异常
 */
public class RpcException extends RuntimeException {
    public RpcException(String message) {
        super(message);
    }

    public RpcException(String message, Throwable cause) {
        super(message, cause);
    }
}
