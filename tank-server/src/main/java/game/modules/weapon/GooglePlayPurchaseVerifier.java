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
import java.security.MessageDigest;
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

    static final class Entitlement {
        final Boolean active;
        final boolean refunded;
        Entitlement(Boolean active, boolean refunded) { this.active = active; this.refunded = refunded; }
    }

    /** Null means uncertain: network/identity errors and partial refunds never authorize a revocation. */
    Boolean getOrderEntitlement(String packageName, String orderId, String productId, String expectedAccountId) throws Exception {
        return getOrderEntitlement(packageName, orderId, productId, expectedAccountId, null);
    }

    Boolean getOrderEntitlement(String packageName, String orderId, String productId, String expectedAccountId,
                                String expectedTokenHash) throws Exception {
        return getOrderAssessment(packageName, orderId, productId, expectedAccountId, expectedTokenHash).active;
    }

    Entitlement getOrderAssessment(String packageName, String orderId, String productId, String expectedAccountId,
                                   String expectedTokenHash) throws Exception {
        String base = API_ROOT + encode(packageName);
        JsonObject order = requestJson("GET", base + "/orders/" + encode(orderId), null);
        if (!orderId.equals(getString(order, "orderId")) || !hasProduct(order, "lineItems", productId))
            return auditEntitlement(orderId, order, null, null, "order_identity_or_product_mismatch");
        String token = getString(order, "purchaseToken");
        if (token == null || token.isEmpty())
            return auditEntitlement(orderId, order, null, null, "purchase_token_missing");
        if (expectedTokenHash != null) {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hash = new StringBuilder();
            for (byte value : digest) hash.append(String.format("%02x", value & 0xff));
            if (!MessageDigest.isEqual(expectedTokenHash.getBytes(StandardCharsets.UTF_8),
                    hash.toString().getBytes(StandardCharsets.UTF_8)))
                return auditEntitlement(orderId, order, null, null, "purchase_token_mismatch");
        }
        JsonObject purchase = requestJson("GET", base + "/purchases/productsv2/tokens/" + encode(token), null);
        Entitlement result = classifyAssessment(order, purchase, orderId, productId, expectedAccountId);
        auditEntitlement(orderId, order, purchase, result.active,
                result.refunded ? "verified_full_refund" : (result.active == null ? "state_or_identity_uncertain" : "official_state_pair"));
        return result;
    }

    private Entitlement auditEntitlement(String orderId, JsonObject order, JsonObject purchase, Boolean active, String reason) {
        JsonObject context = purchase != null && purchase.has("purchaseStateContext")
                && purchase.get("purchaseStateContext").isJsonObject()
                ? purchase.getAsJsonObject("purchaseStateContext") : null;
        // Log only state labels: raw responses, account IDs, tokens and exception URLs are private.
        logger.info("Google entitlement order audit orderId={} orderState={} purchaseState={} active={} reason={}",
                orderId, auditState(order, "state"), auditState(context, "purchaseState"), active, reason);
        return new Entitlement(active, false);
    }

    private static String auditState(JsonObject json, String key) {
        if (json == null || !json.has(key) || json.get(key).isJsonNull()) return "MISSING";
        if (!json.get(key).isJsonPrimitive()) return "UNRECOGNIZED";
        String state = json.get(key).getAsString();
        return state.matches("[A-Z_]{1,40}") ? state : "UNRECOGNIZED";
    }

    static Boolean classifyEntitlement(JsonObject order, JsonObject purchase, String orderId,
                                      String productId, String expectedAccountId) {
        return classifyAssessment(order, purchase, orderId, productId, expectedAccountId).active;
    }

    static Entitlement classifyAssessment(JsonObject order, JsonObject purchase, String orderId,
                                          String productId, String expectedAccountId) {
        String account = getString(purchase, "obfuscatedExternalAccountId");
        if (expectedAccountId == null || expectedAccountId.isEmpty() || account == null
                || !MessageDigest.isEqual(expectedAccountId.getBytes(StandardCharsets.UTF_8), account.getBytes(StandardCharsets.UTF_8))
                || !orderId.equals(getString(order, "orderId")) || !orderId.equals(getString(purchase, "orderId"))
                || !hasProduct(order, "lineItems", productId) || !hasProduct(purchase, "productLineItem", productId)) return new Entitlement(null, false);
        // Full monetary refund is authoritative after binding checks, even without product revocation.
        String state = getString(order, "state");
        if ("REFUNDED".equals(state)) return new Entitlement(false, true);
        if (!purchase.has("purchaseStateContext") || !purchase.get("purchaseStateContext").isJsonObject()) return new Entitlement(null, false);
        String purchaseState = getString(purchase.getAsJsonObject("purchaseStateContext"), "purchaseState");
        if ("PROCESSED".equals(state) && "PURCHASED".equals(purchaseState)) return new Entitlement(true, false);
        if ("CANCELED".equals(state) && "CANCELLED".equals(purchaseState)) return new Entitlement(false, false);
        return new Entitlement(null, false);
    }

    private static boolean hasProduct(JsonObject object, String field, String productId) {
        if (!object.has(field) || !object.get(field).isJsonArray()) return false;
        for (com.google.gson.JsonElement item : object.getAsJsonArray(field)) {
            if (item.isJsonObject() && productId.equals(getString(item.getAsJsonObject(), "productId"))) return true;
        }
        return false;
    }

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
