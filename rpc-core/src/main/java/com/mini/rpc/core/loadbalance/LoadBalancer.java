package com.mini.rpc.core.loadbalance;

import java.util.List;

/**
 * 负载均衡策略接口
 */
public interface LoadBalancer {
    /**
     * 从服务地址列表中选择一个
     */
    String select(List<String> addresses);
}
