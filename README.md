# mini-rpc

> 一句话定位：一个使用 Netty + ZooKeeper 实现的轻量级 RPC 框架，用于学习 RPC 原理（网络通信、服务注册与发现、动态代理、负载均衡、序列化扩展）。

> ⚠️ 本项目为**个人学习项目**，仅供学习交流参考，未经充分生产验证，请勿直接用于生产环境。

## 技术栈

| 层面 | 技术 |
|---|---|
| 语言 / 构建 | Java 17、Maven 多模块 |
| 网络通信 | Netty 4.1.108（NIO、长连接、TCP_NODELAY / SO_KEEPALIVE） |
| 注册中心 | ZooKeeper 3.9.2 + Apache Curator 5.6.0 |
| 序列化 | Jackson 2.17.2（JSON），SPI 机制支持扩展其他序列化方式 |
| 日志 / 工具 | SLF4J 2.0.13 + Logback 1.5.6、Lombok 1.18.32 |

## 核心特性

- **透明化远程调用**：基于 JDK 动态代理（`RpcClientProxy`），客户端像调用本地方法一样调用远程服务
- **自定义协议与编解码器**：`RpcMessage` 消息抽象 + `RpcEncoder` / `RpcDecoder` 处理粘包拆包
- **服务注册与发现**：服务端启动时将服务注册到 ZooKeeper `/mini-rpc` 节点；客户端基于 Curator `PathChildrenCache` 监听节点变化，动态感知服务上下线
- **负载均衡**：内置随机（`RandomLoadBalancer`）与轮询（`RoundRobinLoadBalancer`）两种策略，可扩展
- **异步调用**：基于 `CompletableFuture` 管理请求-响应匹配，连接按地址缓存复用（`ConcurrentHashMap`）
- **SPI 扩展点**：序列化器通过 Java SPI（`META-INF/services`）加载，默认 JSON，可替换实现

## 项目结构

```
mini-rpc
├── rpc-common       # 通用模块：协议（RpcMessage/RpcRequest/RpcResponse）、
│                    #   序列化接口（Serializer）、异常（RpcException）
├── rpc-core         # 框架核心：
│   ├── client/      #   RpcClient（Netty 连接管理）、RpcClientHandler、RpcClientProxy（动态代理）
│   ├── server/      #   RpcServer（Netty 服务端）、RpcServerHandler（反射调用真实服务）
│   ├── codec/       #   RpcEncoder / RpcDecoder
│   ├── registry/    #   ServiceRegistry 接口、ZookeeperRegistry（Curator 实现）
│   ├── loadbalance/ #   LoadBalancer 接口 + 随机 / 轮询实现
│   └── serializer/  #   JsonSerializer、SerializerFactory（SPI 加载）
├── rpc-server       # 示例服务端：HelloService 实现 + 启动类
└── rpc-client       # 示例客户端：接口引用 + 启动类
```

## 快速开始

1. **环境**：JDK 17+、Maven 3.8+、ZooKeeper 3.9.x（本机 2181 端口）
2. **构建**：项目根目录执行 `mvn clean package -DskipTests`
3. **改地址**：如 ZooKeeper 不在本机，修改 `ServerApplication` / `ClientApplication` 中的 `ZK_ADDRESS` 常量
4. **启动服务端**：运行 `rpc-server` 模块的 `com.mini.rpc.server.ServerApplication`
5. **启动客户端**：运行 `rpc-client` 模块的 `com.mini.rpc.client.ClientApplication`，控制台可见远程调用返回结果

## License

[MIT](LICENSE)
