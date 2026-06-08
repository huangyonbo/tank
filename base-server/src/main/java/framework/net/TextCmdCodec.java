/**   
*    
* 描述：   
* 文件：TextCmdCodec.java
* 创建人：胡中伟
* 创建时间：2018年5月14日 下午3:58:43 
*    
*/
package framework.net;

import org.apache.commons.codec.binary.StringUtils;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.filter.codec.demux.MessageDecoder;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;
import org.apache.mina.filter.codec.demux.MessageEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**   
*    
* 描述：   
*    
*/
public class TextCmdCodec implements MessageEncoder<String>, MessageDecoder {

	static final Logger logger = LoggerFactory.getLogger(TextCmdCodec.class);


	/**
	 * @param session
	 * @param message
	 * @param out
	 * @throws Exception
	 */
	@Override
	public void encode(IoSession session, String message, ProtocolEncoderOutput out) throws Exception {
		
		IoBuffer buffer = IoBuffer.allocate(StringUtils.getBytesUtf8(message).length);
		buffer.put(StringUtils.getBytesUtf8(message));
		buffer.flip();
		
		out.write(buffer);
	}
	
	/**
	 * @param session
	 * @param in
	 * @return
	 */
	@Override
	public MessageDecoderResult decodable(IoSession session, IoBuffer in) {
		int len = in.remaining();
		byte[] buff = new byte[len];
		in.get(buff);
		String str = StringUtils.newStringUtf8(buff);

		if(str.endsWith("\n"))
		{
			return MessageDecoderResult.OK;
		}
		return MessageDecoderResult.NEED_DATA;
	}

	/**
	 * @param session
	 * @param in
	 * @param out
	 * @return
	 * @throws Exception
	 */
	@Override
	public MessageDecoderResult decode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
		int len = in.remaining();
		byte[] buff = new byte[len];
		in.get(buff);
		String str = StringUtils.newStringUtf8(buff);
		if(str.endsWith("\n"))
		{
			out.write(str);
			return MessageDecoderResult.OK;
		}
		return MessageDecoderResult.NEED_DATA;
	}

	/**
	 * @param session
	 * @param out
	 * @throws Exception
	 */
	@Override
	public void finishDecode(IoSession session, ProtocolDecoderOutput out) throws Exception {
	}
}
