package com.mini.rpc.core.client;

import com.mini.rpc.common.exception.RpcException;
import com.mini.rpc.common.protocol.RpcMessage;
import com.mini.rpc.common.protocol.RpcRequest;
import com.mini.rpc.common.protocol.RpcResponse;
import com.mini.rpc.common.serializer.Serializer;
import com.mini.rpc.core.codec.RpcDecoder;
import com.mini.rpc.core.codec.RpcEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * RPC 客户端（基于 Netty）
 * 负责建立连接、发送请求、接收响应
 */
@Slf4j
public class RpcClient {
    private static final long RESPONSE_TIMEOUT_SECONDS = 10;

    private final EventLoopGroup eventLoopGroup;
    private final Bootstrap bootstrap;
    private final Map<String, Channel> channelCache = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<RpcResponse>> pendingRequests = new ConcurrentHashMap<>();

    public RpcClient() {
        this.eventLoopGroup = new NioEventLoopGroup();
        this.bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast(new RpcDecoder())
                                .addLast(new RpcEncoder())
                                .addLast(new RpcClientHandler(pendingRequests));
                    }
                });
    }

    /**
     * 发送 RPC 请求（同步阻塞）
     */
    public RpcResponse sendRequest(RpcRequest request, String address) {
        try {
            // 获取或创建连接
            Channel channel = getChannel(address);

            // 构造 RPC 消息
            RpcMessage message = new RpcMessage();
            message.setSerializerType(Serializer.JSON);
            message.setMessageType(RpcMessage.TYPE_REQUEST);
            message.setBody(request);

            // 创建异步结果
            CompletableFuture<RpcResponse> future = new CompletableFuture<>();
            pendingRequests.put(request.getRequestId(), future);

            // 发送请求
            channel.writeAndFlush(message).addListener((ChannelFutureListener) channelFuture -> {
                if (channelFuture.isSuccess()) {
                    log.debug("请求发送成功: {}", request.getRequestId());
                } else {
                    future.completeExceptionally(new RpcException("请求发送失败", channelFuture.cause()));
                    pendingRequests.remove(request.getRequestId());
                }
            });

            // 等待响应（超时 10 秒）
            return future.get(RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            // 超时必须清理 pendingRequests，否则条目一直泄漏，迟到的响应也无人认领
            throw new RpcException("RPC 调用超时: " + request.getRequestId(), e);
        } catch (ExecutionException e) {
            // 断连、发送失败等场景：future 已被异常完成，解包出真实原因抛出
            Throwable cause = e.getCause();
            throw (cause instanceof RpcException)
                    ? (RpcException) cause
                    : new RpcException("RPC 调用失败", cause);
        } catch (Exception e) {
            throw new RpcException("RPC 调用失败", e);
        } finally {
            // 成功时 Handler 已移除条目，此处为幂等兜底，保证任何路径下都不泄漏
            pendingRequests.remove(request.getRequestId());
        }
    }

    /**
     * 获取或创建到指定地址的连接
     */
    private Channel getChannel(String address) throws InterruptedException {
        Channel channel = channelCache.get(address);
        if (channel != null && channel.isActive()) {
            return channel;
        }
        if (channel != null) {
            // 清理已失效的缓存连接，避免残留
            channelCache.remove(address, channel);
            log.warn("连接已失效，重新建立: {}", address);
        }

        // 解析地址
        String[] parts = address.split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);

        // 建立连接
        ChannelFuture future = bootstrap.connect(new InetSocketAddress(host, port)).sync();
        channel = future.channel();
        channelCache.put(address, channel);

        log.info("连接建立成功: {}", address);
        return channel;
    }

    /**
     * 关闭客户端
     */
    public void close() {
        channelCache.values().forEach(Channel::close);
        channelCache.clear();
        eventLoopGroup.shutdownGracefully();
        log.info("RPC 客户端已关闭");
    }
}
