package com.mini.rpc.example;

/**
 * 示例服务接口（客户端需要同样的接口定义）
 */
public interface HelloService {
    String sayHello(String name);
    int add(int a, int b);
}
