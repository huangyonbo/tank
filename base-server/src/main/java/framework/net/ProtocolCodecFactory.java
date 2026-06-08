package framework.net;

import org.apache.mina.filter.codec.demux.DemuxingProtocolCodecFactory;

public class ProtocolCodecFactory extends DemuxingProtocolCodecFactory {
	public ProtocolCodecFactory() {
		super.addMessageDecoder(MsgCodec.class);
		super.addMessageEncoder(SendMessage.class, new MsgCodec());
	}
}
