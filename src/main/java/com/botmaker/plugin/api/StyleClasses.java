package com.botmaker.plugin.api;

/**
 * The style-class names the host's stylesheet defines for a plugin to wear.
 *
 * <p>A plugin's editor is attached to a scene the host has already themed, so it inherits the host's
 * stylesheet for free — but only for the classes that stylesheet actually names. The names are the host's
 * alone to know, which is why they are here and not in a plugin's own code: the host spells them with these
 * constants too, and its tests fail when a constant names a class its stylesheet no longer defines. A
 * rename is therefore a compile error on both sides, never a plugin widget that silently loses its look.
 *
 * <p><b>These are names, not a look.</b> What each one renders as is the host's to change, and changes with
 * the theme the user picked. A plugin that hard-codes a colour matches the application once; a plugin that
 * names a class matches it after the next release too.
 *
 * <p>Constants only: adding one is binary-compatible, removing one is a break. The toolkit's {@code Styles}
 * implements this interface, so a plugin that already writes {@code Styles.PILL} keeps compiling.
 */
public interface StyleClasses {

    /** The control standing in for a value — the pill shape every argument editor wears. */
    String PILL = "argument-pill";

    /** A field sunk into the surface behind it, for a control sitting inside a block or a card. */
    String INSET_FIELD = "block-inset-field";

    /** {@link #INSET_FIELD} without its border — for a field that is already inside a bordered container. */
    String INSET_FIELD_FLAT = "block-inset-field--flat";

    /** A small rounded token: a tag, a unit, a mode. */
    String CHIP = "block-chip";

    /** {@link #CHIP} with no fill, for a chip that must not compete with the value beside it. */
    String CHIP_PLAIN = "block-chip--plain";

    /** Secondary text: a unit, a hint, the word between two fields. */
    String CAPTION = "block-caption";

    /** {@link #CAPTION} at full weight, for the one word in a row that carries the meaning. */
    String CAPTION_STRONG = "block-caption--strong";

    /** A value shown but not editable here. */
    String VALUE_LABEL = "static-value-label";

    /** The greyed stand-in for a value that has not been chosen yet. */
    String PLACEHOLDER = "block-placeholder";

    /** A square glyph-only button, sized to the same footprint as every other one in a row. */
    String ICON_BUTTON = "icon-button";

    /** The title line of a dialog built by hand rather than through {@code Dialog}. */
    String DIALOG_HEADING = "dialog-heading";

    /** A section title inside a dialog. */
    String DIALOG_SUBHEADING = "dialog-subheading";

    /** An explanatory line under a dialog's controls. */
    String DIALOG_HINT = "dialog-hint";

    /** A dialog root with the host's standard padding and spacing. */
    String DIALOG_COMPACT = "dialog-compact";

    /** The confirming button of a dialog — one per dialog, never two. */
    String PRIMARY_BUTTON = "primary-button";

    /** An on/off toggle whose state shows in its colour as well as its word — off one colour, on another. */
    String SWITCH = "value-switch";

    /** One cell of a thumbnail grid. */
    String TILE = "template-tile";

    /** The caption under a {@link #TILE}. */
    String TILE_NAME = "template-tile-name";

    /**
     * On the root of a window that must <em>not</em> acquire the host's chrome — a translucent surface drawn
     * over a live game, where the shell's background, border and radius are the one thing that would ruin it.
     *
     * <p>An opt-out rather than an omission: the host themes a plugin's windows for it, so a surface that
     * wants no theme has to say so. It is a marker the host reads when theming a window, so it is the one
     * name here with no rule of its own in the stylesheet.
     */
    String UNTHEMED = "unthemed-window";
}
