package com.botmaker.plugin.api;

import com.botmaker.plugin.api.overlay.OverlayPart;
import com.botmaker.plugin.api.run.RunOverlayPart;
import com.botmaker.plugin.api.source.ManagedValue;
import com.botmaker.plugin.api.value.PluginType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeclaredPluginTest {

    static final ManagedValue<String> NOTE = ManagedValue.method("note").in("Notes").holds(String.class, "")
            .because("Mine.");

    static final class Declared extends DeclaredPlugin {
        Declared(AtomicInteger asked) {
            super(StudioPlugin.id("com.example.notes").named("Notes")
                    .values(() -> {
                        asked.incrementAndGet();
                        return List.of(NOTE);
                    }));
        }
    }

    @Test
    void aSurfaceLeftOutContributesNothing() {
        Declared plugin = new Declared(new AtomicInteger());
        assertEquals("com.example.notes", plugin.id());
        assertEquals("Notes", plugin.displayName());
        assertTrue(plugin.types().isEmpty());
        assertTrue(plugin.componentTypes().isEmpty());
        assertTrue(plugin.slotEditors().isEmpty());
        assertTrue(plugin.toolbarItems().isEmpty());
        assertTrue(plugin.recordedValues().isEmpty());
        assertTrue(plugin.runOverlayParts().isEmpty());
        assertTrue(plugin.catalog().isEmpty());
    }

    @Test
    void runOverlayPartsAreDeclaredLikeTheToolbar() {
        RunOverlayPart part = RunOverlayPart.id("notes.run").bar(ctx -> null);
        StudioPlugin plugin = new DeclaredPlugin(StudioPlugin.id("com.example.z").named("Z")
                .runOverlay(() -> List.of(part))) {};
        assertEquals(List.of(part), plugin.runOverlayParts());
    }

    @Test
    void anOverlayPartIsAskedForOnlyWhenTheHostAsks() {
        AtomicInteger asked = new AtomicInteger();
        OverlayPart part = OverlayPart.of().targets(Runnable.class, "Home");
        StudioPlugin plugin = new DeclaredPlugin(StudioPlugin.id("com.example.o").named("O")
                .overlay(() -> {
                    asked.incrementAndGet();
                    return part;
                })) {};
        assertEquals(0, asked.get());
        assertEquals(Optional.of(part), plugin.overlayPart());
        assertEquals(1, asked.get());
        assertTrue(new Declared(new AtomicInteger()).overlayPart().isEmpty());
        assertTrue(new DeclaredPlugin(StudioPlugin.id("com.example.e").named("E")
                .overlay(OverlayPart::of)) {}.overlayPart().isEmpty(), "a part declaring nothing is no part");
    }

    @Test
    void nothingIsBuiltUntilTheHostAsks() {
        AtomicInteger asked = new AtomicInteger();
        Declared plugin = new Declared(asked);
        assertEquals(0, asked.get(), "constructing a plugin must build nothing");
        assertEquals(List.of(NOTE), plugin.managedValues());
        assertEquals(1, asked.get());
    }

    @Test
    void aBlankNameIsTheId() {
        StudioPlugin plugin = new DeclaredPlugin(StudioPlugin.id("com.example.x").named(" ")) {};
        assertEquals("com.example.x", plugin.displayName());
        assertThrows(IllegalArgumentException.class, () -> StudioPlugin.id(" "));
    }

    @Test
    void aTypeListIsCopied() {
        StudioPlugin plugin = new DeclaredPlugin(StudioPlugin.id("com.example.y").named("Y")
                .types(() -> List.of(PluginType.value(int.class).fresh(() -> 0).editor(() -> ctx -> null)
                        .writtenAsLiteral()))) {};
        assertEquals(1, plugin.types().size());
        assertEquals(int.class, plugin.types().getFirst().type());
    }
}
