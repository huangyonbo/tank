package framework;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpRequest;

public class HttpServerMsg extends Message {
	public String json;
	public String url;
	public Channel channel;
	public HttpRequest request;

	public HttpServerMsg(String url, String json, Channel channel, HttpRequest request) {
		super(1, HttpServerMsg.SYS_HTTP_SERVER_MSG);
		this.json = json;
		this.url = url;
		this.channel = channel;
		this.request = request;
	}

}
