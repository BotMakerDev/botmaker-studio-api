package com.botmaker.plugin.api;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The overlay members answer "no overlay is open" by default, which is every toolbar click.
 *
 * <p>Tested rather than left to the eye because the default is the whole compatibility story: an
 * {@link ActionContext} written before these members existed — a plugin's own test stub, the host's
 * {@code HostActionContext} — must still compile and must still answer something truthful.
 */
@DisplayNameGeneration(ReplaceUnderscores.class)
class OverlayContextDefaultsTest {

    /** A context implementing only what a toolbar item always needed. */
    private record Minimal(String projectName, String pinnedVersion) implements ActionContext {
        @Override
        public StudioServices services() {
            return null;
        }
    }

    @Test
    void a_context_with_no_overlay_answers_empty_rather_than_null() {
        ActionContext ctx = new Minimal("Demo", "1.2.0");

        assertTrue(ctx.overWindowTitle().isEmpty(), "no overlay is open");
        assertTrue(ctx.overBounds().isEmpty(), "no overlay is open");
    }

    @Test
    void inserting_at_a_cursor_that_does_not_exist_is_a_no_op_rather_than_a_throw() {
        ActionContext ctx = new Minimal("Demo", "1.2.0");

        // A plugin pressing an item with no overlay open must not take the editor down with it.
        ctx.insertAtCursor("Mouse.click(10, 20);");
    }

    @Test
    void an_area_is_four_ints_and_nothing_else() {
        ActionContext.Area area = new ActionContext.Area(10, 20, 640, 480);

        assertEquals(10, area.x());
        assertEquals(20, area.y());
        assertEquals(640, area.width());
        assertEquals(480, area.height());
    }

    @Test
    void the_overlay_group_exists_and_is_not_the_hosts_own() {
        assertEquals("OVERLAY", ToolbarGroup.OVERLAY.name());
        assertTrue(ToolbarGroup.OVERLAY != ToolbarGroup.STUDIO);
    }
}
