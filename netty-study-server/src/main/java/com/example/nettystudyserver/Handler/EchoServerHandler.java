package com.example.nettystudyserver.Handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EchoServerHandler extends ChannelInboundHandlerAdapter { // (1)

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        //默认情况下应该是一个ByteBuf 可以在前面的其他handler中添加encoder或者decoder进行编码解码
        //写入缓冲区 需要flush才能刷新 可以使用writeAndFlush
        log.info("ok "+msg);
        ctx.write("ok "+msg);
        // 会自动release消息
        ctx.flush();


        //必须要release消息 这里因为已经flush了所以不用
        // ((ByteBuf) msg).release();
        //或者
        //  ReferenceCountUtil.release(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }
}
