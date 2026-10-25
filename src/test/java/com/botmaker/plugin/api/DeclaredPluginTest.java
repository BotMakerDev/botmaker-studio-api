package com.botmaker.plugin.api;

import com.botmaker.plugin.api.managed.ManagedMarker;
import com.botmaker.plugin.api.overlay.OverlayPart;
import com.botmaker.plugin.api.run.Readiness;
import com.botmaker.plugin.api.run.RunOverlayPart;
import com.botmaker.plugin.api.source.ManagedValue;
import com.botmaker.plugin.api.value.ComponentType;
import com.botmaker.plugin.api.value.DeclaredCall;
import com.botmaker.plugin.api.value.PluginType;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeclaredPluginTest {

    @ManagedMarker
    @Retention(RetentionPolicy.RUNTIME)
    public @interface NoteValue {

        Id value();

        enum Id { NOTE }
    }

    static final ManagedValue<String> NOTE = ManagedValue.method(NoteValue.Id.NOTE).in("Notes")
            .holds(String.class, "").because("Mine.");

    static final class Declared extends DeclaredPlugin {
        Declared(AtomicInteger asked) {
            super(StudioPlugin.id("com.example.notes").named("Notes")
                    .values(() -> {
                        asked.incrementAndGet();
                        return List.of(NOTE);
                    }));
        }
    }

    @Test
    void aSurfaceLeftOutContributesNothing() {
        Declared plugin = new Declared(new AtomicInteger());
        assertEquals("com.example.notes", plugin.id());
        assertEquals("Notes", plugin.displayName());
        assertTrue(plugin.types().isEmpty());
        assertTrue(plugin.componentTypes().isEmpty());
        assertTrue(plugin.slotEditors().isEmpty());
        assertTrue(plugin.toolbarItems().isEmpty());
        assertTrue(plugin.runOverlayParts().isEmpty());
        assertTrue(plugin.catalog().isEmpty());
    }

    @Test
    void runOverlayPartsAreDeclaredLikeTheToolbar() {
        RunOverlayPart part = RunOverlayPart.id("notes.run").bar(ctx -> null);
        StudioPlugin plugin = new DeclaredPlugin(StudioPlugin.id("com.example.z").named("Z")
                .runOverlay(() -> List.of(part))) {};
        assertEquals(List.of(part), plugin.runOverlayParts());
    }

    @Test
    void anOverlayPartIsAskedForOnlyWhenTheHostAsks() {
        AtomicInteger asked = new AtomicInteger();
        OverlayPart part = OverlayPart.of().targets(Runnable.class, "Home");
        StudioPlugin plugin = new DeclaredPlugin(StudioPlugin.id("com.example.o").named("O")
                .overlay(() -> {
                    asked.incrementAndGet();
                    return part;
                })) {};
        assertEquals(0, asked.get());
        assertEquals(Optional.of(part), plugin.overlayPart());
        assertEquals(1, asked.get());
        assertTrue(new Declared(new AtomicInteger()).overlayPart().isEmpty());
        assertTrue(new DeclaredPlugin(StudioPlugin.id("com.example.e").named("E")
                .overlay(OverlayPart::of)) {}.overlayPart().isEmpty(), "a part declaring nothing is no part");
    }

    @Test
    void nothingIsBuiltUntilTheHostAsks() {
        AtomicInteger asked = new AtomicInteger();
        Declared plugin = new Declared(asked);
        assertEquals(0, asked.get(), "constructing a plugin must build nothing");
        assertEquals(List.of(NOTE), plugin.managedValues());
        assertEquals(1, asked.get());
    }

    static Readiness windowFound() {
        return Readiness.ready(" game window found ");
    }

    static Readiness nothingToSay() {
        return null;
    }

    @Test
    void readinessIsAskedEachTimeAndNothingToSayIsEmpty() {
        assertTrue(new Declared(new AtomicInteger()).readiness().isEmpty());
        StudioPlugin ready = new DeclaredPlugin(StudioPlugin.id("com.example.r").named("R")
                .readiness(DeclaredPluginTest::windowFound)) {};
        assertEquals(Optional.of(Readiness.ready("game window found")), ready.readiness());
        assertEquals("Ready — game window found", ready.readiness().orElseThrow().line());
        assertTrue(new DeclaredPlugin(StudioPlugin.id("com.example.q").named("Q")
                .readiness(DeclaredPluginTest::nothingToSay)) {}.readiness().isEmpty());
        assertEquals("Not ready", Readiness.blocked("").line());
    }

    @Test
    void theAssistantsGuideIsTheDeclaredTextTrimmed() {
        assertEquals("", new Declared(new AtomicInteger()).assistantGuide());
        StudioPlugin plugin = new DeclaredPlugin(StudioPlugin.id("com.example.g").named("G")
                .assistantGuide("\n  One activity per chore.\n")) {};
        assertEquals("One activity per chore.", plugin.assistantGuide());
        assertEquals("", new DeclaredPlugin(StudioPlugin.id("com.example.n").named("N")
                .assistantGuide(null)) {}.assistantGuide());
    }

    @Test
    void aBlankNameIsTheId() {
        StudioPlugin plugin = new DeclaredPlugin(StudioPlugin.id("com.example.x").named(" ")) {};
        assertEquals("com.example.x", plugin.displayName());
        assertThrows(IllegalArgumentException.class, () -> StudioPlugin.id(" "));
    }

    public record Mark() {
        public static Mark make() {
            return new Mark();
        }
    }

    static final AtomicInteger LAZY_INITIALISED = new AtomicInteger();

    static final class Lazy {
        static {
            LAZY_INITIALISED.incrementAndGet();
        }

        public static final PluginType<Character> LETTER = PluginType.value(char.class).fresh(() -> 'a')
                .editor(() -> ctx -> null).writtenAsLiteral();
    }

    static final class Types {
        public static final PluginType<Integer> COUNT = PluginType.value(int.class).fresh(() -> 0)
                .editor(() -> ctx -> null).writtenAsLiteral();
        public static final DeclaredCall<Mark> MARK = ComponentType.part(Mark.class).writtenAs(Mark::make);
        public static final PluginType<String> LABEL = PluginType.value(String.class).fresh(() -> "")
                .editor(() -> ctx -> null).writtenAsLiteral();
        public static final List<DeclaredCall<Mark>> MARKS = List.of(
                ComponentType.part(Mark.class).writtenAs(Mark::make));
        public static final List<PluginType<?>> ALL = List.of(LABEL, COUNT);
        static final PluginType<Boolean> HIDDEN = PluginType.value(boolean.class).fresh(() -> false)
                .editor(() -> ctx -> null).writtenAsLiteral();
        public static final String NOT_A_TYPE = "x";
    }

    static final class Values {
        public static final ManagedValue<String> NOTE_TOO = NOTE;
        public static final List<ManagedValue<?>> ALL = List.of(NOTE);
    }

    @Test
    void aHolderClassGivesItsFieldsInDeclaredOrderOnceEach() {
        StudioPlugin plugin = new DeclaredPlugin(StudioPlugin.id("com.example.h").named("H")
                .types(Types.class).values(Values.class)) {};
        assertEquals(List.of(Types.COUNT, Types.LABEL), plugin.types(), "a type is not a part; ALL adds nothing");
        assertEquals(List.of(Types.MARK, Types.MARKS.getFirst()), plugin.componentTypes(),
                "a list field gives its elements");
        assertEquals(List.of(NOTE), plugin.managedValues());
    }

    @Test
    void aHolderIsInitialisedOnlyWhenTheHostAsks() {
        StudioPlugin plugin = new DeclaredPlugin(StudioPlugin.id("com.example.i").named("I")
                .types(Lazy.class)) {};
        assertEquals(0, LAZY_INITIALISED.get(), "naming a class must not initialise it");
        assertEquals(List.of(Lazy.LETTER), plugin.types());
        assertEquals(1, LAZY_INITIALISED.get());
    }

    @Test
    void aListAndAHolderAreBothKept() {
        PluginType<Long> big = PluginType.value(long.class).fresh(() -> 0L).editor(() -> ctx -> null)
                .writtenAsLiteral();
        StudioPlugin plugin = new DeclaredPlugin(StudioPlugin.id("com.example.j").named("J")
                .types(Types.class).types(() -> List.of(big, Types.COUNT))) {};
        assertEquals(List.of(big, Types.COUNT, Types.LABEL), plugin.types());
    }

    @Test
    void aHolderStepNeedsAClass() {
        IllegalArgumentException none = assertThrows(IllegalArgumentException.class,
                () -> StudioPlugin.id("com.example.k").named("K").types(new Class<?>[0]));
        assertTrue(none.getMessage().contains(".types(MyTypes.class)"), none.getMessage());
        assertThrows(IllegalArgumentException.class,
                () -> StudioPlugin.id("com.example.k").named("K").values((Class<?>) null));
    }

    @Test
    void aTypeListIsCopied() {
        StudioPlugin plugin = new DeclaredPlugin(StudioPlugin.id("com.example.y").named("Y")
                .types(() -> List.of(PluginType.value(int.class).fresh(() -> 0).editor(() -> ctx -> null)
                        .writtenAsLiteral()))) {};
        assertEquals(1, plugin.types().size());
        assertEquals(int.class, plugin.types().getFirst().type());
    }
}
