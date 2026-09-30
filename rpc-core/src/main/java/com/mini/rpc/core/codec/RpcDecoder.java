package com.mini.rpc.core.codec;

import com.mini.rpc.common.protocol.RpcMessage;
import com.mini.rpc.common.protocol.RpcRequest;
import com.mini.rpc.common.protocol.RpcResponse;
import com.mini.rpc.common.serializer.Serializer;
import com.mini.rpc.core.serializer.SerializerFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

/**
 * RPC 消息解码器
 * 基于 LengthFieldBasedFrameDecoder 解决 TCP 粘包/拆包问题
 *
 * 协议头共 11 字节：魔数(4B) + 版本(1B) + 序列化类型(1B) + 消息类型(1B) + 消息体长度(4B)
 *
 * 参数说明：
 * maxFrameLength: 最大帧长度 8MB
 * lengthFieldOffset: 长度域偏移量 7（魔数4 + 版本1 + 序列化1 + 类型1）
 * lengthFieldLength: 长度域字段长度 4
 * lengthAdjustment: 长度调整值 0（长度域只记录消息体长度，不含协议头）
 * initialBytesToStrip: 跳过的字节数 0（协议头在 decodeFrame 中逐字段校验）
 */
@Slf4j
public class RpcDecoder extends LengthFieldBasedFrameDecoder {
    private static final int MAX_FRAME_LENGTH = 8 * 1024 * 1024; // 8MB
    private static final int LENGTH_FIELD_OFFSET = 7; // 魔数4 + 版本1 + 序列化1 + 类型1
    private static final int LENGTH_FIELD_LENGTH = 4;

    public RpcDecoder() {
        super(MAX_FRAME_LENGTH, LENGTH_FIELD_OFFSET, LENGTH_FIELD_LENGTH, 0, 0);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        // 使用父类处理粘包/拆包
        ByteBuf frame = (ByteBuf) super.decode(ctx, in);
        if (frame == null) {
            return null;
        }

        try {
            return decodeFrame(frame);
        } finally {
            frame.release();
        }
    }

    private RpcMessage decodeFrame(ByteBuf in) {
        // 1. 读取魔数并校验
        int magic = in.readInt();
        if (magic != RpcMessage.MAGIC_NUMBER) {
            throw new CorruptedFrameException("无效的魔数: " + Integer.toHexString(magic));
        }

        // 2. 读取版本号
        byte version = in.readByte();
        if (version != RpcMessage.VERSION) {
            throw new CorruptedFrameException("不支持的协议版本: " + version);
        }

        // 3. 读取序列化类型
        byte serializerType = in.readByte();

        // 4. 读取消息类型
        byte messageType = in.readByte();

        // 5. 读取消息体长度
        int bodyLength = in.readInt();

        // 6. 读取消息体
        byte[] bodyBytes = new byte[bodyLength];
        in.readBytes(bodyBytes);

        // 7. 反序列化消息体
        Serializer serializer = SerializerFactory.getSerializer(serializerType);
        Object body;
        if (messageType == RpcMessage.TYPE_REQUEST) {
            body = serializer.deserialize(bodyBytes, RpcRequest.class);
        } else {
            body = serializer.deserialize(bodyBytes, RpcResponse.class);
        }

        log.debug("解码消息: type={}, bodyLength={}", messageType, bodyLength);

        RpcMessage message = new RpcMessage();
        message.setSerializerType(serializerType);
        message.setMessageType(messageType);
        message.setBody(body);
        return message;
    }
}
