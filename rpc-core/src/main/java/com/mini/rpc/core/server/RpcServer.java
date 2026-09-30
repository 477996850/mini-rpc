package com.mini.rpc.core.server;

import com.mini.rpc.common.exception.RpcException;
import com.mini.rpc.core.codec.RpcDecoder;
import com.mini.rpc.core.codec.RpcEncoder;
import com.mini.rpc.core.registry.ServiceRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RPC 服务端（基于 Netty）
 * 负责启动服务、注册服务、处理请求
 */
@Slf4j
public class RpcServer {
    private final int port;
    private final ServiceRegistry registry;
    /**
     * 注册到注册中心的服务地址（多网卡环境可显式指定，避免注册出连不上的虚拟网卡 IP），
     * 为空时自动探测本机 IP
     */
    private final String serverHost;
    private final Map<String, Object> serviceMap = new ConcurrentHashMap<>();

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    /**
     * 业务线程组：Handler 中的反射调用投递到这里执行，
     * 避免慢业务阻塞 IO 线程、拖垮同一 EventLoop 上的其他连接
     */
    private EventExecutorGroup businessGroup;

    public RpcServer(int port, ServiceRegistry registry) {
        this(port, registry, null);
    }

    public RpcServer(int port, ServiceRegistry registry, String serverHost) {
        this.port = port;
        this.registry = registry;
        this.serverHost = serverHost;
    }

    /**
     * 注册服务
     *
     * @param interfaceClass 服务接口
     * @param serviceImpl 服务实现对象
     */
    public <T> void registerService(Class<T> interfaceClass, T serviceImpl) {
        String serviceName = interfaceClass.getName();
        serviceMap.put(serviceName, serviceImpl);
        log.info("注册服务: {}", serviceName);
    }

    /**
     * 启动服务器
     */
    public void start() {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        businessGroup = new DefaultEventExecutorGroup(16);

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline()
                                    .addLast(new RpcDecoder())
                                    .addLast(new RpcEncoder())
                                    // 业务处理器运行在独立线程组，与 IO 线程隔离
                                    .addLast(businessGroup, new RpcServerHandler(serviceMap));
                        }
                    });

            // 绑定端口
            ChannelFuture future = bootstrap.bind(port).sync();
            log.info("RPC 服务端启动成功，监听端口: {}", port);

            // 注册服务到注册中心（多网卡环境优先使用显式指定的 serverHost）
            String host = (serverHost != null && !serverHost.isEmpty())
                    ? serverHost
                    : InetAddress.getLocalHost().getHostAddress();
            String localAddress = host + ":" + port;
            for (String serviceName : serviceMap.keySet()) {
                registry.register(serviceName, localAddress);
            }

            // 等待服务器关闭
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            log.error("RPC 服务端启动失败", e);
            // 不吞异常：端口占用等启动失败要让调用方感知，而非静默退出
            throw new RpcException("RPC 服务端启动失败", e);
        } finally {
            shutdown();
        }
    }

    /**
     * 关闭服务器
     */
    public void shutdown() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (businessGroup != null) {
            businessGroup.shutdownGracefully();
        }
        if (registry != null) {
            registry.close();
        }
        log.info("RPC 服务端已关闭");
    }
}
