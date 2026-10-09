package com.botmaker.plugin.api.value;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A call with named links after it: each wither's part follows the factory's, so {@code build(components(v))}
 * is {@code v} for every combination, and the factory's parts alone build what the factory makes.
 */
class WitherTest {

    /** A factory taking the name, and a link for everything else. */
    public record Task(String name, String note, boolean home, int tries) {
        public static Task of(String name) {
            return new Task(name, "", false, 1);
        }

        public Task described(String note) {
            return new Task(name, note, home, tries);
        }

        public Task goesHome() {
            return new Task(name, note, true, tries);
        }

        public Task tries(int count) {
            return new Task(name, note, home, count);
        }

        public static Task copy(Task task) {
            return task;
        }

        public static Task all(String... names) {
            return of(String.join(" ", names));
        }
    }

    private static final DeclaredCall<Task> TASK = ComponentType.part(Task.class)
            .writtenAs(Task::of, Task::name)
            .with(Task::described, Task::note)
            .flag(Task::goesHome, Task::home)
            .with(Task::tries, Task::tries);

    @Test
    void eachWithersPartFollowsTheFactorys() {
        assertEquals(List.of(String.class, String.class, boolean.class, int.class), TASK.componentTypes());
        Task task = new Task("Collect", "Picks up ore", true, 3);
        assertEquals(List.of("Collect", "Picks up ore", true, 3), TASK.components(task));
        assertEquals(List.of("described", "goesHome", "tries"),
                TASK.withers().stream().map(w -> w.method().getName()).toList());
        assertTrue(TASK.withers().get(1).flag());
        assertEquals(int.class, TASK.withers().get(2).partType());
    }

    @Test
    void everyCombinationRoundTrips() {
        for (String note : List.of("", "Picks up ore")) {
            for (boolean home : new boolean[]{false, true}) {
                for (int tries : new int[]{1, 4}) {
                    Task task = new Task("Collect", note, home, tries);
                    assertEquals(task, TASK.build(TASK.components(task)), task.toString());
                }
            }
        }
    }

    @Test
    void theFactorysPartsAloneBuildWhatTheFactoryMakes() {
        assertEquals(Task.of("Collect"), TASK.build(List.of("Collect")));
        assertNull(TASK.build(List.of("Collect", "note")), "neither the factory's parts nor all of them");
    }

    @Test
    void aFlagCannotTurnOffWhatTheValueHasOn() {
        Wither<Task> home = TASK.withers().get(1);
        Task on = Task.of("Collect").goesHome();
        assertNull(home.apply(on, false));
        assertEquals(on, home.apply(on, true));
        Task off = Task.of("Collect");
        assertEquals(off, home.apply(off, false));
        assertNull(TASK.build(List.of("Collect", "", "yes", 1)), "a flag's part is a Boolean");
    }

    /** A note the factory leaves null, and a wither that refuses one. */
    public record Mark(String name, String note) {
        public static Mark of(String name) {
            return new Mark(name, null);
        }

        public Mark noted(String note) {
            return new Mark(name, java.util.Objects.requireNonNull(note));
        }
    }

    /** A wither is never asked to take the part the factory made, which it may refuse. */
    @Test
    void aPartAsTheFactoryMadeItIsNotApplied() {
        DeclaredCall<Mark> mark = ComponentType.part(Mark.class).writtenAs(Mark::of, Mark::name)
                .with(Mark::noted, Mark::note);
        assertEquals(Mark.of("a"), mark.build(mark.components(Mark.of("a"))));
        assertEquals(new Mark("a", "x"), mark.build(mark.components(new Mark("a", "x"))));
    }

    @Test
    void aPartIsCoercedToTheParameterItFills() {
        assertEquals(new Task("Collect", "", false, 2), TASK.build(Arrays.asList("Collect", "", false, 2L)));
    }

    @Test
    void whatIsNoWitherIsRefused() {
        DeclaredCall<Task> base = ComponentType.part(Task.class).writtenAs(Task::of, Task::name);
        assertThrows(IllegalArgumentException.class, () -> base.with(Task::described, Task::note)
                .with(Task::described, Task::note), "chained twice");
        assertThrows(IllegalArgumentException.class, () -> base.flag(Task::copy, Task::home), "static");
        assertThrows(IllegalArgumentException.class, () -> ComponentType.part(Task.class)
                .writtenAsEach(Task::all, t -> List.of(t.name())).with(Task::described, Task::note), "varargs");
        assertFalse(base.withers().iterator().hasNext());
    }
}
