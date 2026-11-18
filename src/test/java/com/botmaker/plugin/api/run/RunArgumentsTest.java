// SPDX-FileCopyrightText: 2026 LiQiyeDev
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package com.botmaker.plugin.api.run;

import com.botmaker.plugin.api.assist.Describe;
import com.botmaker.plugin.api.assist.ToolSchema;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunArgumentsTest {

    public static final class Sample {
        public static void main(String[] args) {
        }
    }

    enum Speed {SLOW, FAST}

    record Window(@Describe("its width") int width, @Describe(value = "its title", optional = true) String title) {
    }

    record Budget(@Describe(value = "minutes at most", optional = true) Integer minutes,
                  @Describe(value = "how fast", optional = true) Speed speed,
                  @Describe(value = "the window", optional = true) Window window,
                  @Describe(value = "names", optional = true) List<String> names) {
    }

    record Needed(@Describe("the site") String url) {
    }

    private static final RunEntry BUDGETED = RunEntry.of("Explore", Sample::main).takes(Budget.class);

    @Test
    void theSchemaIsReadAsATool() {
        List<ToolSchema.Field> fields = BUDGETED.schema().fields();
        assertEquals(List.of("minutes", "speed", "window", "names"), fields.stream().map(ToolSchema.Field::name).toList());
        assertEquals(ToolSchema.Field.Kind.WHOLE_NUMBER, fields.get(0).kind());
        assertTrue(fields.get(0).optional());
        assertEquals(List.of("slow", "fast"), fields.get(1).choices());
        ToolSchema.Field window = fields.get(2);
        assertEquals(ToolSchema.Field.Kind.RECORD, window.kind());
        assertEquals(List.of("width", "title"), window.fields().stream().map(ToolSchema.Field::name).toList());
        assertFalse(window.fields().get(0).optional());
        assertFalse(BUDGETED.needsArguments());
    }

    @Test
    void anEntryWithoutParametersTakesNone() {
        RunEntry plain = RunEntry.of("Explore", Sample::main);
        assertEquals(List.of(), plain.schema().fields());
        assertEquals("", plain.arguments(null));
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> plain.arguments(Map.of("minutes", 1)));
        assertTrue(refused.getMessage().contains("minutes"), refused.getMessage());
    }

    @Test
    void aRequiredParameterKeepsItFromStartingWithNone() {
        RunEntry entry = RunEntry.of("Discover", Sample::main).takes(Needed.class).offBar();
        assertTrue(entry.needsArguments());
        assertFalse(entry.onBar(), "offBar keeps the parameters and takes keeps the bar");
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class, () -> entry.arguments(Map.of()));
        assertTrue(refused.getMessage().contains("'url' is required"), refused.getMessage());
    }

    @Test
    void aRefusedArgumentNamesItsField() {
        IllegalArgumentException wrongKind = assertThrows(IllegalArgumentException.class,
                () -> BUDGETED.arguments(Map.of("minutes", "soon")));
        assertTrue(wrongKind.getMessage().contains("'minutes'"), wrongKind.getMessage());
        IllegalArgumentException nested = assertThrows(IllegalArgumentException.class,
                () -> BUDGETED.arguments(Map.of("window", Map.of("title", "x"))));
        assertTrue(nested.getMessage().contains("'window.width' is required"), nested.getMessage());
        IllegalArgumentException choice = assertThrows(IllegalArgumentException.class,
                () -> BUDGETED.arguments(Map.of("speed", "warp")));
        assertTrue(choice.getMessage().contains("'speed'"), choice.getMessage());
    }

    @Test
    void argumentsRoundTripThroughTheRunProperty() {
        Map<String, Object> sent = new LinkedHashMap<>();
        sent.put("minutes", 1);
        sent.put("speed", "fast");
        sent.put("window", Map.of("width", 800, "title", "a \"quoted\" title\nwith é and \\"));
        sent.put("names", List.of("a", "b"));
        String property = BUDGETED.arguments(sent);
        assertFalse(property.contains("\""), "no quote reaches a command line: " + property);
        Budget read = RunArguments.decode(Budget.class, property);
        assertEquals(new Budget(1, Speed.FAST, new Window(800, "a \"quoted\" title\nwith é and \\"),
                List.of("a", "b")), read);
    }

    @Test
    void aRunStartedWithNoneReadsEveryFieldLeftOut() {
        Budget none = RunArguments.decode(Budget.class, null);
        assertNull(none.minutes());
        assertNull(none.window());
        assertThrows(IllegalArgumentException.class, () -> RunArguments.decode(Needed.class, ""));
    }

    @Test
    void jsonReadsWhatItWrites() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("text", "tab\there \u0001 \"q\"");
        value.put("whole", 12345678901L);
        value.put("number", 1.5);
        value.put("yes", true);
        value.put("list", List.of(1L, List.of(), Map.of()));
        assertEquals(value, RunJson.read(RunJson.write(value)));
        assertEquals(Map.of("a", List.of(-2L, 3.0e2)), RunJson.read(" { \"a\" : [ -2 , 3e2 ] } "));
        assertThrows(IllegalArgumentException.class, () -> RunJson.read("{\"a\":1} x"));
        assertThrows(IllegalArgumentException.class, () -> RunJson.read("{\"a\":}"));
    }
}
