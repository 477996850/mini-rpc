package com.mini.rpc.core.registry;

import com.mini.rpc.common.exception.RpcException;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 ZooKeeper 的服务注册中心实现
 * 使用 Curator 客户端简化 ZK 操作
 */
@Slf4j
public class ZookeeperRegistry implements ServiceRegistry {
    private static final String ROOT_PATH = "/mini-rpc";
    private static final int SESSION_TIMEOUT = 5000;
    private static final int CONNECTION_TIMEOUT = 3000;

    private final CuratorFramework zkClient;
    private final Map<String, List<String>> serviceCache = new ConcurrentHashMap<>();
    /**
     * 已创建的监听器缓存：避免每次 discover 都新建 PathChildrenCache
     * （否则监听线程与 Watcher 会不断累积泄漏）
     */
    private final Map<String, PathChildrenCache> watcherCache = new ConcurrentHashMap<>();

    public ZookeeperRegistry(String zkAddress) {
        // 创建 ZK 客户端
        this.zkClient = CuratorFrameworkFactory.builder()
                .connectString(zkAddress)
                .sessionTimeoutMs(SESSION_TIMEOUT)
                .connectionTimeoutMs(CONNECTION_TIMEOUT)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();

        this.zkClient.start();
        log.info("ZooKeeper 客户端已启动: {}", zkAddress);

        // 创建根节点
        try {
            if (zkClient.checkExists().forPath(ROOT_PATH) == null) {
                zkClient.create().creatingParentsIfNeeded().forPath(ROOT_PATH);
            }
        } catch (Exception e) {
            throw new RpcException("创建 ZooKeeper 根节点失败", e);
        }
    }

    @Override
    public void register(String serviceName, String address) {
        try {
            String servicePath = ROOT_PATH + "/" + serviceName;
            // 创建服务节点（持久节点）
            if (zkClient.checkExists().forPath(servicePath) == null) {
                zkClient.create().creatingParentsIfNeeded().forPath(servicePath);
            }

            // 创建地址节点（临时节点，客户端断开自动删除）
            String addressPath = servicePath + "/" + address;
            if (zkClient.checkExists().forPath(addressPath) == null) {
                zkClient.create()
                        .withMode(CreateMode.EPHEMERAL)
                        .forPath(addressPath);
            }

            log.info("服务注册成功: {} -> {}", serviceName, address);
        } catch (Exception e) {
            throw new RpcException("服务注册失败: " + serviceName, e);
        }
    }

    @Override
    public List<String> discover(String serviceName) {
        // 先从缓存获取
        List<String> addresses = serviceCache.get(serviceName);
        if (addresses != null && !addresses.isEmpty()) {
            return addresses;
        }

        // 从 ZK 获取并监听变化
        try {
            String servicePath = ROOT_PATH + "/" + serviceName;
            if (zkClient.checkExists().forPath(servicePath) == null) {
                return List.of();
            }

            // 获取服务地址列表
            addresses = zkClient.getChildren().forPath(servicePath);
            serviceCache.put(serviceName, addresses);

            // 监听服务节点变化（每个服务只创建一次）
            watchService(servicePath, serviceName);

            log.info("服务发现: {} -> {}", serviceName, addresses);
            return addresses;
        } catch (Exception e) {
            throw new RpcException("服务发现失败: " + serviceName, e);
        }
    }

    /**
     * 监听服务节点变化（用于动态感知服务上下线）
     * 通过 watcherCache 保证同一服务只创建一个 PathChildrenCache
     */
    private void watchService(String servicePath, String serviceName) {
        watcherCache.computeIfAbsent(serviceName, key -> {
            try {
                PathChildrenCache cache = new PathChildrenCache(zkClient, servicePath, true);
                PathChildrenCacheListener listener = (client, event) -> {
                    // 节点变化时更新缓存
                    try {
                        List<String> newAddresses = zkClient.getChildren().forPath(servicePath);
                        serviceCache.put(serviceName, newAddresses);
                        log.info("服务节点变化: {} -> {}", serviceName, newAddresses);
                    } catch (Exception e) {
                        log.error("更新服务缓存失败", e);
                    }
                };
                cache.getListenable().addListener(listener);
                cache.start();
                log.debug("已监听服务节点: {}", servicePath);
                return cache;
            } catch (Exception e) {
                log.error("监听服务节点失败", e);
                // 返回 null 不缓存失败结果，下次 discover 可重试
                return null;
            }
        });
    }

    @Override
    public void close() {
        // 先关闭监听器，再关闭 ZK 客户端
        watcherCache.values().forEach(cache -> {
            try {
                cache.close();
            } catch (Exception e) {
                log.warn("关闭服务监听器失败", e);
            }
        });
        watcherCache.clear();

        if (zkClient != null) {
            zkClient.close();
            log.info("ZooKeeper 客户端已关闭");
        }
    }
}
