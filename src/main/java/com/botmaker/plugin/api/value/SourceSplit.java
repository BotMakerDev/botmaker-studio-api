package com.botmaker.plugin.api.value;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Taking a written call back apart: the depth-zero comma split every container needs.
 *
 * <p><b>Not a parser, deliberately.</b> What this reads is what the catalog wrote — one literal per part —
 * and the host has a real Java parser (JDT) for anything else. The grammar exists so the <em>round trip</em>
 * is decidable without one, which is true only because the writer's output is the reader's whole input
 * domain.
 *
 * <p>Brackets and quotes are tracked so a part containing a comma is one part: a string, a
 * {@code new Color(255, 0, 0)}, a nested {@code List.of(a, b)}. An unbalanced source answers empty rather
 * than a wrong split — a partial reading is not a reading.
 *
 * <p>Lifted out of {@code ValueCatalog.listItems}, which did exactly this for {@code List.of(…)} alone.
 */
final class SourceSplit {

    private SourceSplit() {
    }

    /**
     * The arguments of a call to {@code factory}, or empty when {@code source} is not one.
     *
     * <p>Both the fully qualified spelling and the trailing one are accepted — {@code java.util.List.of(…)}
     * is what a generated file carries and {@code List.of(…)} is what a user's own file does, and a form has
     * to read both.
     */
    static Optional<List<String>> arguments(String source, String factory) {
        if (source == null || factory == null) return Optional.empty();
        String trimmed = source.strip();
        if (!trimmed.endsWith(")")) return Optional.empty();

        // Every suffix of the call that starts at a dot boundary, longest first: `java.util.Map.ofEntries`,
        // then `util.Map.ofEntries`, `Map.ofEntries`, `ofEntries`. A generated file carries the first and a
        // user's own file, having imported the class, carries a shorter one — a form has to read both, and
        // matching a suffix is how without knowing what the file imported.
        for (int start = 0; start >= 0; start = factory.indexOf('.', start) + 1) {
            String candidate = factory.substring(start) + "(";
            if (trimmed.startsWith(candidate)) {
                return parts(trimmed.substring(candidate.length(), trimmed.length() - 1));
            }
            if (factory.indexOf('.', start) < 0) break;
        }
        return Optional.empty();
    }

    /** {@code inner} split on its depth-zero commas, or empty when it does not balance. */
    static Optional<List<String>> parts(String inner) {
        if (inner == null) return Optional.empty();
        if (inner.isBlank()) return Optional.of(List.of());

        List<String> parts = new ArrayList<>();
        StringBuilder part = new StringBuilder();
        int depth = 0;
        boolean inString = false;
        boolean inChar = false;
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            boolean escaped = i > 0 && inner.charAt(i - 1) == '\\';
            if (c == '"' && !inChar && !escaped) inString = !inString;
            else if (c == '\'' && !inString && !escaped) inChar = !inChar;
            // Angle brackets are deliberately not tracked, as they were not before: a diamond balances
            // nothing and needs nothing, and an explicit type witness is something no writer here emits. A
            // lone `>` from tracking them would empty an answer that is fine.
            else if (!inString && !inChar && (c == '(' || c == '[' || c == '{')) depth++;
            else if (!inString && !inChar && (c == ')' || c == ']' || c == '}')) depth--;
            else if (!inString && !inChar && c == ',' && depth == 0) {
                parts.add(part.toString().strip());
                part.setLength(0);
                continue;
            }
            if (depth < 0) return Optional.empty();
            part.append(c);
        }
        if (depth != 0 || inString || inChar) return Optional.empty();
        parts.add(part.toString().strip());
        return Optional.of(List.copyOf(parts));
    }
}
