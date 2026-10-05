package com.botmaker.plugin.api.assist;

import com.botmaker.plugin.api.DeclaredPlugin;
import com.botmaker.plugin.api.StudioPlugin;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssistantToolTest {

    enum Shape { BOX, CIRCLE }

    record Crop(@Describe("the picture's name") String name, int x, long y, double scale, boolean keep,
                Shape shape, List<String> tags, @Describe(value = "how many", optional = true) Integer count) {
        Crop {
            if (name.isBlank()) throw new IllegalArgumentException("a name is needed");
        }
    }

    record Bad(java.awt.Point where) {
    }

    record OptionalPrimitive(@Describe(value = "n", optional = true) int n) {
    }

    static AgentReply crop(Crop crop, AgentContext context) {
        return AgentReply.text(crop.name() + " " + crop.x() + " " + crop.y() + " " + crop.scale() + " " + crop.keep()
                + " " + crop.shape() + " " + crop.tags() + " " + crop.count());
    }

    static AgentReply nothing(AssistantTool.None none, AgentContext context) {
        throw new IllegalStateException("no screen");
    }

    static final AssistantTool<Crop> CROP = AssistantTool.named("crop_picture")
            .describedAs("Cuts a picture out of the watched screen")
            .takes(Crop.class)
            .handledBy(AssistantToolTest::crop);

    @Test
    void theRecordIsTheSchema() {
        assertEquals("crop_picture", CROP.name());
        List<ToolParam> params = CROP.params();
        assertEquals(List.of("name", "x", "y", "scale", "keep", "shape", "tags", "count"),
                params.stream().map(ToolParam::name).toList());
        assertEquals(List.of(ToolParam.Kind.TEXT, ToolParam.Kind.WHOLE_NUMBER, ToolParam.Kind.WHOLE_NUMBER,
                        ToolParam.Kind.NUMBER, ToolParam.Kind.YES_NO, ToolParam.Kind.CHOICE, ToolParam.Kind.TEXT_LIST,
                        ToolParam.Kind.WHOLE_NUMBER),
                params.stream().map(ToolParam::kind).toList());
        assertEquals("the picture's name", params.getFirst().description());
        assertEquals(List.of("box", "circle"), params.get(5).choices());
        assertTrue(params.get(7).optional());
        assertFalse(params.get(0).optional());
        assertEquals("array", ToolParam.Kind.TEXT_LIST.jsonType());
    }

    @Test
    void aCallIsBuiltIntoTheRecord() {
        Map<String, Object> sent = new HashMap<>(Map.of("name", "ore", "x", 12, "y", 3.0, "scale", 2,
                "keep", "true", "shape", "Circle", "tags", List.of("a", "b")));
        AgentReply reply = CROP.invoke(sent, null);
        assertFalse(reply.isRefused());
        assertEquals("ore 12 3 2.0 true CIRCLE [a, b] null", reply.parts().getFirst().text());
    }

    @Test
    void aCallThatDoesNotFitIsRefusedNotThrown() {
        Map<String, Object> base = Map.of("name", "ore", "x", 1, "y", 1, "scale", 1, "keep", true, "shape", "box",
                "tags", List.of());
        assertRefused(without(base, "x"), "'x' is required");
        assertRefused(with(base, "x", 1.5), "'x' is a whole number");
        assertRefused(with(base, "x", 3_000_000_000L), "out of range");
        assertRefused(with(base, "y", 1e30), "'y' is a whole number");
        assertRefused(with(base, "y", new java.math.BigInteger("99999999999999999999")), "in range");
        assertFalse(CROP.invoke(with(base, "y", new java.math.BigInteger("9007199254740993")), null).isRefused());
        assertRefused(with(base, "shape", "star"), "one of [box, circle]");
        assertRefused(with(base, "tags", List.of(1)), "list of text");
        assertRefused(with(base, "name", 5), "'name' is text");
        assertRefused(with(base, "colour", "red"), "no parameter [colour]");
        assertRefused(with(base, "name", " "), "a name is needed");
    }

    @Test
    void aHandlerThatThrowsIsARefusal() {
        AssistantTool<AssistantTool.None> tool = AssistantTool.named("screenshot").describedAs("Looks")
                .takesNothing().handledBy(AssistantToolTest::nothing);
        assertTrue(tool.params().isEmpty());
        AgentReply reply = tool.invoke(null, null);
        assertTrue(reply.isRefused());
        assertTrue(reply.parts().getFirst().text().contains("no screen"));

        AssistantTool<AssistantTool.None> linking = AssistantTool.named("headless").describedAs("Links JavaFX")
                .takesNothing().handledBy(AssistantToolTest::linksMissing);
        assertTrue(linking.invoke(Map.of(), null).isRefused(), "an Error is the tool's failure too");
    }

    static AgentReply linksMissing(AssistantTool.None none, AgentContext context) {
        throw new NoClassDefFoundError("javafx/scene/Node");
    }

    @Test
    void aComponentTheSchemaCannotSayIsRefusedWhenDeclared() {
        IllegalArgumentException bad = assertThrows(IllegalArgumentException.class,
                () -> AssistantTool.named("bad").describedAs("x").takes(Bad.class).handledBy((p, c) -> null));
        assertTrue(bad.getMessage().contains("Bad.where"), bad.getMessage());
        assertThrows(IllegalArgumentException.class, () -> AssistantTool.named("opt").describedAs("x")
                .takes(OptionalPrimitive.class).handledBy((p, c) -> null));
        assertThrows(IllegalArgumentException.class, () -> AssistantTool.named("Crop Picture"));
        assertThrows(IllegalArgumentException.class, () -> AssistantTool.named("x").describedAs(" "));
    }

    @Test
    void aReplyChainsTextImagesAndRefusals() {
        BufferedImage pixel = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        AgentReply reply = AgentReply.text("found").and(AgentReply.image(pixel)).and(null);
        assertEquals(List.of(AgentReply.Kind.TEXT, AgentReply.Kind.IMAGE),
                reply.parts().stream().map(AgentReply.Part::kind).toList());
        byte[] png = reply.parts().get(1).png();
        assertArrayEquals(new byte[]{(byte) 0x89, 'P', 'N', 'G'}, java.util.Arrays.copyOf(png, 4));
        assertFalse(reply.isRefused());
        assertTrue(reply.and(AgentReply.refused("no")).isRefused());
        assertThrows(IllegalArgumentException.class, () -> AgentReply.image(new byte[0]));
    }

    /** Stands in for the SDK's {@code Bot}. */
    public static final class Entry {
        public static void trial(Runnable body, Class<?>... values) {
            body.run();
        }

        static void hidden(Runnable body, Class<?>... values) {
        }
    }

    @Test
    void theTrialEntryIsNamedAndCheckedOnlyWhenAsked() throws NoSuchMethodException {
        AtomicInteger asked = new AtomicInteger();
        StudioPlugin plugin = new DeclaredPlugin(StudioPlugin.id("com.example.t").named("T")
                .assistant(() -> {
                    asked.incrementAndGet();
                    return List.of(CROP);
                })
                .trial(Entry::trial)) {};
        assertEquals(0, asked.get());
        assertEquals(List.of(CROP), plugin.assistantTools());
        assertEquals(Entry.class.getMethod("trial", Runnable.class, Class[].class), plugin.trialEntry().orElseThrow());

        // Refused where it is declared, so the plugin's own tests catch it rather than the user's ▶ Try.
        assertThrows(IllegalArgumentException.class,
                () -> StudioPlugin.id("com.example.h").named("H").trial(Entry::hidden));
        assertThrows(IllegalArgumentException.class,
                () -> StudioPlugin.id("com.example.l").named("L").trial((body, values) -> body.run()));
        assertTrue(new DeclaredPlugin(StudioPlugin.id("com.example.n").named("N")) {}.trialEntry().isEmpty());
    }

    private static void assertRefused(Map<String, Object> sent, String says) {
        AgentReply reply = CROP.invoke(sent, null);
        assertTrue(reply.isRefused(), sent.toString());
        assertTrue(reply.parts().getFirst().text().contains(says), reply.parts().getFirst().text());
    }

    private static Map<String, Object> with(Map<String, Object> base, String key, Object value) {
        Map<String, Object> copy = new HashMap<>(base);
        copy.put(key, value);
        return copy;
    }

    private static Map<String, Object> without(Map<String, Object> base, String key) {
        Map<String, Object> copy = new HashMap<>(base);
        copy.remove(key);
        return copy;
    }
}
