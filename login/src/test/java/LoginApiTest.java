import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
public class LoginApiTest {
    public static void main(String[] args) throws Exception {
        // 登录服地址，根据你的实际端口改
        String url = "http://192.168.0.69:8811/app/login";
        // 根据 LoginParamsDTO 的字段拼参数名，注意大小写要和 DTO 一致
        String params = "userName=huang1"
                + "&password=huang1"
                + "&sdkType=164"
                + "&deviceId=e98eb795cc97099f682e9b34059655a1"
                + "&version=1.1.2"
                + "&verify="
                + "&phone="
                + "&realIp=123.53.38.87";
        byte[] postData = params.getBytes(StandardCharsets.UTF_8);
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
        conn.setRequestProperty("Content-Length", String.valueOf(postData.length));
        try (OutputStream os = conn.getOutputStream()) {
            os.write(postData);
        }
        int code = conn.getResponseCode();
        System.out.println("HTTP 状态码: " + code);
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            System.out.println("登录返回: " + sb);
        }
    }
}