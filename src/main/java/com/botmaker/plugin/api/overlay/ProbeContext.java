package com.botmaker.plugin.api.overlay;

import com.botmaker.plugin.api.toolbar.ActionContext.Area;

import java.awt.image.BufferedImage;
import java.lang.reflect.Executable;
import java.util.Optional;

/**
 * What a {@link Probe} is given: the call it probes, that call's arguments as values, and the watched screen
 * as it is now.
 *
 * <p>Called <b>off the JavaFX thread</b>, up to a few times a second for the row the user is on. A probe reads
 * and answers; it never clicks, types or writes.
 */
public interface ProbeContext extends OverlayContext {

    /** The probed call, resolved on the plugin's loader: the one its {@link OverlayPart#probe} named. */
    Executable call();

    /**
     * The call's argument at {@code index}, read by the host as a value of {@code type} — a managed
     * constant included; empty when it cannot be read (a local variable, a call, a varargs tail).
     */
    <T> Optional<T> argument(int index, Class<T> type);

    /** The watched screen now, empty when it cannot be captured. */
    Optional<BufferedImage> frame();

    /** Where {@link #frame()} sits, in the bot's pixels (see the package comment). */
    Optional<Area> watchedArea();
}
