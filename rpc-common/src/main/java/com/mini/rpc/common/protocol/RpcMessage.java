package com.mini.rpc.common.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RPC 协议消息
 * 包含协议头和消息体
 *
 * 协议格式（11字节头 + 变长体）：
 * +-------+--------+----------+---------+--------+----------+
 * | Magic | Version| Serializer | Type  | Length | Body     |
 * | 4B    | 1B     | 1B       | 1B    | 4B     | Variable |
 * +-------+--------+----------+---------+--------+----------+
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RpcMessage {
    /**
     * 魔数，用于快速识别协议：0xCAFEBABE
     */
    public static final int MAGIC_NUMBER = 0xCAFEBABE;

    /**
     * 协议版本号
     */
    public static final byte VERSION = 1;

    /**
     * 消息类型：0-请求，1-响应
     */
    public static final byte TYPE_REQUEST = 0;
    public static final byte TYPE_RESPONSE = 1;

    /**
     * 序列化类型：0-JSON
     */
    private byte serializerType;

    /**
     * 消息类型
     */
    private byte messageType;

    /**
     * 消息体（RpcRequest 或 RpcResponse）
     */
    private Object body;
}
