package com.mini.rpc.core.client;

import com.mini.rpc.common.exception.RpcException;
import com.mini.rpc.common.protocol.RpcRequest;
import com.mini.rpc.common.protocol.RpcResponse;
import com.mini.rpc.core.loadbalance.LoadBalancer;
import com.mini.rpc.core.registry.ServiceRegistry;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;

/**
 * RPC 客户端代理工厂
 * 使用 JDK 动态代理生成远程服务代理对象
 */
@Slf4j
public class RpcClientProxy implements InvocationHandler {
    private final ServiceRegistry registry;
    private final RpcClient rpcClient;
    private final LoadBalancer loadBalancer;

    public RpcClientProxy(ServiceRegistry registry, RpcClient rpcClient, LoadBalancer loadBalancer) {
        this.registry = registry;
        this.rpcClient = rpcClient;
        this.loadBalancer = loadBalancer;
    }

    /**
     * 创建代理对象
     */
    @SuppressWarnings("unchecked")
    public <T> T getProxy(Class<T> interfaceClass) {
        return (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                this
        );
    }

    /**
     * 代理方法调用
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 排除 Object 方法
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
        }

        // 构造请求
        RpcRequest request = new RpcRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setInterfaceName(method.getDeclaringClass().getName());
        request.setMethodName(method.getName());
        request.setParameterTypes(method.getParameterTypes());
        request.setParameters(args);

        log.info("发起 RPC 调用: {}.{}", request.getInterfaceName(), request.getMethodName());

        // 服务发现
        List<String> addresses = registry.discover(request.getInterfaceName());
        if (addresses == null || addresses.isEmpty()) {
            throw new RpcException("未发现可用服务: " + request.getInterfaceName());
        }

        // 负载均衡选择服务地址
        String address = loadBalancer.select(addresses);
        log.info("选择服务地址: {}", address);

        // 发送请求
        RpcResponse response = rpcClient.sendRequest(request, address);

        // 处理响应
        if (response.getCode() != RpcResponse.SUCCESS_CODE) {
            throw new RpcException("RPC 调用失败: " + response.getMessage());
        }

        log.info("RPC 调用成功: {}", request.getRequestId());
        return response.getResult();
    }
}
