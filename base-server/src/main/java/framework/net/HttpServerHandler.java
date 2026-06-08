package framework.net;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.codec.Charsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import framework.BaseServer;
import framework.HttpServerMsg;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.ReferenceCountUtil;

/**
 * Created by Zhenglei on 2018/5/11.
 */
public class HttpServerHandler extends ChannelInboundHandlerAdapter {

    static final Logger logger = LoggerFactory.getLogger(HttpServerHandler.class);

    private static final String FAVICON_ICO = "/favicon.ico";
    static final String SUCCESS = "success";
    private static final String ERROR = "error";
    public static final String CONNECTION_KEEP_ALIVE = "keep-alive";
    public static final String CONNECTION_CLOSE = "close";

    private BaseServer m_baseServer;

    public HttpServerHandler(BaseServer m_baseServer) {
        super();
        this.m_baseServer = m_baseServer;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(msg instanceof HttpRequest){
            HttpRequest request = null;
            try{
                request = (HttpRequest) msg;
                HttpHeaders headers = request.headers();

                String url = request.uri();
                //去除浏览器"/favicon.ico"的干扰
                if(url.equals(FAVICON_ICO)){
                    return;
                }

                HttpMethod method = request.method();
                if(method.equals(HttpMethod.GET)){
//                    QueryStringDecoder queryDecoder = new QueryStringDecoder(uri, Charsets.toCharset(CharEncoding.UTF_8));
//                    Map<String, List<String>> uriAttributes = queryDecoder.parameters();
//                    //此处仅打印请求参数（你可以根据业务需求自定义处理）
//                    for (Map.Entry<String, List<String>> attr : uriAttributes.entrySet()) {
//                        for (String attrVal : attr.getValue()) {
//                            System.out.println(attr.getKey() + "=" + attrVal);
//                        }
//                    }
                }else if(method.equals(HttpMethod.POST)){

                    //POST请求，由于你需要从消息体中获取数据，因此有必要把msg转换成FullHttpRequest
                    FullHttpRequest fullRequest = (FullHttpRequest) msg;

                    //根据不同的 Content_Type 处理 body 数据
                    dealWithContentType(fullRequest,headers,url,ctx.channel());
                }else{
                    //其他类型在此不做处理，需要的话可自己扩展
                }

                //writeResponse(ctx.channel(),request, HttpResponseStatus.OK, SUCCESS, false);
            }catch(Exception e){
                writeResponse(ctx.channel(),request, HttpResponseStatus.INTERNAL_SERVER_ERROR, ERROR, true);

            }finally{
                ReferenceCountUtil.release(msg);
            }

        }else{
            //discard request...
            ReferenceCountUtil.release(msg);
        }
    }


    /**
     * 简单处理常用几种 Content-Type 的 POST 内容（可自行扩展）
     * @throws Exception
     */
    private void dealWithContentType(FullHttpRequest fullRequest,HttpHeaders headers,String url,Channel channel) throws Exception{
        String contentType = getContentType(headers);
        if(contentType.startsWith("application/json")){  //可以使用HttpJsonDecoder
            String jsonStr = fullRequest.content().toString(Charsets.toCharset(CharEncoding.UTF_8));
            HttpServerMsg httpServerMsg = new HttpServerMsg(url,jsonStr,channel,fullRequest);
            m_baseServer.send(httpServerMsg);
        }else if(contentType.startsWith("application/x-www-form-urlencoded")){
            //to do

        }else if(contentType.startsWith("multipart/form-data")){  //用于文件上传
            //to do
        }else{
            //do nothing...
        }
    }

    private void writeResponse(Channel channel,HttpRequest request, HttpResponseStatus status, String msg, boolean forceClose){
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

        boolean close = isClose(request);
        if(!close && !forceClose){
            response.content().writeBytes(msg.getBytes());
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
            response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        }
        ChannelFuture future = channel.writeAndFlush(response);
        if(close || forceClose){
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    private String getContentType(HttpHeaders headers){
        String typeStr = headers.get("Content-Type").toString();
        String[] list = typeStr.split(";");
        return list[0];
    }

    private boolean isClose(HttpRequest request){
        if(request.headers().contains(org.apache.http.HttpHeaders.CONNECTION, CONNECTION_CLOSE, true) ||
                (request.protocolVersion().equals(HttpVersion.HTTP_1_0) &&
                        !request.headers().contains(org.apache.http.HttpHeaders.CONNECTION, CONNECTION_KEEP_ALIVE, true)))
            return true;
        return false;
    }

}
