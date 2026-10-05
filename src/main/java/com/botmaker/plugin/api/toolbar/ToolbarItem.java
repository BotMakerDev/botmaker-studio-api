package com.botmaker.plugin.api.toolbar;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * One button on the toolbar, contributed as <b>data</b>: the host builds the {@code Node}.
 *
 * <p>Deliberately unlike {@link com.botmaker.plugin.api.slot.SlotEditor}, which hands back a {@code Node} the plugin built itself. The
 * difference is not consistency but expressiveness: a bespoke image picker cannot be described as data and a
 * button can, and describing it as data is what lets the host own the things a shared bar has to own —
 * grouping, ordering, separators, packing, the overflow menu, the icon box and the theme. A plugin returning
 * a {@code Node} would take all of that with it, and two plugins doing so would produce a bar with two
 * different button heights.
 *
 * <h2>The label is a {@link Supplier}, and that is the one thing worth reading twice</h2>
 *
 * <p>It is called when the bar is built and again whenever the host refreshes, so a button may say what the
 * project currently holds — <i>"🎯 Diablo IV"</i> rather than <i>"🎯 Capture Targets"</i>. This exists
 * because Studio's own bar already worked that way before there was a plugin surface at all: two of its
 * buttons track project state and a third resolves a game's real title in the background. A record of
 * {@code String} would have described a toolbar nobody has.
 *
 * <p><b>Keep it cheap and keep it pure.</b> It is called during layout, on the JavaFX thread. Read a field,
 * format a string, return. Anything that touches a disk or a network belongs on a background thread whose
 * result the supplier then reads — which is exactly how a cover-art title gets onto a button.
 *
 * <h2>A class, not a record</h2>
 *
 * <p>Only {@link #id(String)}'s steps build one; the constructor is package-private. A record's canonical
 * constructor is public, and growing a component changes it, so every plugin compiled against the old one
 * would throw {@code NoSuchMethodError} the day a component was added. Through the steps a new component is
 * a new optional step, and a compiled plugin never sees it.
 *
 * <p>The accessors below are what the host reads:
 *
 * <ul>
 *   <li>{@code id}: stable, and unique within the contributing plugin. The host prefixes it with the plugin's
 *       own id, so two plugins may both call an item {@code "settings"}</li>
 *   <li>{@code label}: the button's text, called at build and at every refresh; never {@code null}, may
 *       return different text each time</li>
 *   <li>{@code tooltip}: the sentence explaining what pressing it does. A toolbar button is a glyph and two
 *       words, and the tooltip is where the rest lives</li>
 *   <li>{@code icon}: a resource name in the plugin's own jar, or any URI the host can load ({@code file:},
 *       {@code jar:}) — called like {@code label}, so an icon may arrive late. {@code null}, or a supplier
 *       answering {@code null}, means no icon, which is an ordinary state: the label already carries a glyph
 *       in most of this application</li>
 *   <li>{@code group}: which section of the bar; {@link ToolbarGroup#STUDIO} is refused</li>
 *   <li>{@code order}: position within the group, low first. Ties break on the plugin's id, so a bar built
 *       from two plugins is stable rather than dependent on discovery order</li>
 *   <li>{@code enabledWhen}: when the item may be pressed</li>
 *   <li>{@code onClick}: what pressing it does, given the host facts in {@link ActionContext}</li>
 * </ul>
 */
public final class ToolbarItem {

    private final String id;
    private final Supplier<String> label;
    private final String tooltip;
    private final Supplier<String> icon;
    private final ToolbarGroup group;
    private final int order;
    private final EnabledWhen enabledWhen;
    private final Consumer<ActionContext> onClick;

    ToolbarItem(String id, Supplier<String> label, String tooltip, Supplier<String> icon, ToolbarGroup group,
                int order, EnabledWhen enabledWhen, Consumer<ActionContext> onClick) {
        this.id = id;
        this.label = label;
        this.tooltip = tooltip;
        this.icon = icon;
        this.group = group;
        this.order = order;
        this.enabledWhen = enabledWhen;
        this.onClick = onClick;
    }

    /**
     * Declares an item, one named step at a time ({@link ToolbarSteps}): {@code ToolbarItem.id(ID).label(…)
     * .tooltip(…).in(group, order).onPress(() -> MyWindow::open)}; {@code .enabledWhen(…)} and
     * {@code .icon(…)} are the optional steps before the press.
     */
    public static ToolbarSteps.Labelling id(String id) {
        return new ToolbarSteps.Labelling(id);
    }

    public String id() {
        return id;
    }

    public Supplier<String> label() {
        return label;
    }

    public String tooltip() {
        return tooltip;
    }

    public Supplier<String> icon() {
        return icon;
    }

    public ToolbarGroup group() {
        return group;
    }

    public int order() {
        return order;
    }

    public EnabledWhen enabledWhen() {
        return enabledWhen;
    }

    public Consumer<ActionContext> onClick() {
        return onClick;
    }

    @Override
    public String toString() {
        return "ToolbarItem[" + id + " in " + group + " at " + order + "]";
    }
}
