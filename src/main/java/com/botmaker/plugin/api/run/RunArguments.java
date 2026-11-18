// SPDX-FileCopyrightText: 2026 LiQiyeDev
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package com.botmaker.plugin.api.run;

import com.botmaker.plugin.api.assist.ToolSchema;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * The arguments a run was started with, inside the run: {@code RunArguments.read(Budget.class)} in the {@code main}
 * of an entry declared {@link RunEntry#takes(Class) RunEntry.of(…).takes(Budget.class)}.
 *
 * <p>The host checks what it was given against the entry's schema before the start ({@link RunEntry#arguments}) and
 * hands it to the run's JVM as the one system property {@link #PROPERTY}. An entry started with none — its toolbar
 * button, {@code Runs.start(entry)} — reads the record with every field left out: an optional field is {@code null},
 * and the plugin applies its own default.
 */
public final class RunArguments {

    /**
     * The system property a run's arguments cross in: their JSON, Base64 (URL-safe) so that no command line —
     * Windows' least of all — has quotes to mangle. Unset for a run started with none.
     */
    public static final String PROPERTY = "botmaker.run.arguments";

    private RunArguments() {
    }

    /**
     * The record this run was started with, built as {@link RunEntry#takes} described it.
     *
     * @throws IllegalArgumentException naming the field, when the run was started without a required one, or with
     *                                  arguments for another record
     */
    public static <P extends Record> P read(Class<P> params) {
        return decode(params, System.getProperty(PROPERTY));
    }

    /** {@code text} as {@link #PROPERTY} holds it, built into {@code params}; none for {@code null} or blank. */
    static <P extends Record> P decode(Class<P> params, String text) {
        if (text == null || text.isBlank()) return ToolSchema.build(params, Map.of());
        String json;
        try {
            json = new String(Base64.getUrlDecoder().decode(text.strip()), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(PROPERTY + " is not Base64: " + e.getMessage(), e);
        }
        if (!(RunJson.read(json) instanceof Map<?, ?> object)) {
            throw new IllegalArgumentException(PROPERTY + " is not a JSON object");
        }
        @SuppressWarnings("unchecked")
        Map<String, ?> sent = (Map<String, ?>) object;
        return ToolSchema.build(params, sent);
    }

    /** {@code sent} as {@link #PROPERTY} holds it. */
    static String encode(Map<String, ?> sent) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(RunJson.write(sent).getBytes(StandardCharsets.UTF_8));
    }
}
