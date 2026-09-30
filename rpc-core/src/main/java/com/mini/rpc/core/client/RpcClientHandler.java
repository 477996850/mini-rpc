package com.mini.rpc.core.client;

import com.mini.rpc.common.exception.RpcException;
import com.mini.rpc.common.protocol.RpcMessage;
import com.mini.rpc.common.protocol.RpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 客户端消息处理器
 * 接收服务端响应并完成异步 Future
 */
@Slf4j
public class RpcClientHandler extends SimpleChannelInboundHandler<RpcMessage> {
    private final Map<String, CompletableFuture<RpcResponse>> pendingRequests;

    public RpcClientHandler(Map<String, CompletableFuture<RpcResponse>> pendingRequests) {
        this.pendingRequests = pendingRequests;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcMessage msg) {
        if (msg.getMessageType() != RpcMessage.TYPE_RESPONSE) {
            log.warn("收到非响应消息: {}", msg.getMessageType());
            return;
        }

        RpcResponse response = (RpcResponse) msg.getBody();
        CompletableFuture<RpcResponse> future = pendingRequests.remove(response.getRequestId());

        if (future != null) {
            future.complete(response);
            log.debug("响应处理完成: {}", response.getRequestId());
        } else {
            log.warn("未找到对应的请求: {}", response.getRequestId());
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        // 连接断开：批量失败所有在途请求，避免调用方干等 10 秒超时
        // 注：pendingRequests 是客户端全局表，多服务端场景下可能波及其他连接的在途请求（教学版简化处理）
        int pending = pendingRequests.size();
        if (pending > 0) {
            log.warn("连接断开: {}，失败 {} 个在途请求", ctx.channel().remoteAddress(), pending);
            pendingRequests.entrySet().removeIf(entry -> {
                entry.getValue().completeExceptionally(
                        new RpcException("连接已断开: " + ctx.channel().remoteAddress()));
                return true;
            });
        }
        ctx.fireChannelInactive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("客户端异常", cause);
        ctx.close();
    }
}
