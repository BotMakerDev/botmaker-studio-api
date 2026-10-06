package com.botmaker.plugin.api.overlay;

import com.botmaker.plugin.api.toolbar.ActionContext.Area;

import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * What the bot sees now, as its plugin grabbed it: the image and where it sits, in the bot's pixels (see the
 * package comment). A value: {@link #at} builds one.
 */
public final class OverlayFrame {

    private final BufferedImage image;
    private final Area area;

    private OverlayFrame(BufferedImage image, Area area) {
        this.image = image;
        this.area = area;
    }

    /** {@code image}, whose top-left pixel is {@code x,y} in the bot's pixels. */
    public static OverlayFrame at(BufferedImage image, int x, int y) {
        Objects.requireNonNull(image, "image");
        return new OverlayFrame(image, new Area(x, y, image.getWidth(), image.getHeight()));
    }

    public BufferedImage image() {
        return image;
    }

    /** Where {@link #image()} sits in the bot's pixels: its origin and its size. */
    public Area area() {
        return area;
    }
}
