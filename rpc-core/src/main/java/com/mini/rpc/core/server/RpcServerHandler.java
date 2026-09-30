package com.mini.rpc.core.server;

import com.mini.rpc.common.protocol.RpcMessage;
import com.mini.rpc.common.protocol.RpcRequest;
import com.mini.rpc.common.protocol.RpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * 服务端消息处理器
 * 接收客户端请求、反射调用本地方法、返回响应
 */
@Slf4j
public class RpcServerHandler extends SimpleChannelInboundHandler<RpcMessage> {
    private final Map<String, Object> serviceMap;

    public RpcServerHandler(Map<String, Object> serviceMap) {
        this.serviceMap = serviceMap;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcMessage msg) {
        if (msg.getMessageType() != RpcMessage.TYPE_REQUEST) {
            log.warn("收到非请求消息: {}", msg.getMessageType());
            return;
        }

        RpcRequest request = (RpcRequest) msg.getBody();
        log.info("收到 RPC 请求: {}.{}", request.getInterfaceName(), request.getMethodName());

        // 处理请求
        RpcResponse response = handleRequest(request);

        // 构造响应消息
        RpcMessage responseMsg = new RpcMessage();
        // 回填请求头中的序列化类型，与客户端保持一致，而非写死 JSON
        responseMsg.setSerializerType(msg.getSerializerType());
        responseMsg.setMessageType(RpcMessage.TYPE_RESPONSE);
        responseMsg.setBody(response);

        // 发送响应
        ctx.writeAndFlush(responseMsg);
        log.info("响应已发送: {}", response.getRequestId());
    }

    /**
     * 处理 RPC 请求（反射调用本地方法）
     */
    private RpcResponse handleRequest(RpcRequest request) {
        try {
            // 获取服务对象
            Object serviceBean = serviceMap.get(request.getInterfaceName());
            if (serviceBean == null) {
                return RpcResponse.fail(request.getRequestId(), "服务未找到: " + request.getInterfaceName());
            }

            // 反射调用方法
            Method method = serviceBean.getClass().getMethod(
                    request.getMethodName(),
                    request.getParameterTypes()
            );
            Object result = method.invoke(serviceBean, request.getParameters());

            return RpcResponse.success(request.getRequestId(), result);
        } catch (InvocationTargetException e) {
            // method.invoke 会把业务方法抛出的异常包装为 InvocationTargetException，
            // 其自身 getMessage() 为 null，必须解包取出真实异常，否则客户端只能看到 "服务调用异常: null"
            Throwable cause = e.getTargetException();
            log.error("服务方法执行异常: {}.{}", request.getInterfaceName(), request.getMethodName(), cause);
            return RpcResponse.fail(request.getRequestId(), "服务调用异常: " + cause);
        } catch (Exception e) {
            log.error("处理请求失败", e);
            return RpcResponse.fail(request.getRequestId(), "服务调用异常: " + e);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("服务端异常", cause);
        ctx.close();
    }
}
