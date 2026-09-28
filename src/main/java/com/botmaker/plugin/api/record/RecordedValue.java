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

    /**
     * <b>The way to declare one</b>: {@code RecordedValue.of(ImageTemplate.class).at(PictureAt::find)}, where
     * {@code find} is {@code Optional<ImageTemplate> find(StudioServices, Spot)}.
     */
    static <T> Steps<T> of(Class<T> type) {
        return new Steps<>(type);
    }

    /** After {@link #of}: what is at a spot. */
    final class Steps<T> {

        private final Class<T> type;

        private Steps(Class<T> type) {
            if (type == null) throw new IllegalArgumentException("No type given");
            this.type = type;
        }

        /** Answered by {@code at}; see {@link RecordedValue#at}. */
        public RecordedValue<T> at(java.util.function.BiFunction<StudioServices, Spot, Optional<T>> at) {
            if (at == null) throw new IllegalArgumentException(type.getName() + ": nothing reads the spot");
            Class<T> declared = type;
            return new RecordedValue<>() {
                @Override
                public Class<T> type() {
                    return declared;
                }

                @Override
                public Optional<T> at(StudioServices services, Spot spot) {
                    Optional<T> found = at.apply(services, spot);
                    return found == null ? Optional.empty() : found;
                }

                @Override
                public String toString() {
                    return "RecordedValue[" + declared.getName() + "]";
                }
            };
        }
    }

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
