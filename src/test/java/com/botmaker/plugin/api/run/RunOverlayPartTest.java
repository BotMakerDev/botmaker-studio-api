package com.botmaker.plugin.api.run;

import com.botmaker.plugin.api.Runs;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunOverlayPartTest {

    private static final RunOverlayPart.Factory BAR = ctx -> null;
    private static final RunOverlayPart.Factory LAYER = ctx -> null;

    @Test
    void aPartDrawsInTheBarTheLayerOrBoth() {
        RunOverlayPart barOnly = RunOverlayPart.id("x.bar").bar(BAR);
        assertSame(BAR, barOnly.barFactory().orElseThrow());
        assertTrue(barOnly.layerFactory().isEmpty());

        RunOverlayPart layerOnly = RunOverlayPart.id("x.layer").layer(LAYER);
        assertTrue(layerOnly.barFactory().isEmpty());
        assertSame(LAYER, layerOnly.layerFactory().orElseThrow());

        RunOverlayPart both = RunOverlayPart.id("x.both").bar(BAR).layer(LAYER);
        assertEquals("x.both", both.id());
        assertSame(BAR, both.barFactory().orElseThrow());
        assertSame(LAYER, both.layerFactory().orElseThrow());
    }

    @Test
    void aBlankIdAndAMissingFactoryAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> RunOverlayPart.id(" "));
        assertThrows(NullPointerException.class, () -> RunOverlayPart.id("x").bar(null));
        assertThrows(NullPointerException.class, () -> RunOverlayPart.id("x").layer(null));
        assertThrows(NullPointerException.class, () -> RunOverlayPart.id("x").bar(BAR).layer(null));
    }

    @Test
    void aHostThatRunsNothingCannotPause() {
        assertFalse(Runs.NONE.canPause());
        assertFalse(Runs.NONE.isPaused());
        Runs.NONE.pause();
        Runs.NONE.resume();
        assertFalse(Runs.NONE.isPaused());
    }
}
