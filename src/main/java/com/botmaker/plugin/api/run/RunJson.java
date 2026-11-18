// SPDX-FileCopyrightText: 2026 LiQiyeDev
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package com.botmaker.plugin.api.run;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The JSON a run's arguments cross in ({@link RunArguments}): this module has no Jackson, and a run's arguments are
 * a tool's — objects, arrays, text, numbers, yes and no. {@link #write} writes what {@link #read} reads back.
 */
final class RunJson {

    private RunJson() {
    }

    /** {@code value} as JSON: a {@code Map}, a {@code Collection}, text, a number, a boolean, an enum or null. */
    static String write(Object value) {
        StringBuilder out = new StringBuilder();
        write(value, out);
        return out.toString();
    }

    private static void write(Object value, StringBuilder out) {
        switch (value) {
            case null -> out.append("null");
            case Boolean yes -> out.append(yes);
            case Double d when d.isNaN() || d.isInfinite() -> throw new IllegalArgumentException("not a JSON number: " + d);
            case Float f when f.isNaN() || f.isInfinite() -> throw new IllegalArgumentException("not a JSON number: " + f);
            case Number number -> out.append(number);
            case Enum<?> constant -> text(constant.name(), out);
            case CharSequence text -> text(text.toString(), out);
            case Map<?, ?> map -> {
                out.append('{');
                boolean first = true;
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getValue() == null) continue;
                    if (!first) out.append(',');
                    first = false;
                    text(String.valueOf(entry.getKey()), out);
                    out.append(':');
                    write(entry.getValue(), out);
                }
                out.append('}');
            }
            case Collection<?> list -> {
                out.append('[');
                boolean first = true;
                for (Object each : list) {
                    if (!first) out.append(',');
                    first = false;
                    write(each, out);
                }
                out.append(']');
            }
            default -> throw new IllegalArgumentException("not a JSON value: " + value.getClass().getName());
        }
    }

    private static void text(String text, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        out.append('"');
    }

    /**
     * {@code json} read into {@code Map} (in order), {@code List}, {@code String}, {@code Long}, {@code BigInteger},
     * {@code Double}, {@code Boolean} and {@code null}.
     *
     * @throws IllegalArgumentException when it is not one JSON value, saying where
     */
    static Object read(String json) {
        Reader reader = new Reader(json);
        Object value = reader.value();
        reader.space();
        if (reader.at < json.length()) throw reader.wrong("nothing");
        return value;
    }

    private static final class Reader {
        private final String in;
        private int at;

        Reader(String in) {
            this.in = in;
        }

        Object value() {
            space();
            if (at >= in.length()) throw wrong("a value");
            char c = in.charAt(at);
            return switch (c) {
                case '{' -> object();
                case '[' -> array();
                case '"' -> text();
                case 't' -> word("true", Boolean.TRUE);
                case 'f' -> word("false", Boolean.FALSE);
                case 'n' -> word("null", null);
                default -> number();
            };
        }

        private Map<String, Object> object() {
            at++;
            Map<String, Object> map = new LinkedHashMap<>();
            space();
            if (peek('}')) return map;
            while (true) {
                space();
                if (at >= in.length() || in.charAt(at) != '"') throw wrong("a key");
                String key = text();
                space();
                expect(':');
                map.put(key, value());
                space();
                if (peek('}')) return map;
                expect(',');
            }
        }

        private List<Object> array() {
            at++;
            List<Object> list = new ArrayList<>();
            space();
            if (peek(']')) return list;
            while (true) {
                list.add(value());
                space();
                if (peek(']')) return list;
                expect(',');
            }
        }

        private String text() {
            at++;
            StringBuilder out = new StringBuilder();
            while (at < in.length()) {
                char c = in.charAt(at++);
                if (c == '"') return out.toString();
                if (c != '\\') {
                    out.append(c);
                    continue;
                }
                if (at >= in.length()) break;
                char escaped = in.charAt(at++);
                switch (escaped) {
                    case '"', '\\', '/' -> out.append(escaped);
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'u' -> {
                        if (at + 4 > in.length()) throw wrong("four hex digits");
                        try {
                            out.append((char) Integer.parseInt(in.substring(at, at + 4), 16));
                        } catch (NumberFormatException e) {
                            throw wrong("four hex digits");
                        }
                        at += 4;
                    }
                    default -> throw wrong("an escape");
                }
            }
            throw wrong("the end of the text");
        }

        private Object number() {
            int start = at;
            while (at < in.length() && "+-0123456789.eE".indexOf(in.charAt(at)) >= 0) at++;
            String number = in.substring(start, at);
            if (number.isEmpty()) throw wrong("a value");
            try {
                if (number.matches("-?\\d+")) {
                    BigInteger whole = new BigInteger(number);
                    return whole.bitLength() < 64 ? (Object) whole.longValue() : whole;
                }
                return new BigDecimal(number).doubleValue();
            } catch (NumberFormatException e) {
                at = start;
                throw wrong("a number");
            }
        }

        private Object word(String word, Object value) {
            if (!in.startsWith(word, at)) throw wrong("a value");
            at += word.length();
            return value;
        }

        private boolean peek(char c) {
            if (at < in.length() && in.charAt(at) == c) {
                at++;
                return true;
            }
            return false;
        }

        private void expect(char c) {
            if (!peek(c)) throw wrong("'" + c + "'");
        }

        void space() {
            while (at < in.length() && Character.isWhitespace(in.charAt(at))) at++;
        }

        IllegalArgumentException wrong(String wanted) {
            return new IllegalArgumentException("not JSON: " + wanted + " expected at " + at);
        }
    }
}
