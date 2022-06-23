package games.apolion.textchat;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class ChatServer implements Runnable{
    public static Thread trd;

    public  static void StartServer(int port) {
        if(trd!=null){
            return;
        }
        trd=new Thread(new ChatServer(port));
        trd.start();
    }

    private final int port;

     public ChatServer(int port){
         this.port=port;
     }

     public void run(){
         System.out.println("Started Server.");
         EventLoopGroup bossGroup = new NioEventLoopGroup(1);
         EventLoopGroup workerGroup = new NioEventLoopGroup();
         try{
             ServerBootstrap bootstrap = new ServerBootstrap()
                     .group(bossGroup,workerGroup)
                     .channel(NioServerSocketChannel.class)
                     .childHandler(new ChatServerInitializer());
             bootstrap.bind(port).sync().channel().closeFuture().sync();
         } catch (InterruptedException e) {
             e.printStackTrace();
         } finally {
             bossGroup.shutdownGracefully();
             workerGroup.shutdownGracefully();
             try {
                 trd.join();
             } catch (InterruptedException e) {
                 e.printStackTrace();
             }
         }
         System.out.println("Exit Server");
     }
     
}
