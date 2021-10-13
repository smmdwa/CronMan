package com.distribute.executor.handler;


import com.distribute.executor.Message.Message;
import com.distribute.executor.serial.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;

import java.util.List;

public class MessageCodecSharable extends MessageToMessageCodec<ByteBuf, Message> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Message message, List<Object> list) throws Exception {
        ByteBuf out = ctx.alloc().buffer();
        // 1. 4 字节的魔数
        out.writeBytes(new byte[]{8, 0, 9, 9});
        // 2. 1 字节 version
        out.writeByte(1);
        // 3. 1 字节 指令类型
        out.writeByte(message.getMessageType());
        // 4. 8 字节 sequenceId long
        if(message.getRequestId()!=null)
            out.writeLong(message.getRequestId());
        else
            out.writeLong(0);

//        byte[] bytes = new JavaSerializer().serialize(message);
        byte[] bytes = Serializer.Algorithm.Json.serialize(message);

        // 5. msg长度
        out.writeInt(bytes.length);

        // 6. msg内容
        out.writeBytes(bytes);
        list.add(out);
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        // int 4字节 byte 1字节
        int magicNum = byteBuf.readInt();
        byte version =byteBuf.readByte();
        byte messageType= byteBuf.readByte();
        long sequenceId =byteBuf.readLong();
        int length=byteBuf.readInt();

        byte []bytes= new byte[length];
        byteBuf.readBytes(bytes,0,length);

        Class<? extends Message> messageClass = Message.getMessageClass(messageType);
        Message msg= Serializer.Algorithm.Json.deserialize(messageClass,bytes);
        list.add(msg);
    }

}