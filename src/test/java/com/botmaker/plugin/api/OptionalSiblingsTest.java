package com.botmaker.plugin.api;

import javafx.stage.Window;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The {@code Optional} siblings answer exactly what the nullable members answer.
 *
 * <p>Each sibling is a {@code default} method delegating to the member a host implements, so the only thing
 * that can be wrong is the delegation itself — and it is wrong silently, as an editor that never sees a value
 * the host did supply. The interesting half of every case here is therefore the <b>present</b> one, not the
 * empty one.
 *
 * <p>The deprecated members are called on purpose throughout. That is what is under test.
 */
@SuppressWarnings("deprecation")
class OptionalSiblingsTest {

    private static final TypeRef STRING = new TypeRef() {
        @Override public String simpleName() { return "String"; }
        @Override public String qualifiedName() { return "java.lang.String"; }
    };

    // ------------------------------------------------------------------
    // SlotRun.allowed — the one where two opposite absences shared one type
    // ------------------------------------------------------------------

    private static SlotRun runWithAllowed(List<String> allowed) {
        return new SlotRun() {
            @Override public List<String> elements() { return List.of("a"); }
            @Override public List<String> allowed() { return allowed; }
            @Override public void replace(List<String> javaExpressions, String... importsNeeded) {}
        };
    }

    @Test
    void anythingGoesIsEmptyAndNothingAllowedIsAnEmptyList() {
        assertTrue(runWithAllowed(null).allowedSources().isEmpty(), "null means anything goes");

        Optional<List<String>> nothing = runWithAllowed(List.of()).allowedSources();
        assertTrue(nothing.isPresent(), "an empty list is a restriction, not an absence");
        assertEquals(List.of(), nothing.orElseThrow());
    }

    @Test
    void arealRestrictionSurvivesTheDelegation() {
        List<String> only = List.of("new ImageTemplate(\"gold.png\")");
        assertEquals(only, runWithAllowed(only).allowedSources().orElseThrow());
    }

    @Test
    void theDefaultRunIsUnrestricted() {
        SlotRun bare = new SlotRun() {
            @Override public List<String> elements() { return List.of(); }
            @Override public void replace(List<String> javaExpressions, String... importsNeeded) {}
        };
        assertTrue(bare.allowedSources().isEmpty());
    }

    // ------------------------------------------------------------------
    // ValueContext.asSlot / SlotContext's four
    // ------------------------------------------------------------------

    /** A Parameters row: a value with no call site anywhere. */
    private static ValueContext row() {
        return new ValueContext() {
            @Override public TypeRef type() { return STRING; }
            @Override public List<String> value() { return List.of("x"); }
            @Override public void set(List<String> value) {}
            @Override public StudioServices services() { return null; }
        };
    }

    @Test
    void aRowHasNoSlot() {
        assertTrue(row().slot().isEmpty());
    }

    @Test
    void aSlotIsItsOwnCallSite() {
        SlotContext slot = slot("Game", "launchSteam", "Game.launchSteam(\"570\")", null);
        assertSame(slot, slot.slot().orElseThrow());
    }

    @Test
    void aResolvedCallReportsItsClassAndMethod() {
        SlotContext slot = slot("Game", "launchSteam", "Game.launchSteam(\"570\")", null);
        assertEquals("Game", slot.enclosingClassName().orElseThrow());
        assertEquals("launchSteam", slot.enclosingMethodName().orElseThrow());
        assertEquals("Game.launchSteam(\"570\")", slot.enclosingCall().orElseThrow());
    }

    @Test
    void anUnresolvedCallReportsNothingRatherThanThrowing() {
        SlotContext slot = slot(null, null, null, null);
        assertTrue(slot.enclosingClassName().isEmpty());
        assertTrue(slot.enclosingMethodName().isEmpty());
        assertTrue(slot.enclosingCall().isEmpty(), "not an argument of a call is the ordinary state");
        assertTrue(slot.siblingRun().isEmpty(), "standing alone is the ordinary state");
    }

    @Test
    void aRunIsReportedWhenThereIsOne() {
        SlotRun run = runWithAllowed(null);
        assertSame(run, slot("Matches", "any", "Matches.any(a, b)", run).siblingRun().orElseThrow());
    }

    // ------------------------------------------------------------------
    // ActionContext.projectName, Dialogs.owner
    // ------------------------------------------------------------------

    private static ActionContext action(String projectName) {
        return new ActionContext() {
            @Override public String projectName() { return projectName; }
            @Override public String pinnedVersion() { return "1.1.6"; }
            @Override public StudioServices services() { return null; }
        };
    }

    @Test
    void noProjectOpenIsEmptyAndAnOpenOneIsNamed() {
        assertTrue(action(null).openProjectName().isEmpty());
        assertEquals("MyBot", action("MyBot").openProjectName().orElseThrow());
    }

    @Test
    void anUnattachedEditorHasNoOwnerWindow() {
        Dialogs dialogs = new Dialogs() {
            @Override public Window owner() { return null; }
            @Override public Choice chooseProgram(Path initialDir) { return Choice.cancelled(true); }
            @Override public Choice chooseFile(String t, Path d, String... e) { return Choice.cancelled(true); }
            @Override public Choice chooseDirectory(String t, Path d) { return Choice.cancelled(true); }
        };
        assertFalse(dialogs.ownerWindow().isPresent());
    }

    // ------------------------------------------------------------------

    private static SlotContext slot(String owner, String method, String call, SlotRun run) {
        return new SlotContext() {
            @Override public TypeRef type() { return STRING; }
            @Override public List<String> value() { return List.of("\"570\""); }
            @Override public void set(List<String> value) {}
            @Override public StudioServices services() { return null; }
            @Override public String currentSource() { return "\"570\""; }
            @Override public String enclosingClass() { return owner; }
            @Override public String enclosingMethod() { return method; }
            @Override public int argIndex() { return 0; }
            @Override public String enclosingSource() { return call; }
            @Override public void replaceWith(String javaExpression, String... importsNeeded) {}
            @Override public SlotRun run() { return run; }
        };
    }
}
