package com.mini.rpc.core.loadbalance;

import java.util.List;
import java.util.Random;

/**
 * 随机负载均衡
 */
public class RandomLoadBalancer implements LoadBalancer {
    private final Random random = new Random();

    @Override
    public String select(List<String> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            return null;
        }
        return addresses.get(random.nextInt(addresses.size()));
    }
}
