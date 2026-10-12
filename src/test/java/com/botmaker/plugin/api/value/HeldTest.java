package com.botmaker.plugin.api.value;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A part the host holds as something the factory does not take: checked against its kind before anything is
 * built, handed to the factory by {@code give}, or read typed by {@code builtFrom}; a part of another kind is not
 * this call.
 */
class HeldTest {

    /** What the host holds for a class literal: the plugin's value naming it. */
    public record Kind(String name) {
        static final Kind NONE = new Kind("");
    }

    /** What the host holds for a method reference: the text it is written as. */
    public record Named(String source) {}

    /** A factory taking a class literal, which nothing the host holds can be handed to, and two links. */
    public record Card(Kind kind, String body, Named home, Kind next) {
        public static Card of(Class<?> kind, String body) {
            return new Card(new Kind(kind.getSimpleName()), body, null, Kind.NONE);
        }

        public Card home(Named home) {
            return new Card(kind, body, home, next);
        }

        public Card then(Class<?> to) {
            throw new UnsupportedOperationException("the host reads this link and never calls it");
        }

        Card withNext(Kind to) {
            return new Card(kind, body, home, to);
        }
    }

    private static final Held<Card, Kind> KIND = Held.as(Kind.class, Card::kind);
    private static final Held<Card, String> BODY = Held.as(String.class, Card::body);
    private static final Held<Card, String> HOME = Held.as(String.class,
            card -> card.home() == null ? "" : card.home().source());
    private static final Held<Card, Kind> NEXT = Held.as(Kind.class, Card::next);

    private static final DeclaredCall<Card> CARD = ComponentType.part(Card.class)
            .writtenAs(Card::of, KIND.part(), BODY.part())
            .builtFrom(parts -> new Card(parts.get(KIND), parts.get(BODY), null, Kind.NONE))
            .with(Card::home, HOME.part(source -> source.isBlank() ? null : new Named(source)))
            .with(Card::then, NEXT, Card::withNext);

    @Test
    void aCallOfHeldPartsRoundTripsThroughWhatTheHostHolds() {
        Card card = new Card(new Kind("Collect"), "Collect::body", new Named("Bot::goHome"), new Kind("Fight"));
        assertEquals(List.of(new Kind("Collect"), "Collect::body", "Bot::goHome", new Kind("Fight")),
                CARD.components(card));
        assertEquals(card, CARD.build(CARD.components(card)));

        Card plain = new Card(new Kind("Collect"), "Collect::body", null, Kind.NONE);
        assertEquals(plain, CARD.build(List.of(new Kind("Collect"), "Collect::body")), "the factory's parts alone");
        assertEquals(List.of(Class.class, String.class, Named.class, Class.class), CARD.componentTypes());
    }

    @Test
    void aPartOfAnotherKindIsNotThisCall() {
        assertNull(CARD.build(List.of("Collect", "Collect::body")), "a factory part");
        assertNull(CARD.build(List.of(new Kind("Collect"), "Collect::body", 7, Kind.NONE)), "a given link");
        assertNull(CARD.build(List.of(new Kind("Collect"), "Collect::body", "", "Fight")), "an applied link");
        assertNull(CARD.build(List.of(new Kind("Collect"))), "too few");
    }

    /** A map keeps the entries of its kinds; a list refuses an element of another kind. */
    public record Board(Map<String, Integer> cells, List<Kind> kinds) {
        public static Board of(Map<String, Integer> cells, List<Kind> kinds) {
            return new Board(Map.copyOf(cells), List.copyOf(kinds));
        }

        public static Board all(List<Kind> first, Kind... rest) {
            return new Board(Map.of(), first);
        }
    }

    private static final Held<Board, Map<String, Integer>> CELLS =
            Held.map(String.class, Integer.class, Board::cells);
    private static final Held<Board, List<Kind>> KINDS = Held.list(Kind.class, Board::kinds);

    private static final DeclaredCall<Board> BOARD = ComponentType.part(Board.class)
            .writtenAs(Board::of, CELLS.part(), KINDS.part());

    @Test
    void aMapDropsAStrayEntryAndAListRefusesAStrayElement() {
        Map<Object, Object> cells = new LinkedHashMap<>();
        cells.put("a", 1);
        cells.put("b", "two");
        cells.put(3, 3);
        cells.put("c", 4L);
        Board kept = BOARD.build(List.of(cells, List.of(new Kind("Collect"))));
        assertEquals(new Board(Map.of("a", 1, "c", 4), List.of(new Kind("Collect"))), kept,
                "a number widens or rounds as a plain part's does");

        assertNull(BOARD.build(List.of(Map.of(), List.of(new Kind("Collect"), "Fight"))));
        assertNull(BOARD.build(List.of(List.of(), List.of())), "a list where the map goes");
    }

    /** A call with no parts, built as the value the plugin names rather than by calling the factory. */
    public record Here(String where) {
        public static Here current() {
            throw new IllegalStateException("only a running bot knows");
        }
    }

    @Test
    void aCallWithNoPartsIsBuiltAsItsSupplier() {
        DeclaredCall<Here> here = ComponentType.part(Here.class).writtenAs(Here::current)
                .builtAs(() -> new Here("ambient"));
        assertEquals(new Here("ambient"), here.build(List.of()));
        assertNull(here.build(List.of(1)));
        assertEquals(List.of(), here.components(new Here("ambient")));
    }

    @Test
    void aDeclarationThatCannotBeBuiltIsRefusedWhereItIsMade() {
        assertThrows(IllegalArgumentException.class, () -> ComponentType.part(Card.class)
                .writtenAs(Card::of, KIND.part(), Card::body)
                .builtFrom(parts -> null), "a plain accessor builtFrom cannot read");
        assertThrows(IllegalArgumentException.class, () -> ComponentType.part(Card.class)
                .writtenAs(Card::of, KIND.part(), BODY.part())
                .builtAs(() -> null), "a call with parts");
        assertThrows(IllegalArgumentException.class, () -> ComponentType.part(Card.class)
                .writtenAs(Card::of, KIND.part(), KIND.part())
                .builtFrom(parts -> null), "one held part at two places");
        assertThrows(IllegalArgumentException.class, () -> ComponentType.part(Board.class)
                .<List<Kind>, Kind[], Board>writtenAs(Board::all, KINDS.part(), board -> new Kind[0]),
                "a varargs factory");
    }

    @Test
    void aBuildThatThrowsIsNotThisCallAndANullPartIsOne() {
        DeclaredCall<Card> foreign = ComponentType.part(Card.class)
                .writtenAs(Card::of, KIND.part(), BODY.part())
                .builtFrom(parts -> new Card(parts.get(NEXT), "", null, Kind.NONE));
        assertNull(foreign.build(List.of(new Kind("Collect"), "")), "a held part of another call");

        assertEquals(new Card(new Kind("Collect"), null, null, Kind.NONE),
                CARD.build(Arrays.asList(new Kind("Collect"), null)));
        assertThrows(UnsupportedOperationException.class, () -> KIND.part().apply(CARD_OF_COLLECT));
    }

    private static final Card CARD_OF_COLLECT = new Card(new Kind("Collect"), "", null, Kind.NONE);
}
