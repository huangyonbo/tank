package game.modules.weapon;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import java.nio.charset.StandardCharsets;

/** Saved with the same player archive as money and progress. Actor-thread use only. */
final class LevelAttemptState {
    static final String PROPERTY = "LevelAttemptState";
    static final long MAX_AGE_MS = 24L * 60 * 60 * 1000;
    private static final int MAX_RECEIPTS = 32;
    final JSONObject data;

    LevelAttemptState(String saved) {
        data = saved == null || saved.trim().isEmpty() ? new JSONObject() : JSON.parseObject(saved);
        if (data == null) {
            throw new IllegalArgumentException("invalid level attempt state");
        }
    }

    JSONObject active() { return data.getJSONObject("active"); }

    JSONObject receipt(String attemptId) {
        JSONArray receipts = data.getJSONArray("receipts");
        if (receipts != null && attemptId != null && !attemptId.isEmpty()) {
            for (int i = 0; i < receipts.size(); i++) {
                JSONObject receipt = receipts.getJSONObject(i);
                if (attemptId.equals(receipt.getString("attemptId"))) {
                    return JSON.parseObject(receipt.toJSONString());
                }
            }
        }
        return null;
    }

    void finish(JSONObject receipt, long now) {
        JSONArray receipts = data.getJSONArray("receipts");
        if (receipts == null) {
            receipts = new JSONArray();
            data.put("receipts", receipts);
        }
        JSONObject cached = JSON.parseObject(receipt.toJSONString());
        if (cached.remove("achievementStatus") != null) {
            // The archive encodes each STRING with a signed 16-bit byte length.
            // Economic receipts remain small; detailed status can be queried separately.
            cached.put("refreshAchievementStatus", true);
        }
        receipts.add(cached);
        while (receipts.size() > MAX_RECEIPTS
                || (receipts.size() > 1 && data.toJSONString().getBytes(StandardCharsets.UTF_8).length > 24000)) {
            receipts.remove(0);
        }
        data.remove("active");
        data.put("lastFinishTime", now);
    }

    static boolean resolveWin(JSONObject request) {
        Object win = request.get("isWin");
        if (!(win instanceof Boolean)) {
            throw new IllegalArgumentException("isWin must be boolean");
        }
        Object countProgress = request.get("countProgress");
        if (countProgress != null && !(countProgress instanceof Boolean)) {
            throw new IllegalArgumentException("countProgress must be boolean");
        }
        // A failure/restart always wins over the old, incorrectly reported isWin=true.
        String resolution = request.getString("resolution");
        if (resolution != null) {
            if ("Failed".equalsIgnoreCase(resolution) || "Restart".equalsIgnoreCase(resolution)
                    || "Aborted".equalsIgnoreCase(resolution)) {
                return false;
            }
            if (!"Won".equalsIgnoreCase(resolution)) {
                throw new ContractException("invalid_resolution");
            }
        }
        return !Boolean.FALSE.equals(countProgress) && Boolean.TRUE.equals(win);
    }

    static final class ContractException extends IllegalArgumentException {
        final String status;
        ContractException(String status) { super(status); this.status = status; }
    }

    static boolean integer(Object value) {
        return value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long;
    }

    static int settlementVersion(JSONObject request) {
        if (!request.containsKey("settlementVersion")) return 1;
        Object value = request.get("settlementVersion");
        if (!integer(value) || (((Number) value).longValue() != 1 && ((Number) value).longValue() != 2)) {
            throw new ContractException("unsupported_settlement_version");
        }
        return ((Number) value).intValue();
    }

    static String gameplayMode(JSONObject request, int levelId) {
        String expected = levelId == 1 ? "TutorialLevel" : levelId == 999 ? "SurvivalLevel" : "RegularLevel";
        if (!request.containsKey("gameplayMode")) return expected;
        Object mode = request.get("gameplayMode");
        if (!(mode instanceof String) || !("TutorialLevel".equals(mode)
                || "RegularLevel".equals(mode) || "SurvivalLevel".equals(mode))) {
            throw new ContractException("unsupported_gameplay_mode");
        }
        if (!expected.equals(mode)) throw new ContractException("gameplay_mode_mismatch");
        return expected;
    }

    /** Validate the whole strict request before any money, target or achievement mutation. */
    static String validateStrictFinish(JSONObject request, JSONObject active) {
        if (!(request.get("attemptId") instanceof String)) throw new ContractException("invalid_attempt_id");
        Object rawResolution = request.get("resolution");
        if (rawResolution == null) throw new ContractException("resolution_required");
        String resolution = null;
        for (String candidate : new String[]{"Won", "Failed", "Restart", "Aborted"}) {
            if (rawResolution instanceof String && candidate.equalsIgnoreCase((String) rawResolution)) {
                resolution = candidate;
                break;
            }
        }
        if (resolution == null) throw new ContractException("invalid_resolution");
        if ("Won".equals(resolution) && !Boolean.TRUE.equals(request.get("isWin"))) {
            throw new ContractException("resolution_mismatch");
        }
        if ("Won".equals(resolution) && Boolean.FALSE.equals(request.get("countProgress"))) {
            throw new ContractException("count_progress_mismatch");
        }
        if (!integer(request.get("coin")) || ((Number) request.get("coin")).longValue() < 0) {
            throw new ContractException("invalid_metrics");
        }
        Object counts = request.get("achievementCounts");
        if (request.containsKey("achievementCounts") && !(counts instanceof JSONObject)) {
            throw new ContractException("invalid_metrics");
        }
        if (counts instanceof JSONObject) {
            for (java.util.Map.Entry<String, Object> entry : ((JSONObject) counts).entrySet()) {
                try {
                    if (Integer.parseInt(entry.getKey()) < 0 || !integer(entry.getValue())
                            || ((Number) entry.getValue()).longValue() < 0) {
                        throw new ContractException("invalid_metrics");
                    }
                } catch (NumberFormatException e) { throw new ContractException("invalid_metrics"); }
            }
        }
        for (String field : new String[]{"littleTargetId", "littleTargetDelta"}) {
            if (request.containsKey(field) && (!integer(request.get(field))
                    || ((Number) request.get(field)).longValue() < 0
                    || ("littleTargetId".equals(field)
                        && ((Number) request.get(field)).longValue() > Integer.MAX_VALUE))) {
                throw new ContractException("invalid_metrics");
            }
        }
        boolean cancelled = "Restart".equals(resolution) || "Aborted".equals(resolution);
        if (!cancelled && request.getLongValue("littleTargetDelta") > 0) {
            JSONObject start = active.getJSONObject("startResponse");
            if (!request.containsKey("littleTargetId") || start == null
                    || !start.containsKey("littleTargetId")
                    || (start.containsKey("littleTargetAssigned") ? !start.getBooleanValue("littleTargetAssigned")
                        : start.getIntValue("littleTargetNeed") <= 0)
                    || request.getIntValue("littleTargetId") != start.getIntValue("littleTargetId")) {
                throw new ContractException("little_target_mismatch");
            }
        }
        return resolution;
    }
}
