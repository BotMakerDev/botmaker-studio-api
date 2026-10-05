package com.botmaker.plugin.api.overlay;

import com.botmaker.plugin.api.toolbar.ActionContext.Area;

import java.util.Objects;
import java.util.Optional;

/**
 * The screen a bot looks at, as its plugin says — so the overlay opens over it without asking.
 *
 * <p>A value, one of three {@link Kind}s: a window by its title, a part of the desktop, or the project's
 * private display session. The host finds the window, docks beside it and captures it; what the plugin's own
 * value looks like in the bot ({@code CaptureSource}) never crosses.
 */
public final class Watched {

    /** Which screen. */
    public enum Kind {
        /** A window, by title: {@link #title()}. */
        WINDOW,
        /** A part of the desktop: {@link #area()}. */
        REGION,
        /** The project's private display session. */
        SESSION
    }

    private final Kind kind;
    private final String title;
    private final Area area;

    private Watched(Kind kind, String title, Area area) {
        this.kind = kind;
        this.title = title;
        this.area = area;
    }

    /** The window whose title is {@code title}, matched as the host matches a capture target's title. */
    public static Watched window(String title) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("A watched window needs a title");
        return new Watched(Kind.WINDOW, title, null);
    }

    /** This part of the desktop, in desktop pixels. */
    public static Watched region(Area area) {
        if (area == null || area.width() <= 0 || area.height() <= 0) {
            throw new IllegalArgumentException("A watched region needs a size: " + area);
        }
        return new Watched(Kind.REGION, null, area);
    }

    /** The project's private display session, when one is running. */
    public static Watched session() {
        return new Watched(Kind.SESSION, null, null);
    }

    public Kind kind() {
        return kind;
    }

    /** The window's title, for {@link Kind#WINDOW}. */
    public Optional<String> title() {
        return Optional.ofNullable(title);
    }

    /** The part of the desktop, for {@link Kind#REGION}. */
    public Optional<Area> area() {
        return Optional.ofNullable(area);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Watched that && kind == that.kind && Objects.equals(title, that.title)
                && Objects.equals(area, that.area);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, title, area);
    }

    @Override
    public String toString() {
        return switch (kind) {
            case WINDOW -> "window \"" + title + "\"";
            case REGION -> "region " + area.x() + "," + area.y() + " " + area.width() + "×" + area.height();
            case SESSION -> "the display session";
        };
    }
}
