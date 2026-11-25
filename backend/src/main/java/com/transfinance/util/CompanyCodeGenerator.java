package com.transfinance.util;

import java.nio.charset.Charset;
import java.util.Locale;

/**
 * 企业编码生成工具。
 * 中文字符使用 GB2312 区位码估算拼音首字母，英文和数字直接保留为小写。
 */
public final class CompanyCodeGenerator {

    private static final Charset GB2312 = Charset.forName("GB2312");
    private static final int[] AREA_CODES = {
            45217, 45253, 45761, 46318, 46826, 47010, 47297, 47614, 48119, 49062, 49324, 49896,
            50371, 50614, 50622, 50906, 51387, 51446, 52218, 52698, 52980, 53689, 54481, 55290
    };
    private static final char[] INITIALS = {
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'j', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'w', 'x', 'y', 'z'
    };

    private CompanyCodeGenerator() {
    }

    public static String initials(String text) {
        if (text == null || text.isBlank()) {
            return "company";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char value = text.charAt(i);
            if (Character.isLetterOrDigit(value) && value < 128) {
                builder.append(Character.toLowerCase(value));
            } else if (isChinese(value)) {
                builder.append(toPinyinInitial(value));
            }
        }
        String code = builder.toString().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return code.isBlank() ? "company" : code;
    }

    private static boolean isChinese(char value) {
        Character.UnicodeScript script = Character.UnicodeScript.of(value);
        return script == Character.UnicodeScript.HAN;
    }

    private static char toPinyinInitial(char value) {
        try {
            byte[] bytes = String.valueOf(value).getBytes(GB2312);
            if (bytes.length < 2) {
                return 'x';
            }
            int code = (bytes[0] & 0xff) * 256 + (bytes[1] & 0xff);
            for (int i = AREA_CODES.length - 1; i >= 0; i--) {
                if (code >= AREA_CODES[i]) {
                    return INITIALS[i];
                }
            }
        } catch (Exception ignored) {
            return 'x';
        }
        return 'x';
    }
}
