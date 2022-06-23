package games.apolion.textchat;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;

public class ChatClientHandeler extends SimpleChannelInboundHandler<String> {
    public static String output;
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        output = msg;
    }

}
