package game.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class PathPointTool {

	public static void main(String[] args) throws IOException {
		String jsonString = new String(Files.readAllBytes(Paths.get("config/PathPointTable.json")),
				StandardCharsets.UTF_8);
		// System.out.println(jsonString);
		Gson gson = new Gson();
		Map<String, Object> map = gson.fromJson(jsonString, new TypeToken<Map<String, Object>>() {
		}.getType());
		String data = gson.toJson(map.get("data"));
		Map<String, Object> dataMap = gson.fromJson(data, new TypeToken<Map<String, Object>>() {
		}.getType());
		Iterator<String> it = dataMap.keySet().iterator();
		List<String> needContent = new ArrayList<String>();
		while (it.hasNext()) {
			String key = it.next();
			String path = gson.toJson(dataMap.get(key));
			Map<String, Object> needMap = gson.fromJson(path, new TypeToken<Map<String, Object>>() {
			}.getType());
			String need = needMap.get("name") + "," + needMap.get("length");
			// System.out.println(need);
			needContent.add(need);
		}
		writeXmlFile(needContent);

		/*
		 * <?xml version="1.0" ?> <config> <item Diamond="5" Id="1"
		 * Item="item_skill_frozen"/> <item Diamond="10" Id="2"
		 * Item="item_skill_speed"/> <item Diamond="2" Id="3"
		 * Item="item_skill_lock"/> </config>
		 */

	}

	public static void writeXmlFile(List<String> content) {
		File file = new File("config/res/Game/PathPointTable.xml");
		FileWriter fw = null;
		BufferedWriter writer = null;
		try {
			fw = new FileWriter(file);
			writer = new BufferedWriter(fw);
			writer.write("<?xml version=\"1.0\" ?>");
			writer.newLine();
			writer.write("<config>");
			writer.newLine();
			StringBuilder builder = new StringBuilder();
			for (String line : content) {
				String[] info = line.split(",");
				builder.append("<item Id=\"").append(info[0]).append("\"").append(" Length=\"").append(info[1])
						.append("\"/>");
				writer.write(builder.toString());
				writer.newLine();
				builder.delete(0, builder.capacity());
			}
			writer.write("</config>");
			writer.newLine();
			writer.flush();
			System.out.println("write file success");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				writer.close();
				fw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
