package com.example.nettystudyclient;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.ReferenceCountUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class NettyClient {
    @Value("${netty.client.port}")
    private int port;
    @Value("${netty.client.host}")
    private String host;
    public void run(){


        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    //Encoder是一个OutboundHandler
                    ch.pipeline().addLast(new StringEncoder());
                }
            });
/*
            //同步调用
            // Start the client.
            ChannelFuture f = b.connect(host, port).sync();
            f.channel().writeAndFlush("hello world!");*/

            // 异步调用
            ChannelFuture f = b.connect(host, port);
            f.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    channelFuture.channel().writeAndFlush("hello world!");
                }
            });

            f.channel().closeFuture().sync();
        } catch (InterruptedException e){
            e.printStackTrace();
        }
        finally {
            workerGroup.shutdownGracefully();
        }
    }
}
