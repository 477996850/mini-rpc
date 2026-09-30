package com.mini.rpc.core.serializer;

import com.mini.rpc.common.exception.RpcException;
import com.mini.rpc.common.serializer.Serializer;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 序列化器工厂（SPI 加载）
 * 通过 Java SPI 机制动态加载序列化实现
 */
public class SerializerFactory {
    private static final Map<Byte, Serializer> SERIALIZER_MAP = new ConcurrentHashMap<>();

    static {
        // 使用 SPI 加载所有序列化实现
        ServiceLoader<Serializer> serviceLoader = ServiceLoader.load(Serializer.class);
        for (Serializer serializer : serviceLoader) {
            SERIALIZER_MAP.put(serializer.getType(), serializer);
        }

        // 如果没有通过 SPI 加载到，注册默认的 JSON 序列化器
        if (SERIALIZER_MAP.isEmpty()) {
            Serializer jsonSerializer = new JsonSerializer();
            SERIALIZER_MAP.put(jsonSerializer.getType(), jsonSerializer);
        }
    }

    /**
     * 根据类型获取序列化器
     */
    public static Serializer getSerializer(byte type) {
        Serializer serializer = SERIALIZER_MAP.get(type);
        if (serializer == null) {
            throw new RpcException("不支持的序列化类型: " + type);
        }
        return serializer;
    }

    /**
     * 获取默认序列化器（JSON）
     */
    public static Serializer getDefaultSerializer() {
        return getSerializer(Serializer.JSON);
    }
}
