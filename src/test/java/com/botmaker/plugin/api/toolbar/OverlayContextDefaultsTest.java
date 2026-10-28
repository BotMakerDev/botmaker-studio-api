package com.botmaker.plugin.api.toolbar;

import com.botmaker.plugin.api.StudioServices;
import com.botmaker.plugin.api.overlay.Area;
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
    private record Minimal() implements ActionContext {
        @Override
        public StudioServices services() {
            return null;
        }
    }

    @Test
    void a_context_with_no_overlay_answers_empty_rather_than_null() {
        ActionContext ctx = new Minimal();

        assertTrue(ctx.overWindowTitle().isEmpty(), "no overlay is open");
        assertTrue(ctx.overBounds().isEmpty(), "no overlay is open");
    }

    @Test
    void an_area_is_four_ints_and_nothing_else() {
        Area area = new Area(10, 20, 640, 480);

        assertEquals(10, area.x());
        assertEquals(20, area.y());
        assertEquals(640, area.width());
        assertEquals(480, area.height());
    }

    @Test
    void an_overlay_item_is_placed_in_the_overlay_not_on_the_bar() {
        ToolbarItem item = ToolbarItem.id("here").label("⌖ Here").tooltip("Points the bot here")
                .inOverlay(10)
                .onPress(() -> context -> {});

        assertEquals(ToolbarItem.Place.OVERLAY, item.place());
        assertEquals(10, item.order());
    }
}
