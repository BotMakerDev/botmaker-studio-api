package com.botmaker.plugin.api;

import com.botmaker.plugin.api.catalog.PaletteCatalog;
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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
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

    static final StudioPlugin EMPTY = new DeclaredPlugin(StudioPlugin.declare()) {};

    @Test
    void aSurfaceLeftOutContributesNothing() {
        assertTrue(EMPTY.types().isEmpty());
        assertTrue(EMPTY.componentTypes().isEmpty());
        assertTrue(EMPTY.slotEditors().isEmpty());
        assertTrue(EMPTY.toolbarItems().isEmpty());
        assertTrue(EMPTY.runOverlayParts().isEmpty());
        assertTrue(EMPTY.overlayPart().isEmpty());
        assertTrue(EMPTY.catalog().isEmpty());
        EMPTY.projectOpened(null);
        EMPTY.projectClosing();
    }

    static final class RunOverlay implements RunOverlayHolder {
        public static final RunOverlayPart PART = RunOverlayPart.id("notes.run").bar(ctx -> null);
    }

    @Test
    void runOverlayPartsAreDeclaredLikeTheToolbar() {
        StudioPlugin plugin = new DeclaredPlugin(StudioPlugin.declare().runOverlay(RunOverlay.class)) {};
        assertEquals(List.of(RunOverlay.PART), plugin.runOverlayParts());
    }

    static final class Overlay implements OverlayHolder {
        public static final OverlayPart PART = OverlayPart.of().targets(Runnable.class, "Home");
    }

    static final class NoOverlay implements OverlayHolder {
        public static final OverlayPart PART = OverlayPart.of();
    }

    static final class TwoOverlays implements OverlayHolder {
        public static final OverlayPart ONE = OverlayPart.of().targets(Runnable.class, "Home");
        public static final OverlayPart TWO = OverlayPart.of().targets(Runnable.class, "Away");
    }

    @Test
    void anOverlayHolderGivesItsOnePart() {
        assertEquals(Optional.of(Overlay.PART),
                new DeclaredPlugin(StudioPlugin.declare().overlay(Overlay.class)) {}.overlayPart());
        assertTrue(new DeclaredPlugin(StudioPlugin.declare().overlay(NoOverlay.class)) {}.overlayPart().isEmpty(),
                "a part declaring nothing is no part");
        StudioPlugin two = new DeclaredPlugin(StudioPlugin.declare().overlay(TwoOverlays.class)) {};
        IllegalStateException refused = assertThrows(IllegalStateException.class, two::overlayPart);
        assertTrue(refused.getMessage().contains("TwoOverlays holds 2 OverlayPart fields"), refused.getMessage());
    }

    static final class Catalog implements CatalogHolder {
        public static final PaletteCatalog BY_HAND = PaletteCatalog.of(DeclaredPluginTest.class);
    }

    @Test
    void aCatalogHolderOverridesDiscovery() {
        assertSame(Catalog.BY_HAND, new DeclaredPlugin(StudioPlugin.declare().catalog(Catalog.class)) {}.catalog());
    }

    static Readiness windowFound() {
        return Readiness.ready(" game window found ");
    }

    static Readiness nothingToSay() {
        return null;
    }

    @Test
    void readinessIsAskedEachTimeAndNothingToSayIsEmpty() {
        assertTrue(EMPTY.readiness().isEmpty());
        StudioPlugin ready = new DeclaredPlugin(StudioPlugin.declare()
                .readiness(DeclaredPluginTest::windowFound)) {};
        assertEquals(Optional.of(Readiness.ready("game window found")), ready.readiness());
        assertEquals("Ready — game window found", ready.readiness().orElseThrow().line());
        assertTrue(new DeclaredPlugin(StudioPlugin.declare()
                .readiness(DeclaredPluginTest::nothingToSay)) {}.readiness().isEmpty());
        assertEquals("Not ready", Readiness.blocked("").line());
    }

    @Test
    void theAssistantsGuideIsTheDeclaredTextTrimmed() {
        assertEquals("", EMPTY.assistantGuide());
        StudioPlugin plugin = new DeclaredPlugin(StudioPlugin.declare()
                .assistantGuide("\n  One activity per chore.\n")) {};
        assertEquals("One activity per chore.", plugin.assistantGuide());
        assertEquals("", new DeclaredPlugin(StudioPlugin.declare()
                .assistantGuide(null)) {}.assistantGuide());
    }

    static final List<String> TOLD = new ArrayList<>();

    static void opened(StudioServices services) {
        TOLD.add("opened " + services.projectDir());
    }

    static void closing() {
        TOLD.add("closing");
    }

    @Test
    void theLifecycleIsDeclaredAndCannotBeOverridden() throws NoSuchMethodException {
        StudioPlugin plugin = new DeclaredPlugin(StudioPlugin.declare()
                .onProjectOpened(DeclaredPluginTest::opened)
                .onProjectClosing(DeclaredPluginTest::closing)) {};
        TOLD.clear();
        plugin.projectOpened(new StudioServices() {
            public Path projectDir() {
                return Path.of("bot");
            }

            public Path resourcesDir() {
                return Path.of("bot", "resources");
            }

            public Theme theme() {
                return null;
            }

            public Dialogs dialogs() {
                return null;
            }
        });
        plugin.projectClosing();
        assertEquals(List.of("opened bot", "closing"), TOLD);
        for (Method method : List.of(DeclaredPlugin.class.getMethod("projectOpened", StudioServices.class),
                DeclaredPlugin.class.getMethod("projectClosing"))) {
            assertTrue(Modifier.isFinal(method.getModifiers()), method + " must be final: declare it instead");
        }
    }

    @Test
    void aPluginIsADeclaredPlugin() {
        assertTrue(StudioPlugin.class.isSealed());
        assertEquals(List.of(DeclaredPlugin.class), List.of(StudioPlugin.class.getPermittedSubclasses()));
    }

    public record Mark() {
        public static Mark make() {
            return new Mark();
        }
    }

    static final AtomicInteger LAZY_INITIALISED = new AtomicInteger();

    static final class Lazy implements TypeHolder {
        static {
            LAZY_INITIALISED.incrementAndGet();
        }

        public static final PluginType<Character> LETTER = PluginType.value(char.class).fresh(() -> 'a')
                .editor(() -> ctx -> null).writtenAsLiteral();
    }

    static final class Types implements TypeHolder {
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

    static final class MoreTypes implements TypeHolder {
        public static final PluginType<Long> BIG = PluginType.value(long.class).fresh(() -> 0L)
                .editor(() -> ctx -> null).writtenAsLiteral();
        public static final PluginType<Integer> COUNT_TOO = Types.COUNT;
    }

    static final class Values implements ValueHolder {
        public static final ManagedValue<String> NOTE_TOO = NOTE;
        public static final List<ManagedValue<?>> ALL = List.of(NOTE);
    }

    @Test
    void aHolderClassGivesItsFieldsInDeclaredOrderOnceEach() {
        StudioPlugin plugin = new DeclaredPlugin(StudioPlugin.declare()
                .types(Types.class).values(Values.class)) {};
        assertEquals(List.of(Types.COUNT, Types.LABEL), plugin.types(), "a type is not a part; ALL adds nothing");
        assertEquals(List.of(Types.MARK, Types.MARKS.getFirst()), plugin.componentTypes(),
                "a list field gives its elements");
        assertEquals(List.of(NOTE), plugin.managedValues());
    }

    @Test
    void aHolderIsInitialisedOnlyWhenTheHostAsks() {
        StudioPlugin plugin = new DeclaredPlugin(StudioPlugin.declare()
                .types(Lazy.class)) {};
        assertEquals(0, LAZY_INITIALISED.get(), "naming a class must not initialise it");
        assertEquals(List.of(Lazy.LETTER), plugin.types());
        assertEquals(1, LAZY_INITIALISED.get());
    }

    @Test
    void aStepCalledTwiceAddsItsHolders() {
        StudioPlugin plugin = new DeclaredPlugin(StudioPlugin.declare()
                .types(Types.class).types(MoreTypes.class)) {};
        assertEquals(List.of(Types.COUNT, Types.LABEL, MoreTypes.BIG), plugin.types());
    }

    /** No holder at all; {@code types()} alone would name the package-private accessor from this package. */
    @SuppressWarnings("unchecked")
    private static Class<? extends TypeHolder>[] noHolders() {
        return (Class<? extends TypeHolder>[]) new Class<?>[0];
    }

    @Test
    void aHolderStepNeedsAClass() {
        IllegalArgumentException none = assertThrows(IllegalArgumentException.class,
                () -> StudioPlugin.declare().types(noHolders()));
        assertTrue(none.getMessage().contains(".types(MyTypes.class)"), none.getMessage());
        assertThrows(IllegalArgumentException.class,
                () -> StudioPlugin.declare().values((Class<? extends ValueHolder>) null));
        assertThrows(IllegalArgumentException.class, () -> StudioPlugin.declare().overlay(null));
    }
}
