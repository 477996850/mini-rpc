package com.mini.rpc.common.serializer;

/**
 * 序列化器接口（SPI 扩展点）
 * 支持通过 Java SPI 机制动态加载不同的序列化实现
 */
public interface Serializer {
    /**
     * 序列化类型标识
     */
    byte JSON = 0;

    /**
     * 将对象序列化为字节数组
     *
     * @param obj 待序列化的对象
     * @return 字节数组
     */
    byte[] serialize(Object obj);

    /**
     * 将字节数组反序列化为对象
     *
     * @param data 字节数组
     * @param clazz 目标类型
     * @param <T> 泛型类型
     * @return 反序列化后的对象
     */
    <T> T deserialize(byte[] data, Class<T> clazz);

    /**
     * 获取序列化类型标识
     */
    byte getType();
}
