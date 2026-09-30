package com.mini.rpc.core.loadbalance;

import java.util.List;

/**
 * 轮询负载均衡
 */
public class RoundRobinLoadBalancer implements LoadBalancer {
    private int index = 0;

    @Override
    public synchronized String select(List<String> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            return null;
        }
        String address = addresses.get(Math.floorMod(index, addresses.size()));
        index++;
        return address;
    }
}
