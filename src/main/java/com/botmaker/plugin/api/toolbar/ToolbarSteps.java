package com.botmaker.plugin.api.toolbar;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * The steps of a {@link ToolbarItem} declaration, each offering only the next valid move:
 *
 * <pre>{@code
 * ToolbarItem.id(BOT_SETTINGS).label("⚙ Bot Settings")
 *         .tooltip("How the bot clicks and looks")
 *         .in(ToolbarGroup.PROJECT, 60)
 *         .onPress(() -> BotSettingsWindow::open);
 * }</pre>
 *
 * <p>The label, the tooltip and the place are required and named, rather than three strings in a row. When it
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

        /** The section of the bar, and the position in it, low first. */
        public Pressing in(ToolbarGroup group, int order) {
            Objects.requireNonNull(group, "group");
            return new Pressing(id, label, tooltip, group, order, EnabledWhen.ALWAYS, null);
        }
    }

    /** The place is said; the press is next, after an optional state and icon. */
    public static final class Pressing {

        private final String id;
        private final Supplier<String> label;
        private final String tooltip;
        private final ToolbarGroup group;
        private final int order;
        private final EnabledWhen enabledWhen;
        private final Supplier<String> icon;

        private Pressing(String id, Supplier<String> label, String tooltip, ToolbarGroup group, int order,
                         EnabledWhen enabledWhen, Supplier<String> icon) {
            this.id = id;
            this.label = label;
            this.tooltip = tooltip;
            this.group = group;
            this.order = order;
            this.enabledWhen = enabledWhen;
            this.icon = icon;
        }

        /** When it may be pressed; {@link EnabledWhen#ALWAYS} unless said. */
        public Pressing enabledWhen(EnabledWhen when) {
            return new Pressing(id, label, tooltip, group, order, Objects.requireNonNull(when, "when"), icon);
        }

        /** An icon: a resource in the plugin's own jar, or a URI the host can load. */
        public Pressing icon(String icon) {
            Objects.requireNonNull(icon, "icon");
            return new Pressing(id, label, tooltip, group, order, enabledWhen, () -> icon);
        }

        /** An icon read like the label, so it may arrive late. */
        public Pressing icon(Supplier<String> icon) {
            return new Pressing(id, label, tooltip, group, order, enabledWhen, Objects.requireNonNull(icon, "icon"));
        }

        /** What pressing it does: {@code () -> MyWindow::open}, a method taking the {@link ActionContext}. */
        public ToolbarItem onPress(Pressed pressed) {
            Objects.requireNonNull(pressed, "pressed");
            return new ToolbarItem(id, label, tooltip, icon, group, order, enabledWhen, pressed::press);
        }
    }
}
