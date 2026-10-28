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
        static final RunOverlayPart PART = RunOverlayPart.id("notes.run").bar(ctx -> null);

        @Override
        public List<RunOverlayPart> runOverlay() {
            return List.of(PART);
        }
    }

    @Test
    void runOverlayPartsAreDeclaredLikeTheToolbar() {
        StudioPlugin plugin = new DeclaredPlugin(StudioPlugin.declare().runOverlay(RunOverlay.class)) {};
        assertEquals(List.of(RunOverlay.PART), plugin.runOverlayParts());
    }

    static final class Overlay implements OverlayHolder {
        static final OverlayPart PART = OverlayPart.of().targets(Runnable.class, "Home");

        @Override
        public OverlayPart overlay() {
            return PART;
        }
    }

    static final class NoOverlay implements OverlayHolder {
        @Override
        public OverlayPart overlay() {
            return OverlayPart.of();
        }
    }

    @Test
    void anOverlayHolderGivesItsOnePart() {
        assertEquals(Optional.of(Overlay.PART),
                new DeclaredPlugin(StudioPlugin.declare().overlay(Overlay.class)) {}.overlayPart());
        assertTrue(new DeclaredPlugin(StudioPlugin.declare().overlay(NoOverlay.class)) {}.overlayPart().isEmpty(),
                "a part declaring nothing is no part");
        IllegalStateException twice = assertThrows(IllegalStateException.class,
                () -> StudioPlugin.declare().overlay(Overlay.class).overlay(NoOverlay.class));
        assertTrue(twice.getMessage().contains("a plugin has one"), twice.getMessage());
    }

    static final class Catalog implements CatalogHolder {
        static final PaletteCatalog BY_HAND = PaletteCatalog.of(DeclaredPluginTest.class);

        @Override
        public PaletteCatalog catalog() {
            return BY_HAND;
        }
    }

    @Test
    void aCatalogHolderOverridesDiscovery() {
        assertSame(Catalog.BY_HAND, new DeclaredPlugin(StudioPlugin.declare().catalog(Catalog.class)) {}.catalog());
        assertThrows(IllegalStateException.class,
                () -> StudioPlugin.declare().catalog(Catalog.class).catalog(Catalog.class));
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

        static final PluginType<Character> LETTER = PluginType.value(char.class).fresh(() -> 'a')
                .editor(() -> ctx -> null).writtenAsLiteral();

        @Override
        public List<PluginType<?>> types() {
            return List.of(LETTER);
        }
    }

    static final class Types implements TypeHolder {
        static final PluginType<Integer> COUNT = PluginType.value(int.class).fresh(() -> 0)
                .editor(() -> ctx -> null).writtenAsLiteral();
        static final PluginType<String> LABEL = PluginType.value(String.class).fresh(() -> "")
                .editor(() -> ctx -> null).writtenAsLiteral();
        static final DeclaredCall<Mark> MARK = ComponentType.part(Mark.class).writtenAs(Mark::make);

        @Override
        public List<PluginType<?>> types() {
            return List.of(COUNT, LABEL, COUNT);
        }

        @Override
        public List<ComponentType<?>> components() {
            return List.of(MARK);
        }
    }

    static final class MoreTypes implements TypeHolder {
        static final PluginType<Long> BIG = PluginType.value(long.class).fresh(() -> 0L)
                .editor(() -> ctx -> null).writtenAsLiteral();

        @Override
        public List<PluginType<?>> types() {
            return List.of(BIG, Types.COUNT);
        }
    }

    static final class Values implements ValueHolder {
        @Override
        public List<ManagedValue<?>> values() {
            return List.of(NOTE);
        }
    }

    @Test
    void aHolderGivesWhatItsMethodAnswersOnceEach() {
        StudioPlugin plugin = new DeclaredPlugin(StudioPlugin.declare()
                .types(Types.class).values(Values.class)) {};
        assertEquals(List.of(Types.COUNT, Types.LABEL), plugin.types(), "a value given twice counts once");
        assertEquals(List.of(Types.MARK), plugin.componentTypes());
        assertEquals(List.of(NOTE), plugin.managedValues());
    }

    @Test
    void aHolderIsBuiltOnlyWhenTheHostAsks() {
        StudioPlugin plugin = new DeclaredPlugin(StudioPlugin.declare()
                .types(Lazy.class)) {};
        assertEquals(0, LAZY_INITIALISED.get(), "naming a class must not initialise it");
        assertEquals(List.of(Lazy.LETTER), plugin.types());
        assertEquals(1, LAZY_INITIALISED.get());
    }

    @Test
    void aStepCalledTwiceAddsItsHoldersInOrder() {
        StudioPlugin plugin = new DeclaredPlugin(StudioPlugin.declare()
                .types(Types.class).types(MoreTypes.class)) {};
        assertEquals(List.of(Types.COUNT, Types.LABEL, MoreTypes.BIG), plugin.types());
    }

    abstract static class Unbuildable implements TypeHolder {
    }

    final class Inner implements TypeHolder {
        @Override
        public List<PluginType<?>> types() {
            return List.of();
        }
    }

    static final class NeedsArgument implements TypeHolder {
        NeedsArgument(String unused) {
        }

        @Override
        public List<PluginType<?>> types() {
            return List.of();
        }
    }

    enum Enumerated implements TypeHolder {
        ONLY;

        @Override
        public List<PluginType<?>> types() {
            return List.of();
        }
    }

    static final class Throws implements TypeHolder {
        Throws() {
            throw new IllegalArgumentException("broken");
        }

        @Override
        public List<PluginType<?>> types() {
            return List.of();
        }
    }

    static final class GivesNull implements TypeHolder {
        @Override
        public List<PluginType<?>> types() {
            return null;
        }
    }

    /** No holder at all; {@code types()} alone would name the package-private accessor from this package. */
    @SuppressWarnings("unchecked")
    private static Class<? extends TypeHolder>[] noHolders() {
        return (Class<? extends TypeHolder>[]) new Class<?>[0];
    }

    @Test
    void aHolderStepRefusesWhatTheHostCouldNotBuild() {
        IllegalArgumentException none = assertThrows(IllegalArgumentException.class,
                () -> StudioPlugin.declare().types(noHolders()));
        assertTrue(none.getMessage().contains(".types(MyTypes.class)"), none.getMessage());
        assertThrows(IllegalArgumentException.class,
                () -> StudioPlugin.declare().values((Class<? extends ValueHolder>) null));
        assertThrows(IllegalArgumentException.class, () -> StudioPlugin.declare().overlay(null));
        assertThrows(IllegalArgumentException.class, () -> StudioPlugin.declare().types(Unbuildable.class));
        assertThrows(IllegalArgumentException.class, () -> StudioPlugin.declare().types(Inner.class));
        assertThrows(IllegalArgumentException.class, () -> StudioPlugin.declare().types(Enumerated.class));
        final class Local implements TypeHolder {
            @Override
            public List<PluginType<?>> types() {
                return List.of();
            }
        }
        assertThrows(IllegalArgumentException.class, () -> StudioPlugin.declare().types(Local.class));

        StudioPlugin plugin = new DeclaredPlugin(StudioPlugin.declare().types(NeedsArgument.class)) {};
        IllegalStateException refused = assertThrows(IllegalStateException.class, plugin::types);
        assertTrue(refused.getMessage().contains("needs a constructor with no arguments"), refused.getMessage());
        IllegalStateException broken = assertThrows(IllegalStateException.class,
                new DeclaredPlugin(StudioPlugin.declare().types(Throws.class)) {}::types);
        assertEquals("broken", broken.getCause().getMessage());
        assertTrue(new DeclaredPlugin(StudioPlugin.declare().types(GivesNull.class)) {}.types().isEmpty(),
                "a holder answering null gives nothing");
    }
}
