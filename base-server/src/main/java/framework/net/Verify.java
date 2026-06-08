/**
* 描述：   
* 文件：Verify.java
* 创建人：胡中伟
* 创建时间：2018年7月3日 上午10:42:42
*/
package framework.net;

import framework.SystemConfigData;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 
 * 描述：
 * 
 */
public class Verify {
	/*
	static {
		System.loadLibrary("javaVerify");
	}
	//public native static int GetVersion();
	public native static byte[] Encode(int index, short key, byte[] data, int len);
	public native static byte[] Decode(int index, short key, byte[] data, int len);
	 */

   	private static byte[] encodes;
   	private static byte[] decodes;

	public static void init(Logger logger) throws Exception{
		if (logger != null) {
			logger.info("Verify version: {}", getVersion());
		}
		String dir = SystemConfigData.getConfig("user.dir","");
		String path = dir + File.separator + "config" + File.separator + "encode.bin";
		encodes = Files.readAllBytes(Paths.get(path));
		path = dir + File.separator + "config" + File.separator + "decode.bin";
		decodes = Files.readAllBytes(Paths.get(path));
	}

	public static int getVersion(){
		return 1001;
	}

	public static byte[] encode(int index, short key, byte[] data, int len){
		int l, r;
		byte[] result = new byte[len];
		if (index % 2 == 0) {
			l = ((key >> 8) + index) & 0xFF;
			r = key & 0xFF;
		} else {
			l = (key >> 8) & 0xFF;
			r = (key + index) & 0xFF;
		}
		for (int i = 0 ; i < len; i++ ) {
			int d = data[i] < 0 ? data[i] + 256 : data[i];
			int col = (r + d & 0xFF) % 256;
			int cursor = l * 256 + col;
			result[i] = encodes[cursor];
		}
		return result;
	}

	public static byte[] decode(int index, short key, byte[] data, int len){
		int l , r;
		byte[] result = new byte[len];
		if (index % 2 == 0) {
			l = ((key >> 8) + index) & 0xFF;
			r = key & 0xFF;
		} else {
			l = (key >> 8) & 0xFF;
			r = (key + index) & 0xFF;
		}
		for (int i = 0 ; i < len; i++) {
			int d = data[i] < 0 ? data[i] + 256 : data[i];
			int col = d & 0xFF;
			int cursor = l * 256 + col;
			int v = decodes[cursor];
			v = v < 0 ? v + 256 : v;
			int _v = (v - r + 256) % 256;
			if (_v < Byte.MAX_VALUE){
				result[i] = (byte)_v;
			}else {
				result[i] = (byte)(_v-256);
			}
		}
		return result;
	}
}
