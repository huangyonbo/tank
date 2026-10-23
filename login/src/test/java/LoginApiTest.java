import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import javax.crypto.Cipher;

public class LoginApiTest {
    private static final String LOGIN_URL = "http://152.136.121.228:8811/app/login";
    private static final String USER_NAME = "huang1";
    private static final String PASSWORD = "huang1";
    private static final String DEVICE_ID = "e98eb795cc97099f682e9b34059655a1";

    private static final String GATE_HOST = "127.0.0.1";
    private static final int GATE_PORT = 6002;
    private static final String TEST_PRODUCT_ID = "google_gold_12";
    private static final String TEST_PURCHASE_TOKEN = "";
    private static final int SOCKET_TIMEOUT_MS = 60000;

    private static final int CLIENT_LOGIN = 0;
    private static final int CLIENT_REQUEST = 5;
    private static final int CLIENT_RESPONSE = 6;
    private static final int CLIENT_LOGIN_RES = 1;
    private static final int CLIENT_KEY = 20;

    private static final int REQ_GOOGLE_PLAY_BUY_GOLD = 10018;
    private static final int REQ_GOOGLE_PLAY_PREPARE = 10019;
    private static final int REQ_GOOGLE_PLAY_PRODUCTS = 10020;

    private static final AtomicInteger REQ_ID = new AtomicInteger(1000);
    private static final AtomicInteger SEND_INDEX = new AtomicInteger(1);
    private static final String RSA_PRIVATE_BLOB =
            "BwIAAACkAABSU0EyAAQAAAEAAQAxsBuZZ4JDIRpvc/1aXWEDPQJKqrLndSy9CZCY1zJDnTMNvNVa/52lH04nSCQhzPGZ2QEVvrJBISiOBrEFV65sZidbgyyMSxhzB+mLCoYTv7PLSwZXyeTQwPp2VfoYrelIt6tWROMvq2VyDaOp5338cAKE+jYt7AI1UNVpKOgBultnro8ncx0DZ/Ix/7riywhD4p7uaUBtzxakr8IRgAvixMLoXQ9cykYbw+7cEygRbljAJytWfO71pJqhjZYHlPNjqGJMeuE3nV9pTn0H0JnjO0gE2JzCS2Irctq2JpQGhozykdCRyK2L1JeBSRw6QWH/xehcqJvC/9xlhXv2Rn7DXyGkckUdLABbrQa+JqmDoMEyX36PPQLlDWAcFN7UDRALbvyPUP5fTvxqDlBTZ+rG+D2vXhgFg9M52LR7LT7ptzMkFbTN/diZxRxdWuvxT4xk2VSYcmHcPjVkNKsZzvqIqENFMwrlbcyAnnmW8AdnLO2fBw+5lnaLBf5VQcmHIYJvwOE0cbvyEqUnv/nwEA7kCB77O0OgMUtD8Z9lE98ctOg+GZlvmxDiVQxS50BdGeMBB2Yg/rBrcuVBp58Z0EeQIXHwRokURGLLtH7NGvKaFi4jWkGvJi9LFkgfTO1WRDTCGtmGp5A3do268JNDjyIYE74Z6dmkRbZbduM892XyyoSbZi1USS0DMGUU42j0yEo6vc5ZqiUK5TEwdYfLwmquapwXCbo4xr4+Bk9P8N8VFNvqycXD9/14QipEEi4OrU0=";

    public static void main(String[] args) throws Exception {
        LoginSession session = loginHttp();
        try (GameClient client = new GameClient(GATE_HOST, GATE_PORT)) {
            client.login(session.uid, session.token);
            client.request("products", REQ_GOOGLE_PLAY_PRODUCTS, "{}");
            client.request("prepare", REQ_GOOGLE_PLAY_PREPARE,
                    "{\"productId\":\"" + jsonEscape(TEST_PRODUCT_ID) + "\"}");

            if (TEST_PURCHASE_TOKEN.trim().isEmpty()) {
                System.out.println("skip buy: TEST_PURCHASE_TOKEN is empty; 10018 needs a real Google token.");
                return;
            }
            client.request("buy", REQ_GOOGLE_PLAY_BUY_GOLD,
                    "{\"productId\":\"" + jsonEscape(TEST_PRODUCT_ID)
                            + "\",\"purchaseToken\":\"" + jsonEscape(TEST_PURCHASE_TOKEN) + "\"}");
        }
    }

    private static LoginSession loginHttp() throws Exception {
        String params = "userName=" + USER_NAME
                + "&password=" + PASSWORD
                + "&sdkType=164"
                + "&deviceId=" + DEVICE_ID
                + "&version=1.1.2"
                + "&verify="
                + "&phone="
                + "&realIp=123.53.38.87";
        byte[] postData = params.getBytes(StandardCharsets.UTF_8);
        HttpURLConnection conn = (HttpURLConnection) new URL(LOGIN_URL).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
        conn.setRequestProperty("Content-Length", String.valueOf(postData.length));
        try (OutputStream os = conn.getOutputStream()) {
            os.write(postData);
        }
        int code = conn.getResponseCode();
        byte[] body = readAll(conn.getInputStream());
        String text = new String(body, StandardCharsets.UTF_8);
        System.out.println("HTTP status: " + code);
        System.out.println("login response: " + text);
        LoginSession session = parseLoginSession(text);
        if (session.uid <= 0 || session.token.isEmpty()) {
            throw new IllegalStateException("login response does not contain uid/token: " + text);
        }
        System.out.println("parsed login uid=" + session.uid + ", token=" + mask(session.token));
        return session;
    }

    private static final class GameClient implements AutoCloseable {
        private final Socket socket;
        private final DataInputStream in;
        private final DataOutputStream out;
        private final byte[] encodes;
        private final byte[] decodes;
        private Short verifyCode;

        private GameClient(String host, int port) throws Exception {
            socket = new Socket(host, port);
            socket.setSoTimeout(SOCKET_TIMEOUT_MS);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            encodes = readVerifyTable("tank-server/config/encode.bin");
            decodes = readVerifyTable("tank-server/config/decode.bin");
        }

        private void login(int uid, String token) throws Exception {
            byte[] login = proto(
                    fieldVarint(1, uid),
                    fieldString(3, token),
                    fieldString(5, DEVICE_ID),
                    fieldString(6, "LoginApiTest"),
                    fieldString(7, "1.1.2"));
            send(CLIENT_LOGIN, login);
            System.out.println("sent socket login uid=" + uid + ", token=" + mask(token));
            long deadline = System.currentTimeMillis() + SOCKET_TIMEOUT_MS;
            while (System.currentTimeMillis() < deadline) {
                Response resp;
                try {
                    resp = readResponse();
                } catch (SocketTimeoutException e) {
                    throw new IllegalStateException("timeout waiting socket login response. "
                            + "Check tank-server GateLogic logs, loginHost/loginPort, "
                            + "and whether clientVerify=false took effect after restart.", e);
                }
                if (resp.msgId == CLIENT_LOGIN_RES) {
                    System.out.println("login socket response hex: " + toHex(resp.data));
                    return;
                }
                System.out.println("skip before login result msgId=" + resp.msgId
                        + ", index=" + resp.index + ", len=" + resp.data.length);
            }
            throw new IllegalStateException("timeout waiting CLIENT_LOGIN_RES");
        }

        private void request(String name, int msgId, String json) throws Exception {
            int reqId = REQ_ID.incrementAndGet();
            System.out.println("request " + name + " msgId=" + msgId + ", body=" + json);
            byte[] customString = fieldString(1, json);
            byte[] request = proto(
                    fieldVarint(1, msgId),
                    fieldVarint(2, reqId),
                    fieldBytes(3, customString));
            send(CLIENT_REQUEST, request);
            while (true) {
                Response resp = readResponse();
                if (resp.msgId != CLIENT_RESPONSE) {
                    System.out.println("skip msgId=" + resp.msgId + ", len=" + resp.data.length);
                    continue;
                }
                ParsedResponse parsed = parseResponseMsg(resp.data);
                if (parsed.reqId != reqId) {
                    System.out.println("skip response reqId=" + parsed.reqId);
                    continue;
                }
                String value = parseCustomString(parsed.data);
                System.out.println("response " + name + ": " + value);
                return;
            }
        }

        private void send(int msgId, byte[] data) throws Exception {
            ByteArrayOutputStream body = new ByteArrayOutputStream();
            body.write(shortBytes(msgId));
            body.write(data);
            byte[] payload = body.toByteArray();
            int index = SEND_INDEX.incrementAndGet();
            if (verifyCode != null && msgId != CLIENT_KEY) {
                payload = verifyEncode(index, verifyCode, payload, encodes);
            }

            out.writeInt(payload.length + 8);
            out.writeInt(index);
            out.write(payload);
            out.writeInt(0);
            out.flush();
        }

        private Response readResponse() throws Exception {
            int len = in.readInt();
            int index = in.readInt();
            byte[] payload = new byte[len];
            in.readFully(payload);
            if (verifyCode != null) {
                payload = verifyDecode(index, verifyCode, payload, decodes);
            }
            int msgId = ((payload[0] & 0xff) << 8) | (payload[1] & 0xff);
            byte[] data = new byte[Math.max(0, payload.length - 2)];
            if (data.length > 0) {
                System.arraycopy(payload, 2, data, 0, data.length);
            }
            if (msgId == CLIENT_KEY) {
                byte[] key = rsaDecrypt(data);
                int version = (key[0] & 0xff) | ((key[1] & 0xff) << 8)
                        | ((key[2] & 0xff) << 16) | ((key[3] & 0xff) << 24);
                verifyCode = (short) ((key[4] & 0xff) | ((key[5] & 0xff) << 8));
                System.out.println("received CLIENT_KEY version=" + version + ", verifyCode=" + verifyCode);
            }
            return new Response(msgId, index, data);
        }

        @Override
        public void close() throws Exception {
            socket.close();
        }
    }

    private static ParsedResponse parseResponseMsg(byte[] data) {
        int pos = 0;
        int reqId = 0;
        byte[] payload = new byte[0];
        while (pos < data.length) {
            int[] key = readVarint(data, pos);
            pos = key[1];
            int field = key[0] >>> 3;
            int wire = key[0] & 7;
            if (field == 1 && wire == 0) {
                int[] value = readVarint(data, pos);
                reqId = value[0];
                pos = value[1];
            } else if (field == 2 && wire == 2) {
                int[] len = readVarint(data, pos);
                pos = len[1];
                payload = new byte[len[0]];
                System.arraycopy(data, pos, payload, 0, len[0]);
                pos += len[0];
            } else {
                pos = skipField(data, pos, wire);
            }
        }
        return new ParsedResponse(reqId, payload);
    }

    private static String parseCustomString(byte[] data) {
        int pos = 0;
        while (pos < data.length) {
            int[] key = readVarint(data, pos);
            pos = key[1];
            int field = key[0] >>> 3;
            int wire = key[0] & 7;
            if (field == 1 && wire == 2) {
                int[] len = readVarint(data, pos);
                pos = len[1];
                return new String(data, pos, len[0], StandardCharsets.UTF_8);
            }
            pos = skipField(data, pos, wire);
        }
        return "";
    }

    private static int skipField(byte[] data, int pos, int wire) {
        if (wire == 0) {
            return readVarint(data, pos)[1];
        }
        if (wire == 2) {
            int[] len = readVarint(data, pos);
            return len[1] + len[0];
        }
        throw new IllegalArgumentException("unsupported protobuf wire type: " + wire);
    }

    private static int[] readVarint(byte[] data, int pos) {
        int result = 0;
        int shift = 0;
        while (pos < data.length) {
            int b = data[pos++] & 0xff;
            result |= (b & 0x7f) << shift;
            if ((b & 0x80) == 0) {
                return new int[]{result, pos};
            }
            shift += 7;
        }
        throw new IllegalArgumentException("truncated varint");
    }

    private static byte[] proto(byte[]... fields) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] field : fields) {
            out.write(field);
        }
        return out.toByteArray();
    }

    private static byte[] fieldVarint(int field, int value) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeVarint(out, field << 3);
        writeVarint(out, value);
        return out.toByteArray();
    }

    private static byte[] fieldString(int field, String value) throws Exception {
        return fieldBytes(field, value.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] fieldBytes(int field, byte[] value) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeVarint(out, (field << 3) | 2);
        writeVarint(out, value.length);
        out.write(value);
        return out.toByteArray();
    }

    private static void writeVarint(ByteArrayOutputStream out, int value) {
        while ((value & ~0x7f) != 0) {
            out.write((value & 0x7f) | 0x80);
            value >>>= 7;
        }
        out.write(value);
    }

    private static byte[] shortBytes(int value) {
        return new byte[]{(byte) ((value >>> 8) & 0xff), (byte) (value & 0xff)};
    }

    private static byte[] readAll(java.io.InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int n;
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    private static byte[] readVerifyTable(String path) throws Exception {
        if (Files.exists(Paths.get(path))) {
            return Files.readAllBytes(Paths.get(path));
        }
        String fromLoginModule = "../tank-server/config/" + Paths.get(path).getFileName();
        return Files.readAllBytes(Paths.get(fromLoginModule));
    }

    private static byte[] verifyEncode(int index, short key, byte[] data, byte[] encodes) {
        int l;
        int r;
        byte[] result = new byte[data.length];
        if (index % 2 == 0) {
            l = ((key >> 8) + index) & 0xFF;
            r = key & 0xFF;
        } else {
            l = (key >> 8) & 0xFF;
            r = (key + index) & 0xFF;
        }
        for (int i = 0; i < data.length; i++) {
            int d = data[i] < 0 ? data[i] + 256 : data[i];
            int col = (r + d & 0xFF) % 256;
            int cursor = l * 256 + col;
            result[i] = encodes[cursor];
        }
        return result;
    }

    private static byte[] verifyDecode(int index, short key, byte[] data, byte[] decodes) {
        int l;
        int r;
        byte[] result = new byte[data.length];
        if (index % 2 == 0) {
            l = ((key >> 8) + index) & 0xFF;
            r = key & 0xFF;
        } else {
            l = (key >> 8) & 0xFF;
            r = (key + index) & 0xFF;
        }
        for (int i = 0; i < data.length; i++) {
            int d = data[i] < 0 ? data[i] + 256 : data[i];
            int cursor = l * 256 + (d & 0xFF);
            int v = decodes[cursor];
            v = v < 0 ? v + 256 : v;
            int decoded = (v - r + 256) % 256;
            result[i] = decoded < Byte.MAX_VALUE ? (byte) decoded : (byte) (decoded - 256);
        }
        return result;
    }

    private static byte[] rsaDecrypt(byte[] encrypted) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKeyFromBlob(RSA_PRIVATE_BLOB));
        return cipher.doFinal(encrypted);
    }

    private static PrivateKey privateKeyFromBlob(String base64Blob) throws Exception {
        byte[] blob = Base64.getDecoder().decode(base64Blob);
        int bitLen = readIntLE(blob, 12);
        int byteLen = bitLen / 8;
        int halfLen = byteLen / 2;
        int pos = 20;
        BigInteger publicExponent = leUnsigned(blob, pos, 4);
        pos += 4;
        BigInteger modulus = leUnsigned(blob, pos, byteLen);
        pos += byteLen;
        BigInteger primeP = leUnsigned(blob, pos, halfLen);
        pos += halfLen;
        BigInteger primeQ = leUnsigned(blob, pos, halfLen);
        pos += halfLen;
        BigInteger primeExponentP = leUnsigned(blob, pos, halfLen);
        pos += halfLen;
        BigInteger primeExponentQ = leUnsigned(blob, pos, halfLen);
        pos += halfLen;
        BigInteger crtCoefficient = leUnsigned(blob, pos, halfLen);
        pos += halfLen;
        BigInteger privateExponent = leUnsigned(blob, pos, byteLen);
        RSAPrivateCrtKeySpec spec = new RSAPrivateCrtKeySpec(modulus, publicExponent,
                privateExponent, primeP, primeQ, primeExponentP, primeExponentQ, crtCoefficient);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    private static int readIntLE(byte[] data, int offset) {
        return (data[offset] & 0xff)
                | ((data[offset + 1] & 0xff) << 8)
                | ((data[offset + 2] & 0xff) << 16)
                | ((data[offset + 3] & 0xff) << 24);
    }

    private static BigInteger leUnsigned(byte[] data, int offset, int length) {
        byte[] value = Arrays.copyOfRange(data, offset, offset + length);
        for (int i = 0; i < value.length / 2; i++) {
            byte tmp = value[i];
            value[i] = value[value.length - 1 - i];
            value[value.length - 1 - i] = tmp;
        }
        return new BigInteger(1, value);
    }

    private static LoginSession parseLoginSession(String json) {
        int uid = extractInt(json, "uid");
        if (uid <= 0) {
            uid = extractInt(json, "id");
        }
        String token = extractString(json, "token");
        if (token.isEmpty()) {
            token = extractString(json, "Token");
        }
        return new LoginSession(uid, token);
    }

    private static int extractInt(String json, String key) {
        String value = extractRaw(json, key);
        if (value.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static String extractString(String json, String key) {
        String quoted = "\"" + key + "\"";
        int keyAt = json.indexOf(quoted);
        if (keyAt < 0) {
            return "";
        }
        int colon = json.indexOf(':', keyAt + quoted.length());
        if (colon < 0) {
            return "";
        }
        int start = json.indexOf('"', colon + 1);
        if (start < 0) {
            return "";
        }
        StringBuilder value = new StringBuilder();
        for (int i = start + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                value.append(json.charAt(++i));
            } else if (c == '"') {
                return value.toString();
            } else {
                value.append(c);
            }
        }
        return "";
    }

    private static String extractRaw(String json, String key) {
        String quoted = "\"" + key + "\"";
        int keyAt = json.indexOf(quoted);
        if (keyAt < 0) {
            return "";
        }
        int colon = json.indexOf(':', keyAt + quoted.length());
        if (colon < 0) {
            return "";
        }
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        return json.substring(start, end);
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >>> 4) & 0x0f, 16));
            sb.append(Character.forDigit(b & 0x0f, 16));
        }
        return sb.toString();
    }

    private static String mask(String value) {
        if (value == null || value.length() <= 8) {
            return "****";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }

    private static final class LoginSession {
        private final int uid;
        private final String token;

        private LoginSession(int uid, String token) {
            this.uid = uid;
            this.token = token == null ? "" : token;
        }
    }

    private static final class Response {
        private final int msgId;
        private final int index;
        private final byte[] data;

        private Response(int msgId, int index, byte[] data) {
            this.msgId = msgId;
            this.index = index;
            this.data = data;
        }
    }

    private static final class ParsedResponse {
        private final int reqId;
        private final byte[] data;

        private ParsedResponse(int reqId, byte[] data) {
            this.reqId = reqId;
            this.data = data;
        }
    }
}
