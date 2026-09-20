package com.botmaker.plugin.api.value;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The one grammar: a form directs the write, the same form directs the read, and a live value is what
 * crosses in both directions.
 *
 * <p>Three rules are what these tests are for, and they matter more with nesting rather than less — empty
 * means decline, a partial reading is not a reading, and the split is not a parser. The flat pair these
 * generalise is pinned by {@code ValueVocabularyTest} and the SDK's {@code LiteralInverseTest}, which now run
 * through this implementation.
 */
class ValueGrammarTest {

    private static final ValueType TEXT = ValueType.of(ValueCatalog.TEXT_ID).label("Text").source("String")
            .build();
    private static final ValueType COUNT = ValueType.of("WHOLE_NUMBER").label("Whole number").source("int")
            .boxed("Integer").primitive().build();
    private static final ValueType DURATION = ValueType.of("DURATION").label("Duration").source("Duration")
            .importing("java.time.Duration").build();

    private static final ValueCodec<String> TEXT_CODEC = new ValueCodec<>() {
        @Override
        public String parse(String wire) {
            return wire == null ? "" : wire;
        }

        @Override
        public String store(String value) {
            return value;
        }

        @Override
        public String literal(String value) {
            return "\"" + value + "\"";
        }

        @Override
        public Optional<String> valueOfLiteral(String javaSource) {
            String trimmed = javaSource.strip();
            return trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")
                    ? Optional.of(trimmed.substring(1, trimmed.length() - 1))
                    : Optional.empty();
        }
    };

    private static final ValueCodec<Integer> COUNT_CODEC = new ValueCodec<>() {
        @Override
        public Integer parse(String wire) {
            try {
                return Integer.valueOf(wire.strip());
            } catch (RuntimeException e) {
                return 0;
            }
        }

        @Override
        public String store(Integer value) {
            return String.valueOf(value);
        }

        @Override
        public String literal(Integer value) {
            return String.valueOf(value);
        }

        @Override
        public Optional<Integer> valueOfLiteral(String javaSource) {
            String trimmed = javaSource.strip();
            return trimmed.chars().allMatch(Character::isDigit) && !trimmed.isEmpty()
                    ? Optional.of(Integer.valueOf(trimmed))
                    : Optional.empty();
        }
    };

    /**
     * Writes a literal and reads none back.
     *
     * <p>Until 2026-09-20 this was eight of the seventeen registered types, because the reader had a
     * {@code default} answering empty. It is abstract now, so this codec has to say so explicitly — which is
     * the point of the change and does not make the state unreachable: declining a <em>particular</em>
     * source is still the honest answer for an initialiser a plugin never emits.
     */
    private static final ValueCodec<String> WRITE_ONLY = new ValueCodec<>() {
        @Override
        public String parse(String wire) {
            return wire == null ? "" : wire;
        }

        @Override
        public String store(String value) {
            return value;
        }

        @Override
        public String literal(String value) {
            return "Duration.parse(\"" + value + "\")";
        }

        @Override
        public Optional<String> valueOfLiteral(String javaSource) {
            return Optional.empty();
        }
    };

    private static ValueCatalog catalog() {
        return ValueCatalog.builder()
                .add(TEXT, TEXT_CODEC)
                .add(COUNT, COUNT_CODEC)
                .add(DURATION, WRITE_ONLY)
                .build();
    }

    private static final ValueForm TEXT_FORM = ValueForm.of(TEXT);
    private static final ValueForm COUNT_FORM = ValueForm.of(COUNT);

    // ---- writing -------------------------------------------------------------------------------------------

    @Test
    void aLeafIsItsCodecsLiteral() {
        assertEquals(Optional.of("\"hello\""), catalog().initializer(TEXT_FORM, "hello"));
    }

    @Test
    void aListIsTheFactoryCall() {
        assertEquals(Optional.of("java.util.List.of(\"a\", \"b\")"),
                catalog().initializer(ValueForm.listOf(TEXT_FORM), List.of("a", "b")));
        assertEquals(Optional.of("java.util.List.of()"),
                catalog().initializer(ValueForm.listOf(TEXT_FORM), List.of()));
    }

    @Test
    void aMapIsAlwaysOfEntriesAndNeverMapOf() {
        // One spelling means one rule each side and no behaviour change at the eleventh entry.
        Map<String, Integer> value = new LinkedHashMap<>();
        value.put("a", 1);
        value.put("b", 2);
        assertEquals(Optional.of("java.util.Map.ofEntries("
                        + "java.util.Map.entry(\"a\", 1), java.util.Map.entry(\"b\", 2))"),
                catalog().initializer(ValueForm.mapOf(TEXT_FORM, COUNT_FORM), value));
    }

    @Test
    void nestingIsJustTheWalkDoneAgain() {
        Map<String, List<Integer>> value = new LinkedHashMap<>();
        value.put("a", List.of(1, 2));
        assertEquals(Optional.of("java.util.Map.ofEntries(java.util.Map.entry(\"a\", "
                        + "java.util.List.of(1, 2)))"),
                catalog().initializer(ValueForm.mapOf(TEXT_FORM, ValueForm.listOf(COUNT_FORM)), value));
    }

    @Test
    void anUnknownTypeDeclinesRatherThanGuesses() {
        // A guess compiles into a user's bot, which is worse than showing them source nobody can edit.
        assertTrue(catalog().initializer(ValueForm.of(ValueType.unknown("discord.Channel")), "x").isEmpty());
        assertTrue(catalog().initializer(
                ValueForm.listOf(ValueForm.of(ValueType.unknown("discord.Channel"))), List.of("x")).isEmpty());
    }

    @Test
    void aClassTheBotDeclaresIsNotTheCatalogsToWrite() {
        assertTrue(catalog().initializer(new ValueForm.Declared("com.mybot.Box", List.of()), "x").isEmpty());
    }

    // ---- reading -------------------------------------------------------------------------------------------

    @Test
    void everyFormRoundTrips() {
        ValueCatalog catalog = catalog();
        Map<String, List<Integer>> nested = new LinkedHashMap<>();
        nested.put("a", List.of(1, 2));
        nested.put("b", List.of());

        for (Object[] pair : new Object[][]{
                {TEXT_FORM, "hello"},
                {ValueForm.listOf(TEXT_FORM), List.of("a", "b")},
                {ValueForm.listOf(ValueForm.listOf(COUNT_FORM)), List.of(List.of(1), List.of(2, 3))},
                {ValueForm.mapOf(TEXT_FORM, ValueForm.listOf(COUNT_FORM)), nested}}) {
            ValueForm form = (ValueForm) pair[0];
            Object value = pair[1];
            String written = catalog.initializer(form, value).orElseThrow();
            assertEquals(Optional.of(value), catalog.valueOf(form, written), written);
        }
    }

    @Test
    void aShorterSpellingReadsToo() {
        // A generated file writes java.util.List.of(…); a user's own file, having imported List, does not.
        assertEquals(Optional.of(List.of("a")),
                catalog().valueOf(ValueForm.listOf(TEXT_FORM), "List.of(\"a\")"));
        assertEquals(Optional.of(Map.of("a", 1)),
                catalog().valueOf(ValueForm.mapOf(TEXT_FORM, COUNT_FORM), "Map.ofEntries(Map.entry(\"a\", 1))"));
    }

    @Test
    void onePartTheCodecRefusesEmptiesTheWholeAnswer() {
        // With nesting this matters more, not less: a map with one unreadable value is shown whole and
        // untouched rather than silently losing an entry.
        ValueForm form = ValueForm.mapOf(TEXT_FORM, ValueForm.of(DURATION));
        String written = catalog()
                .initializer(form, Map.of("a", "PT3S"))
                .orElseThrow();
        assertTrue(written.contains("Duration.parse"));
        assertTrue(catalog().valueOf(form, written).isEmpty());
    }

    @Test
    void aSourceThisGrammarDidNotWriteAnswersEmpty() {
        ValueCatalog catalog = catalog();
        ValueForm list = ValueForm.listOf(TEXT_FORM);
        assertTrue(catalog.valueOf(list, "new ArrayList<>()").isEmpty());
        assertTrue(catalog.valueOf(list, "java.util.List.of(\"a\"").isEmpty(), "unbalanced");
        assertTrue(catalog.valueOf(list, "java.util.Set.of(\"a\")").isEmpty());
        // A map read as a list is not a partial success either.
        assertTrue(catalog.valueOf(list, "java.util.Map.ofEntries()").isEmpty());
    }

    @Test
    void aCommaInsideALiteralIsNotASplit() {
        assertEquals(Optional.of(List.of("a, b", "c")),
                catalog().valueOf(ValueForm.listOf(TEXT_FORM), "java.util.List.of(\"a, b\", \"c\")"));
    }

    @Test
    void aContainerNothingRegistersDeclinesBothWays() {
        // Reached when a form built against one catalog is read by another — a plugin uninstalled since.
        ValueContainer<List<?>> unregistered = new ValueContainer<>() {
            @Override
            public Class<?> type() {
                return java.util.Set.class;
            }

            @Override
            public int arity() {
                return 1;
            }

            @Override
            public String factory() {
                return "of";
            }

            @Override
            public List<Object> parts(List<?> value) {
                return List.copyOf(value);
            }

            @Override
            public List<?> build(List<Object> parts) {
                return List.copyOf(parts);
            }

            @Override
            public List<ValueForm> partForms(List<ValueForm> arguments, int parts) {
                return java.util.Collections.nCopies(parts, arguments.getFirst());
            }
        };
        ValueForm form = new ValueForm.Of(unregistered, List.of(TEXT_FORM));
        assertTrue(catalog().initializer(form, List.of("a")).isEmpty());
        assertTrue(catalog().valueOf(form, "java.util.Set.of(\"a\")").isEmpty());
    }

    // ---- imports -------------------------------------------------------------------------------------------

    @Test
    void importsComposeThroughTheWholeForm() {
        assertEquals(List.of("java.util.Map", "java.time.Duration"),
                catalog().imports(ValueForm.mapOf(ValueForm.of(DURATION), TEXT_FORM)));
        // A primitive and a type written fully qualified need none, and the list itself is still an import.
        assertEquals(List.of("java.util.List"), catalog().imports(ValueForm.listOf(COUNT_FORM)));
        assertEquals(List.of(), catalog().imports(TEXT_FORM));
    }
}
