package com.botmaker.plugin.api.value;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A plugin's own type declared by its factory alone: each part read by its parameter's name, each link found by
 * its {@link Link} mark, and Studio picking among several factories by which one rebuilds the value.
 */
class LinkTest {

    /** The links every pickable input shares, marked once on the interface. */
    public interface Input {
        @Link
        Input tried();

        boolean isTried();

        @Link
        Input never(String reason);

        String never();
    }

    public record Pad(String name, boolean isTried, String never, int wheel, String note) implements Input {
        public static Pad of(String name) {
            return new Pad(name, false, null, 0, "");
        }

        @Override
        public Pad tried() {
            return new Pad(name, true, never, wheel, note);
        }

        @Override
        public Pad never(String reason) {
            return new Pad(name, isTried, reason, wheel, note);
        }

        @Link
        public Pad wheel(int notches) {
            return new Pad(name, isTried, never, notches, note);
        }

        /** Read by {@code note()} under another name, so declared explicitly. */
        public Pad noted(String text) {
            return new Pad(name, isTried, never, wheel, text);
        }
    }

    private static final DeclaredCall<Pad> PAD = ComponentType.part(Pad.class).writtenAs(Pad::of);

    /** A varargs link, read by the list it adds to. */
    public record Chore(String name, List<String> tags) {
        public static Chore of(String name) {
            return new Chore(name, List.of());
        }

        @Link
        public Chore tags(String... more) {
            List<String> all = new ArrayList<>(tags);
            all.addAll(List.of(more));
            return new Chore(name, all);
        }
    }

    @Test
    void aVarargsLinkIsOneListPart() {
        DeclaredCall<Chore> chore = ComponentType.part(Chore.class).writtenAs(Chore::of);
        Wither<Chore> tags = chore.withers().getFirst();
        assertTrue(tags.spread());
        assertEquals(String.class, tags.elementType());
        assertEquals(List.of(String.class, List.class), chore.componentTypes());
        Chore tagged = Chore.of("dishes").tags("home", "daily");
        assertEquals(List.of("dishes", List.of("home", "daily")), chore.components(tagged));
        assertEquals(tagged, chore.build(List.of("dishes", List.of("home", "daily"))));
        assertEquals(Chore.of("dishes"), chore.build(List.of("dishes", List.of())));
        assertEquals(null, tags.apply(Chore.of("dishes"), List.of(3)), "an element of the wrong kind refuses");
    }

    @Test
    void theFactorysPartIsReadByItsName() {
        assertEquals(String.class, PAD.componentTypes().getFirst());
        assertEquals("A", PAD.components(Pad.of("A")).getFirst());
    }

    @Test
    void markedLinksFollowInRecordOrderIncludingTheInterfaces() {
        assertEquals(List.of("tried", "never", "wheel"),
                PAD.withers().stream().map(w -> w.method().getName()).toList());
        assertTrue(PAD.withers().getFirst().flag());
        assertEquals(List.of(String.class, boolean.class, String.class, int.class), PAD.componentTypes());
        Pad pad = Pad.of("A").tried().never("closed").wheel(3);
        assertEquals(List.of("A", true, "closed", 3), PAD.components(pad));
        assertEquals(pad, PAD.build(PAD.components(pad)));
    }

    @Test
    void anExplicitStepReplacesTheMarkedLinkInItsPlace() {
        DeclaredCall<Pad> pad = PAD.<String>with(Pad::never, p -> p.never() == null ? null : p.never().toUpperCase());
        assertEquals(List.of("tried", "never", "wheel"),
                pad.withers().stream().map(w -> w.method().getName()).toList());
        assertFalse(pad.withers().get(1).inferred());
        assertEquals("CLOSED", pad.components(Pad.of("A").never("closed")).get(2));
        DeclaredCall<Pad> noted = PAD.with(Pad::noted, Pad::note);
        assertEquals("noted", noted.withers().getLast().method().getName());
        assertThrows(IllegalArgumentException.class, () -> pad.<String>with(Pad::never, Pad::never));
    }

    /** The canonical constructor takes every component, so no link adds a part a second time. */
    @Test
    void aRecordWrittenWholeChainsNoLinkItsConstructorTakes() {
        DeclaredCall<Pad> whole = ComponentType.part(Pad.class).writtenAsRecord();
        assertEquals(List.of(), whole.withers());
        assertEquals(5, whole.componentTypes().size());
    }

    public interface Scalable<S, P> {
        @Link
        S scaled(P by);
    }

    public record Box(int size) implements Scalable<Box, Integer> {
        public static Box of(int size) {
            return new Box(size);
        }

        @Override
        public Box scaled(Integer by) {
            return new Box(size * by);
        }

        public Integer scaled() {
            return 1;
        }
    }

    @Test
    void aMarkOnAGenericSupertypeCoversItsOverride() {
        assertEquals(List.of("scaled"),
                ComponentType.part(Box.class).writtenAs(Box::of).withers().stream()
                        .map(w -> w.method().getName()).toList());
    }

    public interface Moving {
        @Link
        default Moving still() {
            return this;
        }

        boolean isStill();
    }

    public record Ball(String name, boolean isStill) implements Moving {
        public static Ball of(String name) {
            return new Ball(name, false);
        }
    }

    @Test
    void aMarkedDefaultNotOverriddenIsRefusedSayingSo() {
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> ComponentType.part(Ball.class).writtenAs(Ball::of));
        assertTrue(refused.getMessage().contains("override it in"), refused.getMessage());
    }

    public record Orphan(String name) {
        public static Orphan of(String name) {
            return new Orphan(name);
        }

        @Link
        public Orphan hidden() {
            return this;
        }
    }

    @Test
    void aMarkWithNoReaderIsRefused() {
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> ComponentType.part(Orphan.class).writtenAs(Orphan::of));
        assertTrue(refused.getMessage().contains("isHidden"), refused.getMessage());
    }

    public record Misnamed(String label) {
        public static Misnamed of(String name) {
            return new Misnamed(name);
        }
    }

    @Test
    void aPartWithNoReaderIsRefusedNamingIt() {
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> ComponentType.part(Misnamed.class).writtenAs(Misnamed::of));
        assertTrue(refused.getMessage().contains("part name"), refused.getMessage());
        assertEquals("x", ComponentType.part(Misnamed.class).writtenAs(Misnamed::of, Misnamed::label)
                .components(Misnamed.of("x")).getFirst());
    }

    /** A class written by two factories, with no predicate on either. */
    public record Key(String name) {
        public static Key of(String name) {
            return new Key(name);
        }
    }

    public record Hotkey(List<Key> keys) {
        public static Hotkey of(Key key) {
            return new Hotkey(List.of(key));
        }

        public static Hotkey combo(List<Key> keys) {
            return new Hotkey(List.copyOf(keys));
        }

        public Key key() {
            return keys.getFirst();
        }
    }

    @Test
    void studioPicksTheFactoryThatRebuildsTheValue() {
        DeclaredCall<Hotkey> one = ComponentType.part(Hotkey.class).writtenAs(Hotkey::of);
        DeclaredCall<Hotkey> many = ComponentType.part(Hotkey.class).writtenAs(Hotkey::combo);
        List<DeclaredCall<Hotkey>> both = List.of(one, many);
        assertSame(one, ComponentType.writer(both, Hotkey.of(Key.of("F5"))));
        assertSame(many, ComponentType.writer(both, Hotkey.combo(List.of(Key.of("CTRL"), Key.of("S")))));
        assertTrue(one.writes(Hotkey.combo(List.of(Key.of("CTRL"), Key.of("S")))));
    }

    /** No equals, so no copy is ever equal: the first that writes it, as with a single declaration. */
    public static final class Plain {
        private final String name;

        private Plain(String name) {
            this.name = name;
        }

        public static Plain of(String name) {
            return new Plain(name);
        }

        public String name() {
            return name;
        }
    }

    @Test
    void aValueNoDeclarationRebuildsGoesToTheFirstThatWritesIt() {
        DeclaredCall<Plain> first = ComponentType.part(Plain.class).writtenAs(Plain::of);
        DeclaredCall<Plain> refusing = first.when(p -> false);
        Plain plain = Plain.of("a");
        assertFalse(first.rebuilds(plain));
        assertSame(first, ComponentType.writer(List.of(refusing, first, first.when(p -> true)), plain));
        assertSame(null, ComponentType.writer(List.of(refusing), plain));
    }
}
