package com.mini.rpc.example;

/**
 * 示例服务接口
 * 这是客户端和服务端共同约定的接口
 */
public interface HelloService {
    /**
     * 简单问候
     */
    String sayHello(String name);

    /**
     * 加法运算
     */
    int add(int a, int b);
}
