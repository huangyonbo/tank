package framework.back.net;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.demux.MessageDecoder;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TcpServerDecoder implements MessageDecoder {
	private static final Logger logger = LoggerFactory.getLogger(TcpServerDecoder.class.getName());

	@Override
	public void finishDecode(IoSession arg0, ProtocolDecoderOutput arg1) throws Exception {
	}

	@Override
	public MessageDecoderResult decodable(IoSession arg0, IoBuffer in) {
		try {
			in.mark();

			int datalen = (int) in.getUnsignedInt();

			if (datalen == 0x3C706F6C) {
				byte[] requestBytes = new byte[23];
				in.clear();
				in.get(requestBytes);

				String msg = new String(requestBytes).trim();

				if (msg.equals("<policy-file-request/>")) {
					return MessageDecoderResult.OK;
				} else {
					return MessageDecoderResult.NEED_DATA;
				}
			} else if (datalen == 0x7467775F) {
				byte[] requestBytes = new byte[62];
				in.clear();
				in.get(requestBytes);

				String msg = new String(requestBytes).trim();

				if (msg.indexOf("tgw_l7_forward") > -1) {
					return MessageDecoderResult.OK;
				} else {
					return MessageDecoderResult.NEED_DATA;
				}
			}else{
				in.reset();
				if (in.remaining() - 4 >= datalen) {
					return MessageDecoderResult.OK;
				}
				return MessageDecoderResult.NEED_DATA;
			}
		} catch (Exception e) {
			logger.error(StackTraceUtil.getStackTrace(e));
		}
		return MessageDecoderResult.OK;
	}

	@Override
	public MessageDecoderResult decode(IoSession arg0, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
		while (true) {
			in.mark();
			int datalen = 0;

			if (in.remaining() >= 4) {
				datalen = (int) in.getUnsignedInt();

				if (datalen == 0) {
					throw new Exception("data length is 0");
				}

				in.reset();
			} else if (in.remaining() == 0) {
				return MessageDecoderResult.OK;
			} else {
				
				return MessageDecoderResult.NEED_DATA;
			}

			if (datalen == 0) {
				throw new Exception("data length is 0");
			}

			if (in.remaining() - 4 >= datalen) {
				byte[] data = new byte[datalen + 4];
				in.get(data);
				IoBuffer bf = IoBuffer.wrap(data);
				out.write(bf);
			} else if (datalen == 0x3C706F6C) {
				byte[] data = new byte[23];
				in.get(data);
				IoBuffer bf = IoBuffer.wrap(data);
				out.write(bf);

				return MessageDecoderResult.OK;
			} else if (datalen == 0x7467775F) {
				byte[] data = new byte[62];
				in.get(data);
				IoBuffer bf = IoBuffer.wrap(data);
				out.write(bf);

				return MessageDecoderResult.OK;
			} else {
				return MessageDecoderResult.NEED_DATA;
			}
		}
	}
}
