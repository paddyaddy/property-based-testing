package pbt.components.generator;

import com.google.gson.JsonObject;
import pbt.spi.generator.Generator;
import pbt.spi.generator.GeneratorProvider;

/**
 * Generates {@link String} values from a configurable character set.
 * Params: {@code minLen} (0), {@code maxLen} (10), {@code chars} (default: {@code a-z}).
 */
public final class StringGeneratorProvider implements GeneratorProvider {

    static final String DEFAULT_CHARS = "abcdefghijklmnopqrstuvwxyz";

    public static String getChars(JsonObject params) {
        if (params == null || !params.has("chars") || params.get("chars").isJsonNull()) {
            return DEFAULT_CHARS;
        }
        return params.get("chars").getAsString();
    }

    @Override
    public String name() {
        return "string";
    }

    @Override
    public Generator<String> build(JsonObject params) {
        int minLen = IntGeneratorProvider.getInt(params, "minLen", 0);
        int maxLen = IntGeneratorProvider.getInt(params, "maxLen", 10);
        String chars = getChars(params);
        if (minLen < 0 || minLen > maxLen) {
            throw new IllegalArgumentException("string: invalid length bounds " + minLen + ".." + maxLen);
        }
        if (chars.isEmpty()) {
            throw new IllegalArgumentException("string: chars must not be empty");
        }
        return rnd -> {
            int len = minLen == maxLen ? minLen : minLen + rnd.nextInt(maxLen - minLen + 1);
            StringBuilder sb = new StringBuilder(len);
            for (int i = 0; i < len; i++) {
                sb.append(chars.charAt(rnd.nextInt(chars.length())));
            }
            return sb.toString();
        };
    }
}
