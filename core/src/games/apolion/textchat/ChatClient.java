package games.apolion.textchat;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

public class ChatClient implements Runnable{
    public String username = "";
    private final int port;
    private final String host;
    private boolean isDirty = false;
    private String inputString="";
    private boolean done=false;


    public ChatClient(String host, int port){
        this.host=host;
        this.port=port;
    }

    public void run(){
        EventLoopGroup group = new NioEventLoopGroup();
        try{
            Bootstrap bootstrap = new Bootstrap()
                    .group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChatClientInitializer());
            ChannelFuture cf = bootstrap.connect(host,port).sync();
            while(!done){
                Channel channel = cf.sync().channel();
                if(isDirty){
                    System.out.println("Sending:"+inputString);
                    channel.writeAndFlush("["+username+"]:"+inputString);
                    inputString = "";
                    isDirty=false;
                }
            }
            cf.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
    }

    public synchronized void setInputString(String inputString) {
        this.inputString = inputString;
        isDirty=true;
    }

    public synchronized String getInputString() {
        return inputString;
    }
}
