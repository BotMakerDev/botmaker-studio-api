package com.botmaker.plugin.api.record;

import com.botmaker.plugin.api.StudioServices;

import java.awt.image.BufferedImage;
import java.util.Optional;

/**
 * A value of one of this plugin's types, read off the screen where a gesture happened.
 *
 * <p>The host fills the parameters it understands — numbers, text, keys. A parameter of a type only the plugin
 * understands, such as a picture of something on screen, is filled by asking the plugin what is at the spot.
 * The value crosses as a value; the host writes its Java.
 *
 * @param <T> the parameter type this answers
 */
public interface RecordedValue<T> {

    /** The parameter type this answers. */
    Class<T> type();

    /**
     * The value at {@code spot}, or empty when there is nothing there this plugin can name — in which case a
     * method needing it is not used for that gesture.
     *
     * <p>Called off the JavaFX thread, once per gesture. It may read the project through {@code services}.
     */
    Optional<T> at(StudioServices services, Spot spot);

    /**
     * Where a gesture happened.
     *
     * @param x     window-relative x
     * @param y     window-relative y
     * @param frame the window as it looked just before the gesture, or {@code null} when the host could not grab
     *              it
     */
    record Spot(int x, int y, BufferedImage frame) {}
}
