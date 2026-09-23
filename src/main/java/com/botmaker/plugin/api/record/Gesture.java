package com.botmaker.plugin.api.record;

/**
 * What the host recognised in a recording — a fact about the mouse and the keyboard, not a plugin's vocabulary.
 *
 * <p>Each gesture carries its <b>values</b> in a fixed order, and the host hands them to the parameters of the
 * {@link Records} method it picked, left to right (see {@link Records} for the filling rule). Coordinates are
 * relative to the window the recording was made over.
 */
public enum Gesture {

    /** A left click. Values: {@code x, y}. */
    CLICK,

    /** Two left clicks in quick succession at one spot. Values: {@code x, y}. */
    DOUBLE_CLICK,

    /** A right click. Values: {@code x, y}. */
    RIGHT_CLICK,

    /** A middle click. Values: {@code x, y}. */
    MIDDLE_CLICK,

    /** The left button held down while the pointer travelled. Values: {@code x1, y1, x2, y2, durationMs}. */
    DRAG,

    /** The wheel turned away from the user. Values: {@code notches}. */
    SCROLL_UP,

    /** The wheel turned towards the user. Values: {@code notches}. */
    SCROLL_DOWN,

    /** A burst of printable keys. Values: {@code text}. */
    TYPE,

    /** One named key — Enter, Escape, an arrow, a function key. Values: {@code key}. */
    KEY,

    /** Modifiers held while a key was pressed — Ctrl+S. Values: {@code key…}, modifiers first. */
    COMBO,

    /** Nothing happened for a while. Values: {@code milliseconds}. */
    PAUSE,

    /**
     * A pause that ended in a click on something the host could name through a {@link RecordedValue} — waiting
     * for it to appear. Values: {@code timeoutSeconds}; the thing waited for is resolved at the click's spot.
     */
    AWAIT
}
