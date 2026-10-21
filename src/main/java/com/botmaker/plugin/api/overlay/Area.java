package com.botmaker.plugin.api.overlay;

/**
 * Where something sits on screen, in screen pixels: the watched window ({@link FrameContext#watchedArea}), a box
 * the host draws ({@link Marks}), a region the user drags ({@link OverlayToolContext#pickRegion}), and the window
 * a toolbar click's overlay is drawn over ({@code ActionContext.overBounds}).
 */
public record Area(int x, int y, int width, int height) {
}
