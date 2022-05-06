package com.example.nettystudyserver;


import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Slf4j
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
        try {
            //启动器 负责组装netty组件 启动服务器/客户端
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    //选择 channel实现 有bio(oio) nio kqueue(mac的实现) epoll等
                    .channel(NioServerSocketChannel.class)
                    //worker(child)负责处理读写 决定了要执行哪些操作
                    .childHandler(
                            //本身也是一个handler 负责添加别的handler
                            new ChannelInitializer<SocketChannel>() {
                                @Override
                                protected void initChannel(SocketChannel ch) throws Exception {

                                    //注意执行顺序!!! 插入后应该为 head ->h1->h2->h3->h4->tail
                                    //其中h1 h2为InboundHandler,h3 h4为OutboundHandle
                                    //那么在接收到客户端的消息的时候 会h1->h2的顺序执行(不执行h3/h4)
                                    //在给客户端写回消息的时候 会以 h4->h3的顺序反过来执行(不执行h1/h2)


                                    ch.pipeline().addLast("h5",new ChannelOutboundHandlerAdapter(){
                                        @Override
                                        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                                            log.info("h5 write");
                                            super.write(ctx, msg, promise);
                                        }
                                        //如果是ctx触发 不经过后面的StringEncoder 所以说这里要加一个。
                                    }).addLast(new StringEncoder());


                                    ch.pipeline().addLast(new StringDecoder())
                                            //观察是否传入defaultEventLoop时候的处理线程是哪一个
                                            .addLast(defaultEventLoop, "h1", new ChannelInboundHandlerAdapter() {
                                                @Override
                                                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                                    log.warn("h1 read");

                                                    super.channelRead(ctx, "h1" + msg.toString());
                                                }
                                            })
                                            .addLast(defaultEventLoop, "h2", new ChannelInboundHandlerAdapter() {
                                                @Override
                                                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                                    log.warn("h2 read");
                                                    ctx.channel().writeAndFlush("channel 收到了" + msg);
                                                    //注意区别。ctx的writeAndFlush并不会把消息从tail往前传递，而是从当前位置直接往前传递，所以说后面的StringEncoder,h4 h3都不会接收到
                                                    //所以只会有"h5 write"
                                                    ctx.writeAndFlush("ctx 收到了" + msg);
                                                    super.channelRead(ctx, "h2" + msg.toString());
                                                }
                                            }).addLast("h3", new ChannelOutboundHandlerAdapter() {
                                        @Override
                                        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                                            log.warn("h3 write");
                                            super.write(ctx, msg, promise);
                                        }
                                    }).addLast("h4", new ChannelOutboundHandlerAdapter() {
                                        @Override
                                        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                                            log.warn("h4 write");
                                            super.write(ctx, msg, promise);
                                        }
                                    }).addLast(new StringEncoder());
                                }
                            })
                    //好像是设置channel的属性
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture future = bootstrap.bind(port)
                    //阻塞 直到连接建立
                    .sync();
            //等待直到server socket关闭
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}
