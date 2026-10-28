package com.botmaker.plugin.api.toolbar;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * The steps of a {@link ToolbarItem} declaration, each offering only the next valid move:
 *
 * <pre>{@code
 * ToolbarItem.id(BOT_SETTINGS).label("⚙ Bot Settings")
 *         .tooltip("How the bot clicks and looks")
 *         .onBar(40)
 *         .onPress(() -> BotSettingsWindow::open);
 * }</pre>
 *
 * <p>The label, the tooltip and the place ({@code onBar} or {@code inOverlay}) are required and named, rather
 * than three strings in a row. When it
 * may be pressed and its icon are optional, and sit between
 * the place and the press.
 */
public final class ToolbarSteps {

    private ToolbarSteps() {}

    /** The id is said; the label is next. */
    public static final class Labelling {

        private final String id;

        Labelling(String id) {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("A toolbar item needs an id");
            this.id = id;
        }

        /** A fixed label: a glyph and two words. */
        public Explaining label(String label) {
            Objects.requireNonNull(label, "label");
            return new Explaining(id, () -> label);
        }

        /**
         * A label read at every refresh, so a button may name what the project holds. Keep it cheap and pure:
         * see {@link ToolbarItem}.
         */
        public Explaining label(Supplier<String> label) {
            return new Explaining(id, Objects.requireNonNull(label, "label"));
        }
    }

    /** The label is said; the sentence explaining the press is next. */
    public static final class Explaining {

        private final String id;
        private final Supplier<String> label;

        private Explaining(String id, Supplier<String> label) {
            this.id = id;
            this.label = label;
        }

        /** What pressing it does, in a sentence: the button itself only has room for two words. */
        public Placing tooltip(String tooltip) {
            if (tooltip == null || tooltip.isBlank()) {
                throw new IllegalArgumentException(id + " needs a tooltip saying what pressing it does");
            }
            return new Placing(id, label, tooltip);
        }
    }

    /** The tooltip is said; where the item sits is next. */
    public static final class Placing {

        private final String id;
        private final Supplier<String> label;
        private final String tooltip;

        private Placing(String id, Supplier<String> label, String tooltip) {
            this.id = id;
            this.label = label;
            this.tooltip = tooltip;
        }

        /**
         * On the main bar, in this plugin's own row, at {@code order} (low first). The host gives each plugin
         * a tab of its own, so the order is only among this plugin's items.
         */
        public Pressing onBar(int order) {
            return new Pressing(id, label, tooltip, ToolbarItem.Place.BAR, order, EnabledWhen.ALWAYS, null);
        }

        /**
         * In the overlay editor's Actions tab, at {@code order} (low first), never on the main bar: an action
         * whose subject is the window the HUD is drawn over, handed a context that answers
         * {@link ActionContext#overWindowTitle()}. A pane rather than a button is an
         * {@link com.botmaker.plugin.api.overlay.OverlayTool}.
         */
        public Pressing inOverlay(int order) {
            return new Pressing(id, label, tooltip, ToolbarItem.Place.OVERLAY, order, EnabledWhen.ALWAYS, null);
        }
    }

    /** The place is said; the press is next, after an optional state and icon. */
    public static final class Pressing {

        private final String id;
        private final Supplier<String> label;
        private final String tooltip;
        private final ToolbarItem.Place place;
        private final int order;
        private final EnabledWhen enabledWhen;
        private final Supplier<String> icon;

        private Pressing(String id, Supplier<String> label, String tooltip, ToolbarItem.Place place, int order,
                         EnabledWhen enabledWhen, Supplier<String> icon) {
            this.id = id;
            this.label = label;
            this.tooltip = tooltip;
            this.place = place;
            this.order = order;
            this.enabledWhen = enabledWhen;
            this.icon = icon;
        }

        /** When it may be pressed; {@link EnabledWhen#ALWAYS} unless said. */
        public Pressing enabledWhen(EnabledWhen when) {
            return new Pressing(id, label, tooltip, place, order, Objects.requireNonNull(when, "when"), icon);
        }

        /** An icon: a resource in the plugin's own jar, or a URI the host can load. */
        public Pressing icon(String icon) {
            Objects.requireNonNull(icon, "icon");
            return new Pressing(id, label, tooltip, place, order, enabledWhen, () -> icon);
        }

        /** An icon read like the label, so it may arrive late. */
        public Pressing icon(Supplier<String> icon) {
            return new Pressing(id, label, tooltip, place, order, enabledWhen, Objects.requireNonNull(icon, "icon"));
        }

        /** What pressing it does: {@code () -> MyWindow::open}, a method taking the {@link ActionContext}. */
        public ToolbarItem onPress(Pressed pressed) {
            Objects.requireNonNull(pressed, "pressed");
            return new ToolbarItem(id, label, tooltip, icon, place, order, enabledWhen, pressed::press);
        }
    }
}
