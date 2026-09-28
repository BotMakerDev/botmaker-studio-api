package com.botmaker.plugin.api.toolbar;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ToolbarStepsTest {

    static final AtomicInteger PRESSES = new AtomicInteger();

    static void open(ActionContext context) {
        PRESSES.incrementAndGet();
    }

    @Test
    void theStepsFillEveryComponent() {
        ToolbarItem item = ToolbarItem.id("settings").label("⚙ Settings")
                .tooltip("Opens the settings")
                .in(ToolbarGroup.PROJECT, 60)
                .onPress(() -> ToolbarStepsTest::open);

        assertEquals("settings", item.id());
        assertEquals("⚙ Settings", item.label().get());
        assertEquals("Opens the settings", item.tooltip());
        assertNull(item.icon());
        assertEquals(ToolbarGroup.PROJECT, item.group());
        assertEquals(60, item.order());
        assertEquals(EnabledWhen.ALWAYS, item.enabledWhen());
    }

    @Test
    void theOptionalStepsSitBeforeThePress() {
        ToolbarItem item = ToolbarItem.id("run").label(() -> "▶ Run")
                .tooltip("Runs the bot")
                .in(ToolbarGroup.RUN, 1)
                .enabledWhen(EnabledWhen.BOT_STOPPED)
                .icon("run.png")
                .onPress(() -> ToolbarStepsTest::open);

        assertEquals(EnabledWhen.BOT_STOPPED, item.enabledWhen());
        assertEquals("run.png", item.icon().get());
    }

    /** The press is looked up when pressed, not when the item is declared — see {@link Pressed}. */
    @Test
    void thePressIsResolvedOnlyWhenPressed() {
        AtomicInteger resolved = new AtomicInteger();
        ToolbarItem item = ToolbarItem.id("late").label("Late").tooltip("Late").in(ToolbarGroup.TOOLS, 1)
                .onPress(() -> {
                    resolved.incrementAndGet();
                    return (Consumer<ActionContext>) ToolbarStepsTest::open;
                });
        assertEquals(0, resolved.get());

        int before = PRESSES.get();
        item.onClick().accept(null);
        assertEquals(1, resolved.get());
        assertEquals(before + 1, PRESSES.get());
    }

    @Test
    void aBlankIdOrTooltipIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> ToolbarItem.id(" "));
        assertThrows(IllegalArgumentException.class, () -> ToolbarItem.id("x").label("X").tooltip(""));
        assertThrows(NullPointerException.class, () -> ToolbarItem.id("x").label("X").tooltip("t").in(null, 1));
        assertThrows(NullPointerException.class,
                () -> ToolbarItem.id("x").label("X").tooltip("t").in(ToolbarGroup.RUN, 1).onPress(null));
    }
}
