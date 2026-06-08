/**   
*    
* 描述：   
* 文件：TextCmdCodecFactory.java
* 创建人：胡中伟
* 创建时间：2018年5月14日 下午3:57:32 
*    
*/
package framework.net;

import org.apache.mina.filter.codec.demux.DemuxingProtocolCodecFactory;

/**
 * 
 * 描述：
 * 
 */
public class TextCmdCodecFactory extends DemuxingProtocolCodecFactory {
	public TextCmdCodecFactory() {
		super.addMessageDecoder(TextCmdCodec.class);
		super.addMessageEncoder(String.class, new TextCmdCodec());
	}
}
