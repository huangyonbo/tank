package framework.net;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.filter.codec.demux.MessageDecoder;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;
import org.apache.mina.filter.codec.demux.MessageEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 
 * 描述： 编解码器 创建人：胡中伟 创建时间：2018年3月12日 下午6:16:59 DemuxingProtocolEncoder
 */
public class MsgCodec implements MessageEncoder<SendMessage>, MessageDecoder {

	static final Logger logger = LoggerFactory.getLogger(MsgCodec.class);

	@Override
	public void encode(IoSession session, SendMessage message, ProtocolEncoderOutput out) throws Exception {
		if (message == null) {
			return;
		}
		//logger.debug("encode thread is: {}", Thread.currentThread().getName());
		//Perf.GetInstane().StartPerf("MsgCodec.encode");
		int msgDataLen = 0;
		if (message.data != null) {
			msgDataLen = message.data.length;
		}
		if ("Client".equals(session.getAttribute("Type"))) {
			AtomicInteger sendObj = (AtomicInteger)session.getAttribute("SendIndex");
			int sendIndex   = sendObj.incrementAndGet();
			IoBuffer buffer = IoBuffer.allocate(msgDataLen + 2);
			buffer.putShort(message.msgID);
			if (message.data != null) {
				buffer.put(message.data);
			}
			buffer.flip();
			byte[] send = buffer.array();
			if (session.getAttribute("VerifyCode") != null && message.msgID != ClientMsgDef.CLIENT_KEY.ordinal()) {
				short code = (short) session.getAttribute("VerifyCode");
				send = Verify.encode(sendIndex,code, send, send.length);
			}
			IoBuffer writeBuffer = IoBuffer.allocate(send.length + 8);
			writeBuffer.putInt(send.length);
			writeBuffer.putInt(sendIndex);
			writeBuffer.put(send);
			writeBuffer.flip();
			out.write(writeBuffer);
			//logger.info("Send ClientMsg msgId={} Len={}",message.msgID,writeBuffer.position());
		} else {
			IoBuffer buffer = IoBuffer.allocate(msgDataLen + 6);
			buffer.putInt(msgDataLen + 2);
			buffer.putShort(message.msgID);
			if (message.data != null) {
				buffer.put(message.data);
			}
			buffer.flip();
			out.write(buffer);
		}
		//Perf.GetInstane().OverPerf("MsgCodec.encode");
	}

	@Override
	public MessageDecoderResult decodable(IoSession session, IoBuffer in) {
		try {
			in.mark();
			int datalen = (int) in.getUnsignedInt();
			if (datalen > 128 * 1024) {
				if ("Client".equals(session.getAttribute("Type"))) {
					//这里只检查客户单session
					logger.error("datalen > 128 * 1024 {}", datalen);
					session.close(true);
					return MessageDecoderResult.NOT_OK;
				}else{
					return MessageDecoderResult.NEED_DATA;
				}
			} else {
				in.reset();
				if (in.remaining() - 4 >= datalen) {
					return MessageDecoderResult.OK;
				}
				return MessageDecoderResult.NEED_DATA;
			}
		} catch (Exception e) {
			
		}
		return MessageDecoderResult.OK;
	}

	@Override
	public MessageDecoderResult decode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
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
			} else {
				return MessageDecoderResult.NEED_DATA;
			}
		}
	}

	@Override
	public void finishDecode(IoSession session, ProtocolDecoderOutput out) throws Exception {

	}
}
