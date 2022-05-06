package com.example.nettystudyclient;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Scanner;
import java.util.concurrent.ThreadFactory;

@Service
@Slf4j
public class NettyClient {
    @Value("${netty.client.port}")
    private int port;
    @Value("${netty.client.host}")
    private String host;
    public void run(){


        EventLoopGroup workerGroup = new NioEventLoopGroup(new ThreadFactory() {
            int i = 1;
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r,"workerGroup["+(i++)+"]");
            }
        });
        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new LoggingHandler(LogLevel.WARN));

                    //Encoder是一个OutboundHandler
                    ch.pipeline().addLast(new StringEncoder());
                    ch.pipeline().addLast(new StringDecoder()).addLast(new ChannelInboundHandlerAdapter(){
                        @Override
                        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                            System.out.println("收到了服务端的消息::"+msg);
                            super.channelRead(ctx, msg);
                        }
                    });
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
                        Scanner scanner = new Scanner(System.in);
                        Thread thread = new Thread(()->{
                            while(scanner.hasNext()){
                                final String str = scanner.nextLine();
                                log.info(str);
                                if(str.equals("q")){
                                    try {
                                        channelFuture.channel().close();
                                        channelFuture.channel().closeFuture().sync();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    log.info("channel closed");
                                    workerGroup.shutdownGracefully();
                                    break;
                                }
                                channelFuture.channel().writeAndFlush(str);
                            }
                            });
                    thread.start();

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
