package com.botmaker.plugin.api.overlay;

import com.botmaker.plugin.api.run.RunOverlayContext;
import com.botmaker.plugin.api.toolbar.ActionContext.Area;
import com.botmaker.plugin.api.value.Ref;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Executable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverlayPartTest {

    /**
     * Stands in for a plugin's calls a bot writes, overloaded as the SDK's are: {@code find} by arity
     * ({@code ImageFinder.find}), {@code click} by type at one arity ({@code ImageClicker.click}).
     */
    public static final class Finder {
        public static Boolean find(String picture) {
            return Boolean.TRUE;
        }

        public static Boolean find(String picture, Integer within) {
            return Boolean.TRUE;
        }

        public static Boolean near(String picture, Integer distance) {
            return Boolean.TRUE;
        }

        public static Boolean click(String picture) {
            return Boolean.TRUE;
        }

        public static Boolean click(Integer match) {
            return Boolean.TRUE;
        }

        public static void press(String key) {
        }

        public static void press(String key, Integer times) {
        }
    }

    static ProbeResult found(ProbeContext context) {
        return ProbeResult.found("found", new Area(1, 2, 3, 4));
    }

    static ProbeResult missing(ProbeContext context) {
        return ProbeResult.missing("not there");
    }

    static final OverlayTool PICTURE = OverlayTool.id("x.picture").named("Picture").pane(ctx -> null);

    @Test
    void aPartAnswersWhatItDeclared() {
        OverlayPart part = OverlayPart.of()
                .targets(Body.class, "Activities")
                .watched(ctx -> Optional.of(Watched.window("Game")))
                .changeWatched(() -> ctx -> { })
                .tool(PICTURE)
                .probe(Finder::find, String.class, OverlayPartTest::found)
                .probe(Finder::near, String.class, Integer.class, OverlayPartTest::missing);

        assertFalse(part.isEmpty());
        assertEquals("Activities", part.targets().getFirst().group());
        assertEquals(Optional.of(Watched.window("Game")), part.watchedFor(null));
        assertTrue(part.changeWatched().isPresent());
        assertEquals(List.of(PICTURE), part.tools());
        assertEquals(2, part.probes().size());
        assertEquals("find", part.probes().getFirst().call().getName());
        assertEquals(1, part.probes().getFirst().call().getParameterCount());

        Executable near = Arrays.stream(Finder.class.getMethods()).filter(m -> m.getName().equals("near"))
                .findFirst().orElseThrow();
        assertEquals(ProbeResult.State.MISSING, part.probeFor(near).orElseThrow().probe(null).state());
        assertTrue(part.probeFor(Ref.member(String.class, "length")).isEmpty());
    }

    /** The two shapes a bare reference could not name, found in review: overloads by arity and by type. */
    @Test
    void parameterClassesPickAnOverloadedCall() throws NoSuchMethodException {
        OverlayPart part = OverlayPart.of()
                .probe(Finder::find, String.class, Integer.class, OverlayPartTest::found)
                .probe(Finder::click, Integer.class, OverlayPartTest::missing);

        assertEquals(Finder.class.getMethod("find", String.class, Integer.class), part.probes().get(0).call());
        assertEquals(Finder.class.getMethod("click", Integer.class), part.probes().get(1).call());
        assertTrue(part.probeFor(Finder.class.getMethod("click", String.class)).isEmpty());
    }

    @Test
    void aProbeReadsOnlyUnlessDeclaredActing() {
        Probe reads = OverlayPartTest::found;
        Probe acts = Probe.acting(OverlayPartTest::found);
        assertTrue(reads.readsOnly());
        assertFalse(acts.readsOnly());
        assertEquals(ProbeResult.State.FOUND, acts.probe(null).state());
        assertThrows(IllegalArgumentException.class, () -> Probe.acting(null));
    }

    @Test
    void anEmptyPartAnswersNothingAndNullAnswersAreNothing() {
        assertTrue(OverlayPart.of().isEmpty());
        assertEquals(List.of(), OverlayPart.of().targets());
        OverlayPart careless = OverlayPart.of().watched(ctx -> null);
        assertEquals(Optional.empty(), careless.watchedFor(null));
    }

    @Test
    void stepsAnswerCopies() {
        OverlayPart empty = OverlayPart.of();
        OverlayPart withTool = empty.tool(PICTURE);
        assertTrue(empty.tools().isEmpty());
        assertSame(PICTURE, withTool.tools().getFirst());
    }

    @Test
    void aCallProbedTwiceAndAToolIdUsedTwiceAreRefused() {
        OverlayPart part = OverlayPart.of().probe(Finder::find, String.class, OverlayPartTest::found).tool(PICTURE);
        assertThrows(IllegalArgumentException.class,
                () -> part.probe(Finder::find, String.class, OverlayPartTest::missing));
        assertThrows(IllegalArgumentException.class,
                () -> part.tool(OverlayTool.id("x.picture").named("Again").pane(ctx -> null)));
    }

    @Test
    void aLambdaIsNotACallTheHostCanName() {
        Ref.Of1<String, Boolean> lambda = picture -> Boolean.TRUE;
        assertThrows(IllegalArgumentException.class, () -> OverlayPart.of().probe(lambda, String.class, OverlayPartTest::found));
    }

    /** A tool inserts an overloaded call too: the argument's type picks it. */
    @Test
    void aToolInsertsTheCallItNames() throws NoSuchMethodException {
        java.util.List<Object> inserted = new java.util.ArrayList<>();
        OverlayToolContext context = new OverlayToolContext() {
            @Override public com.botmaker.plugin.api.StudioServices services() { return null; }
            @Override public Optional<java.awt.image.BufferedImage> frame() { return Optional.empty(); }
            @Override public Optional<Area> watchedArea() { return Optional.empty(); }
            @Override public java.util.concurrent.CompletionStage<Optional<Area>> pickRegion(String prompt) { return null; }
            @Override public java.util.concurrent.CompletionStage<Optional<Pixel>> pickPoint(String prompt) { return null; }
            @Override public void onClosed(Runnable action) { }

            @Override
            public Optional<String> insertMember(Executable call, Object... arguments) {
                inserted.add(call);
                inserted.addAll(List.of(arguments));
                return Optional.empty();
            }
        };

        assertEquals(Optional.empty(), context.insert(Finder::click, 7));
        assertEquals(Optional.empty(), context.insert(Finder::find, "ore", 3));
        assertEquals(Optional.empty(), context.insertVoid(Finder::press, "a", 2));
        assertEquals(List.of(Finder.class.getMethod("click", Integer.class), 7,
                Finder.class.getMethod("find", String.class, Integer.class), "ore", 3,
                Finder.class.getMethod("press", String.class, Integer.class), "a", 2), inserted);
        assertSame(Marks.NONE, context.marks());
    }

    /** Stands in for the SDK's {@code ActivityBody}. */
    @FunctionalInterface
    public interface Body {
        String run();
    }

    @Test
    void targetsAreATypeAndItsHeading() {
        OverlayPart part = OverlayPart.of().targets(Body.class, " Activities ").targets(Runnable.class, "Home");
        assertEquals(List.of(new OverlayPart.TargetType(Body.class.getName(), "Activities"),
                new OverlayPart.TargetType("java.lang.Runnable", "Home")), part.targets());
        assertThrows(IllegalArgumentException.class, () -> part.targets(Body.class, "Again"));
        assertThrows(IllegalArgumentException.class, () -> OverlayPart.of().targets(String.class, "Text"));
        assertThrows(IllegalArgumentException.class, () -> OverlayPart.of().targets(Body.class, " "));
    }

    @Test
    void aWatchedScreenIsOneOfThree() {
        Watched window = Watched.window("Game");
        assertEquals(Watched.Kind.WINDOW, window.kind());
        assertEquals(Optional.of("Game"), window.title());
        assertTrue(window.area().isEmpty());

        Watched region = Watched.region(new Area(0, 0, 800, 600));
        assertEquals(Watched.Kind.REGION, region.kind());
        assertEquals(Optional.of(new Area(0, 0, 800, 600)), region.area());

        assertEquals(Watched.Kind.SESSION, Watched.session().kind());
        assertThrows(IllegalArgumentException.class, () -> Watched.window(""));
        assertThrows(IllegalArgumentException.class, () -> Watched.region(new Area(0, 0, 0, 10)));
    }

    @Test
    void closedSetsReadTotally() {
        assertEquals(ProbeResult.State.FOUND, ProbeResult.State.fromId(" Found "));
        assertEquals(ProbeResult.State.UNKNOWN, ProbeResult.State.fromId("later"));
        assertEquals(ProbeResult.State.UNKNOWN, ProbeResult.State.fromId(null));
        assertEquals(Marks.Kind.CLICK, Marks.Kind.fromId("click"));
        assertEquals(Marks.Kind.UNKNOWN, Marks.Kind.fromId(""));
    }

    @Test
    void aProbeResultCarriesItsArea() {
        assertEquals(Optional.of(new Area(1, 2, 3, 4)), found(null).area());
        assertTrue(ProbeResult.missing(null).area().isEmpty());
        assertEquals("", ProbeResult.unknown(null).text());
    }

    @Test
    void anOlderHostOpensTheLayerForARun() {
        RunOverlayContext older = new RunOverlayContext() {
            @Override
            public com.botmaker.plugin.api.StudioServices services() {
                return null;
            }

            @Override
            public void onClosed(Runnable action) {
            }
        };
        assertEquals(RunOverlayContext.Mode.RUNNING, older.mode());
        Marks.NONE.show(new Area(0, 0, 1, 1), Marks.Kind.NOTE, "x");
        Marks.NONE.clear();
    }
}
