package game.modules.weapon;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import framework.SystemConfigData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

class GooglePlayPurchaseVerifier {
    private static final Logger logger = LoggerFactory.getLogger(GooglePlayPurchaseVerifier.class);

    private static final String SCOPE = "https://www.googleapis.com/auth/androidpublisher";
    private static final String DEFAULT_TOKEN_URI = "https://oauth2.googleapis.com/token";
    private static final String API_ROOT = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/";

    private JsonObject serviceAccount;
    private String accessToken;
    private long accessTokenExpireAt;

    Purchase getProductPurchase(String packageName, String productId, String purchaseToken) throws Exception {
        String url = API_ROOT + encode(packageName) + "/purchases/products/" + encode(productId)
                + "/tokens/" + encode(purchaseToken);
        JsonObject json = requestJson("GET", url, null);
        Purchase purchase = new Purchase();
        purchase.orderId = getString(json, "orderId");
        purchase.productId = getString(json, "productId");
        purchase.purchaseToken = getString(json, "purchaseToken");
        purchase.purchaseState = getInt(json, "purchaseState", -1);
        purchase.consumptionState = getInt(json, "consumptionState", -1);
        purchase.acknowledgementState = getInt(json, "acknowledgementState", -1);
        purchase.purchaseTimeMillis = getLong(json, "purchaseTimeMillis", 0);
        purchase.quantity = getInt(json, "quantity", 1);
        purchase.obfuscatedExternalAccountId = getString(json, "obfuscatedExternalAccountId");
        return purchase;
    }

    void consumeProductPurchase(String packageName, String productId, String purchaseToken) throws Exception {
        String url = API_ROOT + encode(packageName) + "/purchases/products/" + encode(productId)
                + "/tokens/" + encode(purchaseToken) + ":consume";
        requestJson("POST", url, "");
    }

    private JsonObject requestJson(String method, String url, String body) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setRequestProperty("Authorization", "Bearer " + getAccessToken());
        conn.setRequestProperty("Accept", "application/json");
        if (body != null) {
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
        }

        int code = conn.getResponseCode();
        String text = readAll(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream());
        if (code < 200 || code >= 300) {
            throw new IllegalStateException("Google Play API " + method + " failed, code=" + code + ", body=" + text);
        }
        if (text == null || text.trim().isEmpty()) {
            return new JsonObject();
        }
        return new JsonParser().parse(text).getAsJsonObject();
    }

    private synchronized String getAccessToken() throws Exception {
        long now = System.currentTimeMillis();
        if (accessToken != null && now + 60000 < accessTokenExpireAt) {
            return accessToken;
        }

        JsonObject account = getServiceAccount();
        String clientEmail = getString(account, "client_email");
        String privateKey = getString(account, "private_key");
        String tokenUri = getString(account, "token_uri");
        if (tokenUri == null || tokenUri.isEmpty()) {
            tokenUri = DEFAULT_TOKEN_URI;
        }
        if (clientEmail == null || clientEmail.isEmpty() || privateKey == null || privateKey.isEmpty()) {
            throw new IllegalStateException("Google service account json missing client_email/private_key");
        }

        long iat = now / 1000;
        long exp = iat + 3600;
        String header = "{\"alg\":\"RS256\",\"typ\":\"JWT\"}";
        String claim = "{\"iss\":\"" + jsonEscape(clientEmail) + "\",\"scope\":\"" + SCOPE
                + "\",\"aud\":\"" + jsonEscape(tokenUri) + "\",\"iat\":" + iat + ",\"exp\":" + exp + "}";
        String unsigned = base64Url(header.getBytes(StandardCharsets.UTF_8)) + "."
                + base64Url(claim.getBytes(StandardCharsets.UTF_8));
        String assertion = unsigned + "." + base64Url(sign(unsigned, privateKey));

        String form = "grant_type=" + URLEncoder.encode("urn:ietf:params:oauth:grant-type:jwt-bearer", "UTF-8")
                + "&assertion=" + URLEncoder.encode(assertion, "UTF-8");

        HttpURLConnection conn = (HttpURLConnection) new URL(tokenUri).openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        try (OutputStream os = conn.getOutputStream()) {
            os.write(form.getBytes(StandardCharsets.UTF_8));
        }
        int code = conn.getResponseCode();
        String text = readAll(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream());
        if (code < 200 || code >= 300) {
            throw new IllegalStateException("Google OAuth token failed, code=" + code + ", body=" + text);
        }
        JsonObject json = new JsonParser().parse(text).getAsJsonObject();
        accessToken = getString(json, "access_token");
        if (accessToken == null || accessToken.trim().isEmpty()) {
            throw new IllegalStateException("Google OAuth response missing access_token");
        }
        int expiresIn = getInt(json, "expires_in", 3600);
        accessTokenExpireAt = now + expiresIn * 1000L;
        return accessToken;
    }

    private JsonObject getServiceAccount() throws Exception {
        if (serviceAccount != null) {
            return serviceAccount;
        }
        String path = SystemConfigData.getConfig("googlePlayServiceAccountPath", "config/google-play-service-account.json");
        File file = new File(path);
        if (!file.isAbsolute() && !file.exists()) {
            file = new File("tank-server", path);
        }
        try (InputStream in = new FileInputStream(file)) {
            String text = readAll(in);
            serviceAccount = new JsonParser().parse(text).getAsJsonObject();
            logger.info("loaded google play service account from {}", file.getPath());
            return serviceAccount;
        }
    }

    private static byte[] sign(String data, String privateKeyPem) throws Exception {
        String key = privateKeyPem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] pkcs8 = Base64.getDecoder().decode(key);
        PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(pkcs8));
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(data.getBytes(StandardCharsets.UTF_8));
        return signature.sign();
    }

    private static String readAll(InputStream in) throws Exception {
        if (in == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private static String encode(String value) throws Exception {
        return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
    }

    private static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String getString(JsonObject json, String key) {
        return json != null && json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : "";
    }

    private static int getInt(JsonObject json, String key, int defaultValue) {
        try {
            return json != null && json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsInt() : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static long getLong(JsonObject json, String key, long defaultValue) {
        try {
            return json != null && json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsLong() : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    static class Purchase {
        String orderId;
        String productId;
        String purchaseToken;
        int purchaseState;
        int consumptionState;
        int acknowledgementState;
        long purchaseTimeMillis;
        int quantity;
        String obfuscatedExternalAccountId;
    }
}
