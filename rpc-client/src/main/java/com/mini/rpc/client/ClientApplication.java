package com.mini.rpc.client;

import com.mini.rpc.core.client.RpcClient;
import com.mini.rpc.core.client.RpcClientProxy;
import com.mini.rpc.core.loadbalance.LoadBalancer;
import com.mini.rpc.core.loadbalance.RandomLoadBalancer;
import com.mini.rpc.core.registry.ServiceRegistry;
import com.mini.rpc.core.registry.ZookeeperRegistry;
import com.mini.rpc.example.HelloService;
import lombok.extern.slf4j.Slf4j;

/**
 * 客户端启动类
 * 演示如何通过动态代理调用远程服务
 */
@Slf4j
public class ClientApplication {
    private static final String ZK_ADDRESS = "127.0.0.1:2181";

    public static void main(String[] args) {
        log.info("========== RPC 客户端启动 ==========");

        // 1. 创建注册中心
        ServiceRegistry registry = new ZookeeperRegistry(ZK_ADDRESS);

        // 2. 创建 RPC 客户端
        RpcClient rpcClient = new RpcClient();

        // 3. 创建负载均衡器（实现类为 public，可直接实例化，无需反射绕路）
        LoadBalancer loadBalancer = new RandomLoadBalancer();

        // 4. 创建代理工厂
        RpcClientProxy clientProxy = new RpcClientProxy(registry, rpcClient, loadBalancer);

        // 5. 获取远程服务代理
        HelloService helloService = clientProxy.getProxy(HelloService.class);

        try {
            // 6. 调用远程方法（像调用本地方法一样）
            log.info("========== 开始远程调用 ==========");

            String result1 = helloService.sayHello("Mini-RPC");
            log.info("调用结果: {}", result1);

            int result2 = helloService.add(10, 20);
            log.info("调用结果: {}", result2);

            String result3 = helloService.sayHello("张三");
            log.info("调用结果: {}", result3);

            log.info("========== 所有调用完成 ==========");
        } catch (Exception e) {
            log.error("远程调用失败", e);
        } finally {
            // 7. 关闭客户端
            rpcClient.close();
            registry.close();
        }
    }
}
