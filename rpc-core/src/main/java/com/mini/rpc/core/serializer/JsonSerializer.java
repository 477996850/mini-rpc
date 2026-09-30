package com.mini.rpc.core.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini.rpc.common.exception.RpcException;
import com.mini.rpc.common.serializer.Serializer;

/**
 * JSON 序列化实现（基于 Jackson）
 * 默认序列化方案
 */
public class JsonSerializer implements Serializer {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public byte[] serialize(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsBytes(obj);
        } catch (Exception e) {
            throw new RpcException("JSON 序列化失败", e);
        }
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(data, clazz);
        } catch (Exception e) {
            throw new RpcException("JSON 反序列化失败", e);
        }
    }

    @Override
    public byte getType() {
        return Serializer.JSON;
    }
}
