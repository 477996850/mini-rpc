package com.mini.rpc.server;

import com.mini.rpc.core.registry.ServiceRegistry;
import com.mini.rpc.core.registry.ZookeeperRegistry;
import com.mini.rpc.core.server.RpcServer;
import com.mini.rpc.example.HelloService;
import com.mini.rpc.example.HelloServiceImpl;
import lombok.extern.slf4j.Slf4j;

/**
 * 服务端启动类
 * 演示如何启动 RPC 服务并注册到 ZooKeeper
 */
@Slf4j
public class ServerApplication {
    private static final String ZK_ADDRESS = "127.0.0.1:2181";
    private static final int SERVER_PORT = 8888;

    public static void main(String[] args) {
        log.info("========== RPC 服务端启动 ==========");

        // 1. 创建注册中心
        ServiceRegistry registry = new ZookeeperRegistry(ZK_ADDRESS);

        // 2. 创建 RPC 服务器
        RpcServer server = new RpcServer(SERVER_PORT, registry);

        // 3. 注册服务实现
        server.registerService(HelloService.class, new HelloServiceImpl());

        // 4. 注册关闭钩子（必须在 start() 之前：start() 会阻塞到服务关闭，
        //    若放在其后，进程存活期间钩子永远注册不上，Ctrl+C 无法优雅清理）
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("========== RPC 服务端关闭 ==========");
            server.shutdown();
        }));

        // 5. 启动服务器（阻塞）
        try {
            server.start();
        } catch (Exception e) {
            log.error("服务端启动失败", e);
        }
    }
}
