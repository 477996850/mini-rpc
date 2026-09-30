package com.mini.rpc.core.registry;

import java.util.List;

/**
 * 服务注册中心接口
 * 支持服务注册、发现、监听
 */
public interface ServiceRegistry {
    /**
     * 注册服务
     *
     * @param serviceName 服务名称
     * @param address 服务地址（格式：ip:port）
     */
    void register(String serviceName, String address);

    /**
     * 发现服务
     *
     * @param serviceName 服务名称
     * @return 服务地址列表
     */
    List<String> discover(String serviceName);

    /**
     * 关闭注册中心连接
     */
    void close();
}
