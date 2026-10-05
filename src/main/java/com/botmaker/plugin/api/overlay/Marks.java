package com.botmaker.plugin.api.overlay;

import com.botmaker.plugin.api.toolbar.ActionContext.Area;

import java.util.Locale;

/**
 * Boxes the host draws over the watched screen while the overlay is open — where a probe found its picture,
 * where a click would land, what a tool just picked.
 *
 * <p>The host draws them on the overlay's layer, which takes no clicks, and owns their look; a plugin says
 * only where, which kind and what label. A plugin that wants to draw more draws in its own
 * {@link com.botmaker.plugin.api.run.RunOverlayPart#layer}.
 */
public interface Marks {

    /** A host with no overlay, which draws nothing. */
    Marks NONE = new Marks() {
        @Override
        public void show(Area area, Kind kind, String label) {
        }

        @Override
        public void clear() {
        }
    };

    /** Draws a {@code kind} box over {@code area}, in the bot's pixels, captioned {@code label}, until {@link #clear()} or the overlay closes. Any thread. */
    void show(Area area, Kind kind, String label);

    /** Removes every mark this handle drew. Any thread. */
    void clear();

    /** What a mark says, which picks its look. A closed set with stable ids; an unknown id is {@link #UNKNOWN}. */
    enum Kind {
        /** Something was found here. */
        FOUND("found", "Found"),
        /** Something was looked for and not found; drawn over where it was looked for. */
        MISSING("missing", "Missing"),
        /** A click would land here. */
        CLICK("click", "Click"),
        /** Just a place: what a tool picked. */
        NOTE("note", "Note"),
        /** A kind this host does not know. */
        UNKNOWN("", "Other");

        private final String id;
        private final String displayName;

        Kind(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public String id() {
            return id;
        }

        public String displayName() {
            return displayName;
        }

        /** The kind {@code id} names; {@link #UNKNOWN} for anything else. */
        public static Kind fromId(String id) {
            String wanted = id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
            for (Kind kind : values()) {
                if (!kind.id.isEmpty() && kind.id.equals(wanted)) return kind;
            }
            return UNKNOWN;
        }
    }
}
