package com.mini.rpc.core.codec;

import com.mini.rpc.common.protocol.RpcMessage;
import com.mini.rpc.common.protocol.RpcRequest;
import com.mini.rpc.common.protocol.RpcResponse;
import com.mini.rpc.common.serializer.Serializer;
import com.mini.rpc.core.serializer.SerializerFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

/**
 * RPC 消息编码器
 * 将 RpcMessage 编码为字节流
 *
 * 协议格式：
 * +-------+--------+----------+---------+--------+----------+
 * | Magic | Version| Serializer | Type  | Length | Body     |
 * | 4B    | 1B     | 1B       | 1B    | 4B     | Variable |
 * +-------+--------+----------+---------+--------+----------+
 */
@Slf4j
public class RpcEncoder extends MessageToByteEncoder<RpcMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, RpcMessage msg, ByteBuf out) throws Exception {
        // 1. 写入魔数（4 字节）
        out.writeInt(RpcMessage.MAGIC_NUMBER);

        // 2. 写入版本号（1 字节）
        out.writeByte(RpcMessage.VERSION);

        // 3. 写入序列化类型（1 字节）
        out.writeByte(msg.getSerializerType());

        // 4. 写入消息类型（1 字节）
        out.writeByte(msg.getMessageType());

        // 5. 序列化消息体
        Serializer serializer = SerializerFactory.getSerializer(msg.getSerializerType());
        byte[] bodyBytes = serializer.serialize(msg.getBody());

        // 6. 写入消息体长度（4 字节）
        out.writeInt(bodyBytes.length);

        // 7. 写入消息体
        out.writeBytes(bodyBytes);

        log.debug("编码消息: type={}, bodyLength={}", msg.getMessageType(), bodyBytes.length);
    }
}
