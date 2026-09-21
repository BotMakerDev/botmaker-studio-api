package com.botmaker.plugin.api.toolbar;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * One button on the toolbar, contributed as <b>data</b>: the host builds the {@code Node}.
 *
 * <p>Deliberately unlike {@link SlotEditor}, which hands back a {@code Node} the plugin built itself. The
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
 * @param id         stable, and unique within the contributing plugin. The host prefixes it with the
 *                   plugin's own id, so two plugins may both call an item {@code "settings"}
 * @param label      the button's text, called at build and at every refresh; never {@code null}, may return
 *                   different text each time
 * @param tooltip    the sentence explaining what pressing it does, or {@code null} for none. Write one: a
 *                   toolbar button is a glyph and two words, and the tooltip is where the rest lives
 * @param icon       a resource name in the plugin's own jar, or any URI the host can load ({@code file:},
 *                   {@code jar:}) — called like {@code label}, so an icon may arrive late. {@code null}, or a
 *                   supplier answering {@code null}, means no icon, which is an ordinary state: the label
 *                   already carries a glyph in most of this application
 * @param group      which section of the bar; {@link ToolbarGroup#STUDIO} is refused
 * @param order      position within the group, low first. Ties break on the plugin's id, so a bar built from
 *                   two plugins is stable rather than dependent on discovery order
 * @param enabledWhen when the item may be pressed
 * @param onClick    what pressing it does, given the host facts in {@link ActionContext}
 */
public record ToolbarItem(String id, Supplier<String> label, String tooltip, Supplier<String> icon,
                          ToolbarGroup group, int order, EnabledWhen enabledWhen,
                          Consumer<ActionContext> onClick) {

    /**
     * The ordinary case: a fixed label, no icon, always pressable.
     *
     * <p>Most items are this, and spelling out four nulls and an {@code EnabledWhen} at every call site is
     * how a surface gets a reputation for being heavy.
     */
    public static ToolbarItem of(String id, String label, String tooltip, ToolbarGroup group, int order,
                                 Consumer<ActionContext> onClick) {
        return new ToolbarItem(id, () -> label, tooltip, null, group, order, EnabledWhen.ALWAYS, onClick);
    }

    /** The same, for an item that only makes sense while nothing is running. */
    public static ToolbarItem whenStopped(String id, String label, String tooltip, ToolbarGroup group,
                                          int order, Consumer<ActionContext> onClick) {
        return new ToolbarItem(id, () -> label, tooltip, null, group, order, EnabledWhen.BOT_STOPPED, onClick);
    }
}
