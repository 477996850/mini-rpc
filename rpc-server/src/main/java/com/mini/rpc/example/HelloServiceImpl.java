package com.mini.rpc.example;

import lombok.extern.slf4j.Slf4j;

/**
 * HelloService 实现类
 */
@Slf4j
public class HelloServiceImpl implements HelloService {
    @Override
    public String sayHello(String name) {
        String result = "Hello, " + name + "!";
        log.info("调用 sayHello: {} -> {}", name, result);
        return result;
    }

    @Override
    public int add(int a, int b) {
        int result = a + b;
        log.info("调用 add: {} + {} = {}", a, b, result);
        return result;
    }
}
