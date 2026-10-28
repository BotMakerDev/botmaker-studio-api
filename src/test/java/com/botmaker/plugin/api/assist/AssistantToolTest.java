package com.botmaker.plugin.api.assist;

import com.botmaker.plugin.api.AssistantHolder;
import com.botmaker.plugin.api.DeclaredPlugin;
import com.botmaker.plugin.api.StudioPlugin;
import com.botmaker.plugin.api.assist.ToolSchema.Field;
import com.botmaker.plugin.api.assist.ToolSchema.Field.Kind;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        List<Field> params = CROP.schema().fields();
        assertEquals(List.of("name", "x", "y", "scale", "keep", "shape", "tags", "count"),
                params.stream().map(Field::name).toList());
        assertEquals(List.of(Kind.TEXT, Kind.WHOLE_NUMBER, Kind.WHOLE_NUMBER,
                        Kind.NUMBER, Kind.YES_NO, Kind.CHOICE, Kind.TEXT_LIST,
                        Kind.WHOLE_NUMBER),
                params.stream().map(Field::kind).toList());
        assertEquals("the picture's name", params.getFirst().description());
        assertEquals(List.of("box", "circle"), params.get(5).choices());
        assertTrue(params.get(7).optional());
        assertFalse(params.get(0).optional());
        assertEquals("array", Kind.TEXT_LIST.jsonType());
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
        assertRefused(with(base, "name", List.of("ore")), "'name' is text");
        assertRefused(with(base, "colour", "red"), "no parameter [colour]");
        assertRefused(with(base, "name", " "), "a name is needed");
    }

    @Test
    void aHandlerThatThrowsIsARefusal() {
        AssistantTool<AssistantTool.None> tool = AssistantTool.named("screenshot").describedAs("Looks")
                .takesNothing().handledBy(AssistantToolTest::nothing);
        assertTrue(tool.schema().fields().isEmpty());
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

    static final class Tools implements AssistantHolder {
        @Override
        public List<AssistantTool<?>> tools() {
            return List.of(CROP);
        }
    }

    @Test
    void theTrialEntryIsNamedAndCheckedOnlyWhenAsked() throws NoSuchMethodException {
        StudioPlugin plugin = new DeclaredPlugin(StudioPlugin.declare()
                .assistant(Tools.class)
                .trial(Entry::trial)) {};
        assertEquals(List.of(CROP), plugin.assistantTools());
        assertEquals(Entry.class.getMethod("trial", Runnable.class, Class[].class), plugin.trialEntry().orElseThrow());

        // Refused where it is declared, so the plugin's own tests catch it rather than the user's ▶ Try.
        assertThrows(IllegalArgumentException.class,
                () -> StudioPlugin.declare().trial(Entry::hidden));
        assertThrows(IllegalArgumentException.class,
                () -> StudioPlugin.declare().trial((body, values) -> body.run()));
        assertTrue(new DeclaredPlugin(StudioPlugin.declare()) {}.trialEntry().isEmpty());
    }

    record Box(@Describe("its name") String name, int x, @Describe(value = "skipped", optional = true) Integer w) {
    }

    record Crops(Box first, List<Box> boxes, List<Shape> shapes, @Describe(value = "values", optional = true)
            Map<String, String> values) {
    }

    static AgentReply crops(Crops crops, AgentContext context) {
        return AgentReply.text(crops.toString());
    }

    static final AssistantTool<Crops> CROPS = AssistantTool.named("crop_pictures").describedAs("Cuts several")
            .takes(Crops.class).handledBy(AssistantToolTest::crops);

    @Test
    void nestedRecordsListsAndMapsAreTheSchemaToo() {
        ToolSchema schema = CROPS.schema();
        assertTrue(schema.actions().isEmpty());
        assertEquals(List.of(Kind.RECORD, Kind.RECORD_LIST, Kind.CHOICE_LIST,
                Kind.TEXT_MAP), schema.fields().stream().map(ToolSchema.Field::kind).toList());
        ToolSchema.Field boxes = schema.fields().get(1);
        assertEquals(List.of("name", "x", "w"), boxes.fields().stream().map(ToolSchema.Field::name).toList());
        assertEquals("its name", boxes.fields().getFirst().description());
        assertTrue(boxes.fields().get(2).optional());
        assertEquals(List.of("box", "circle"), schema.fields().get(2).choices());

        Map<String, Object> sent = Map.of("first", Map.of("name", "a", "x", 1),
                "boxes", List.of(Map.of("name", "b", "x", 2, "w", 3)), "shapes", List.of("Box", "circle"),
                "values", Map.of("speed", 2));
        AgentReply reply = CROPS.invoke(sent, null);
        assertFalse(reply.isRefused(), reply.parts().getFirst().text());
        assertEquals("Crops[first=Box[name=a, x=1, w=null], boxes=[Box[name=b, x=2, w=3]], shapes=[BOX, CIRCLE],"
                + " values={speed=2}]", reply.parts().getFirst().text());

        assertRefusedBy(CROPS, with(sent, "boxes", List.of(Map.of("x", 2))), "'boxes[0].name' is required");
        assertRefusedBy(CROPS, with(sent, "boxes", List.of(Map.of("name", "b", "x", "two"))),
                "'boxes[0].x' is a whole number");
        assertRefusedBy(CROPS, with(sent, "first", Map.of("name", "a", "x", 1, "z", 0)),
                "'first' has no field [z]; it takes [name, x, w]");
        assertRefusedBy(CROPS, with(sent, "first", "a 1"), "'first' is an object of [name, x, w]");
        assertRefusedBy(CROPS, with(sent, "shapes", List.of("star")), "one of [box, circle]");
        assertRefusedBy(CROPS, with(sent, "values", Map.of("speed", List.of())), "is text");
        assertRefusedBy(CROPS, with(sent, "shapes", java.util.Arrays.asList("box", null)), "not null");
        assertFalse(CROPS.invoke(with(sent, "unsent", null), null).isRefused(), "a key sent as null is left out");
    }

    /** What a screen map edit does, one record per action. */
    sealed interface MapEdit {
        @Describe("Renames a screen")
        record Rename(@Describe("the screen") String screen, @Describe("its new name") String to) implements MapEdit {
        }

        record Delete(String screen, @Describe(value = "even if used", optional = true) Boolean force)
                implements MapEdit {
        }

        record SetStart(String screen) implements MapEdit {
        }
    }

    static AgentReply edit(MapEdit edit, AgentContext context) {
        return AgentReply.text(switch (edit) {
            case MapEdit.Rename r -> "rename " + r.screen() + " " + r.to();
            case MapEdit.Delete d -> "delete " + d.screen() + " " + d.force();
            case MapEdit.SetStart s -> "start " + s.screen();
        });
    }

    static final AssistantTool<AssistantTool.OneOf<MapEdit>> EDIT_MAP = AssistantTool.named("edit_map")
            .describedAs("Edits the screen map").takesOneOf(MapEdit.class).handledBy(AssistantToolTest::edit);

    @Test
    void anActionToolIsFlatOnTheWireAndSealedInJava() {
        ToolSchema schema = EDIT_MAP.schema();
        assertEquals(List.of("action", "screen", "to", "force"),
                schema.fields().stream().map(ToolSchema.Field::name).toList());
        ToolSchema.Field action = schema.fields().getFirst();
        assertEquals(Kind.CHOICE, action.kind());
        assertFalse(action.optional());
        assertEquals(List.of("rename", "delete", "set_start"), action.choices());
        assertTrue(schema.fields().stream().skip(1).allMatch(ToolSchema.Field::optional));
        assertEquals(List.of(), schema.fields().get(1).actions(), "every action takes the screen");
        assertEquals(List.of("rename"), schema.fields().get(2).actions());
        assertEquals("the screen", schema.fields().get(1).description(), "the first sentence said wins");
        ToolSchema.Action rename = schema.actions().getFirst();
        assertEquals("Renames a screen", rename.description());
        assertEquals(List.of("screen", "to"), rename.required());
        assertEquals(List.of("screen", "force"), schema.actions().get(1).fields());
        assertEquals(List.of("screen"), schema.actions().get(1).required());
        assertEquals(AssistantTool.OneOf.class, EDIT_MAP.paramsType());

        assertEquals("rename a b", EDIT_MAP.invoke(Map.of("action", "rename", "screen", "a", "to", "b"), null)
                .parts().getFirst().text());
        assertEquals("delete a null", EDIT_MAP.invoke(Map.of("action", "Delete", "screen", "a"), null)
                .parts().getFirst().text());
        assertEquals("start a", EDIT_MAP.invoke(Map.of("action", "set_start", "screen", "a"), null)
                .parts().getFirst().text());

        assertRefusedBy(EDIT_MAP, Map.of("screen", "a"), "'action' is required: one of [rename, delete, set_start]");
        assertRefusedBy(EDIT_MAP, Map.of("action", "move", "screen", "a"), "'action' is one of [rename, delete,"
                + " set_start], not move");
        assertRefusedBy(EDIT_MAP, Map.of("action", "rename", "screen", "a"), "'to' is required for rename");
        assertRefusedBy(EDIT_MAP, Map.of("action", "set_start", "screen", "a", "to", "b"),
                "set_start does not take [to]; it takes [screen]");
        assertRefusedBy(EDIT_MAP, Map.of("action", "rename", "screen", "a", "to", "b", "colour", "red"),
                "no parameter [colour]; it takes [action, screen, to, force]");
        Map<String, Object> nulls = new HashMap<>(Map.of("action", "set_start", "screen", "a"));
        nulls.put("to", null);
        nulls.put("bogus", null);
        assertEquals("start a", EDIT_MAP.invoke(nulls, null).parts().getFirst().text());
    }

    sealed interface Step {
        @Describe("Inserts a block")
        record Insert(@Describe("the body") String bodyId, int index) implements Step {
        }

        record Delete(String blockId) implements Step {
        }
    }

    record Steps(String file, List<Step> edits) {
    }

    static final AssistantTool<Steps> STEPS = AssistantTool.named("steps").describedAs("Edits in one go")
            .takes(Steps.class).handledBy((steps, context) -> AgentReply.text(steps.edits().stream().map(step ->
                    switch (step) {
                        case Step.Insert insert -> "insert " + insert.bodyId() + "@" + insert.index();
                        case Step.Delete delete -> "delete " + delete.blockId();
                    }).toList().toString()));

    @Test
    void aListOfActionsIsFlatElementByElement() {
        ToolSchema.Field edits = STEPS.schema().fields().get(1);
        assertEquals(Kind.RECORD_LIST, edits.kind());
        assertEquals(List.of("action", "bodyId", "index", "blockId"),
                edits.fields().stream().map(ToolSchema.Field::name).toList());
        assertEquals(List.of("insert", "delete"), edits.oneOf().stream().map(ToolSchema.Action::id).toList());
        assertEquals("Inserts a block", edits.oneOf().getFirst().description());
        assertEquals(List.of("insert"), edits.fields().get(1).actions());
        assertEquals(List.of(), STEPS.schema().actions(), "the tool itself takes no action");

        assertEquals("[insert b@2, delete x]", STEPS.invoke(Map.of("file", "F", "edits", List.of(
                Map.of("action", "insert", "bodyId", "b", "index", 2),
                Map.of("action", "delete", "blockId", "x"))), null).parts().getFirst().text());
        assertRefusedBy(STEPS, Map.of("file", "F", "edits", List.of(Map.of("bodyId", "b"))),
                "'edits[0].action' is required: one of [insert, delete]");
        assertRefusedBy(STEPS, Map.of("file", "F", "edits", List.of(Map.of("action", "delete", "blockId", "x"),
                Map.of("action", "insert", "bodyId", "b"))), "'edits[1].index' is required for insert");
        assertRefusedBy(STEPS, Map.of("file", "F", "edits", List.of(Map.of("action", "delete", "blockId", "x",
                "index", 1))), "'edits[0]': delete does not take [index]; it takes [blockId]");
        assertRefusedBy(STEPS, Map.of("file", "F", "edits", List.of(Map.of("action", "delete", "colour", "red"))),
                "'edits[0]' has no field [colour]; it takes [action, bodyId, index, blockId]");
    }

    record Keyword(@Describe("the class") @Key("class") String className,
                   @Describe(value = "seconds", optional = true) @Key("wait") Integer seconds) {
    }

    record Twice(String a, @Key("a") String b) {
    }

    @Test
    void aComponentIsSentUnderItsKey() {
        AssistantTool<Keyword> tool = AssistantTool.named("k").describedAs("k").takes(Keyword.class)
                .handledBy((k, c) -> AgentReply.text(k.className() + " " + k.seconds()));
        assertEquals(List.of("class", "wait"), tool.schema().fields().stream().map(ToolSchema.Field::name).toList());
        assertEquals("Main 3", tool.invoke(Map.of("class", "Main", "wait", 3), null).parts().getFirst().text());
        assertRefusedBy(tool, Map.of("className", "Main"), "'class' is required");
        assertDeclarationRefused(() -> AssistantTool.named("t").describedAs("t").takes(Twice.class)
                .handledBy((p, c) -> null), "two components are sent as 'a'");
        assertDeclarationRefused(() -> AssistantTool.named("t").describedAs("t").takes(BlankKey.class)
                .handledBy((p, c) -> null), "@Key is a snake_case key, not ''");
    }

    record BlankKey(@Key("") String a) {
    }

    sealed interface Nest {
        record Deeper(List<Nest> inner) implements Nest {
        }
    }

    record Nested(List<Nest> nest) {
    }

    record Lenient(String value, @Describe(value = "a shape", optional = true) Shape shape,
                   @Describe(value = "values", optional = true) Map<String, String> values) {
    }

    @Test
    void aBareLiteralIsTextAndAnEmptyOptionalChoiceIsLeftOut() {
        AssistantTool<Lenient> tool = AssistantTool.named("l").describedAs("l").takes(Lenient.class)
                .handledBy((l, c) -> AgentReply.text(l.value() + " " + l.shape() + " " + l.values()));
        assertEquals("5 null null", tool.invoke(Map.of("value", 5, "shape", ""), null).parts().getFirst().text());
        assertEquals("true BOX null", tool.invoke(Map.of("value", true, "shape", "box"), null).parts().getFirst()
                .text());
        Map<String, Object> blank = new HashMap<>();
        blank.put("x", null);
        assertEquals("a null {x=}", tool.invoke(Map.of("value", "a", "values", blank), null).parts().getFirst()
                .text());
        assertDeclarationRefused(() -> AssistantTool.named("n").describedAs("n").takes(Nested.class)
                .handledBy((p, c) -> null), "holds itself");
    }

    sealed interface Clashing {
        record One(String screen) implements Clashing {
        }

        record Two(int screen) implements Clashing {
        }
    }

    sealed interface Keyed {
        record One(String action) implements Keyed {
        }
    }

    interface Open {
    }

    record Tree(String name, List<Tree> children) {
    }

    @Test
    void aShapeTheWireCannotSayIsRefusedWhenDeclared() {
        assertDeclarationRefused(() -> AssistantTool.named("x").describedAs("x").takesOneOf(Clashing.class),
                "Two.screen holds what another action's 'screen' does not");
        assertDeclarationRefused(() -> AssistantTool.named("x").describedAs("x").takesOneOf(Keyed.class),
                "'action' is the action's own key");
        assertDeclarationRefused(() -> AssistantTool.named("x").describedAs("x").takesOneOf(Open.class),
                "a sealed interface of records");
        assertDeclarationRefused(() -> AssistantTool.named("x").describedAs("x").takes(Tree.class)
                .handledBy((p, c) -> null), "holds itself");
    }

    private static void assertDeclarationRefused(Runnable declare, String says) {
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class, declare::run);
        assertTrue(refused.getMessage().contains(says), refused.getMessage());
    }

    private static void assertRefusedBy(AssistantTool<?> tool, Map<String, Object> sent, String says) {
        AgentReply reply = tool.invoke(sent, null);
        assertTrue(reply.isRefused(), sent.toString());
        assertTrue(reply.parts().getFirst().text().contains(says), reply.parts().getFirst().text());
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
