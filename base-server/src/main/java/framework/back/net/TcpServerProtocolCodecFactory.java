package framework.back.net;

import org.apache.mina.filter.codec.demux.DemuxingProtocolCodecFactory;
import org.springframework.stereotype.Service;

@Service("myCodecFactory")
public class TcpServerProtocolCodecFactory extends DemuxingProtocolCodecFactory {

	public TcpServerProtocolCodecFactory() {
		super.addMessageDecoder(TcpServerDecoder.class);
		super.addMessageEncoder(TcpServerEncoder.class, new TcpServerEncoder());
	}
}
