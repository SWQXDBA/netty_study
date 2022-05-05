package com.example.nettystudyserver;


import com.example.nettystudyserver.Handler.EchoServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Service;



@Service
public class NettyService {
    @Value("${netty.service.port}")
    private int port;
    public void run() {
        //用于接收连接 实际上只会使用其中的一个线程
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        //用于处理channel的io
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        //用于处理耗时操作 如果在workerGroup中执行耗时操作，可能会有以下问题
        //workerGroup中的一个EventLoop可能会负责多个channel的io 如果其中一个在处理过程中卡住了 会影响其他channel的io
        EventLoopGroup defaultEventLoop = new DefaultEventLoop();
        try{
            //启动器 负责组装netty组件 启动服务器/客户端
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup,workerGroup)
                    //选择 channel实现 有bio(oio) nio kqueue(mac的实现) epoll等
                    .channel(NioServerSocketChannel.class)
                    //worker(child)负责处理读写 决定了要执行哪些操作
                    .childHandler(
                            //本身也是一个handler 负责添加别的handler
                            new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {

                            ch.pipeline().addLast(new StringDecoder())
                                    //观察是否传入defaultEventLoop时候的处理线程是哪一个
                                    .addLast(defaultEventLoop,new EchoServerHandler());
                        }
                    })
                    //好像是设置channel的属性
                    .option(ChannelOption.SO_BACKLOG,128)
                    .childOption(ChannelOption.SO_KEEPALIVE,true);
            ChannelFuture future = bootstrap.bind(port)
                    //阻塞 直到连接建立
                    .sync();
            //等待直到server socket关闭
            future.channel().closeFuture().sync();
        }catch (InterruptedException e){
            e.printStackTrace();
        }
        finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}
