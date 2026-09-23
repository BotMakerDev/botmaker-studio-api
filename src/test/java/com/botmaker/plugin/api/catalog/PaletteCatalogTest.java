package com.botmaker.plugin.api.catalog;

import com.botmaker.plugin.api.palette.Hidden;
import com.botmaker.plugin.api.palette.Palette;
import com.botmaker.plugin.api.palette.PaletteDefault;
import com.botmaker.plugin.api.palette.PaletteLabel;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The claims {@link PaletteCatalog#of(Class[])} rests on. Two matter more than the rest and are the reason
 * this file exists at all:
 *
 * <ul>
 *   <li><b>member order is source order</b>, which the reflective API does not report and
 *       {@link SourceOrder} recovers from the class file — the one capability the deleted annotation
 *       processor had that reflection alone does not;
 *   <li><b>a malformed catalog degrades</b>. Every check reports into {@link PaletteCatalog#problems()} and
 *       builds the rest, because this runs while a project is opening.
 * </ul>
 */
class PaletteCatalogTest {

    @Palette(category = "demo", categoryLabel = "Demo", icon = "🧪", order = 10)
    @SuppressWarnings("unused")
    static final class Widget {

        // Deliberately not alphabetical: zoom, apply, click — the order the menu must show.
        public static void zoom() {
        }

        public static void apply(int times) {
        }

        @PaletteDefault
        public static void click(int x, int y) {
        }

        public static void click(String target) {
        }

        @Hidden("plumbing")
        public static void reset() {
        }

        @PaletteLabel("Find any of…")
        public static void findAny(String... names) {
        }

        @Override
        public String toString() {
            return "Widget";
        }
    }

    @Palette(category = "demo", order = 20)
    @Hidden("a value type: recognised, never proposed")
    record Coordinate(int x, int y) {
    }

    static final class Unmarked {
    }

    @Palette(category = "demo", categoryLabel = "Demonstration", order = 30)
    static final class Disagrees {
    }

    @Palette(category = "demo", order = 40)
    @SuppressWarnings("unused")
    static final class TwoLeads {

        @PaletteDefault
        public static void go() {
        }

        @PaletteDefault
        public static void go(int n) {
        }
    }

    private static PaletteCatalog demo() {
        return PaletteCatalog.of(Widget.class, Coordinate.class);
    }

    @Test
    void catalogsInOrderAndReadsTheAnnotations() {
        PaletteCatalog catalog = demo();
        assertEquals(List.of(), catalog.problems());
        assertEquals(List.of("Widget", "Coordinate"), catalog.facades().stream()
                .map(FacadeEntry::simpleName).toList());

        FacadeEntry widget = catalog.facade(Widget.class).orElseThrow();
        assertEquals("Demo", widget.category().label());
        assertEquals("🧪", widget.icon());
        assertTrue(widget.offered());
    }

    /** Hidden on the type keeps the entry and drops it from the menus — the old HIDDEN and VALUE, collapsed. */
    @Test
    void hiddenOnATypeIsCataloguedButNotOffered() {
        PaletteCatalog catalog = demo();
        assertTrue(catalog.offers(Coordinate.class), "a hidden type is still catalogued");
        assertFalse(catalog.facade(Coordinate.class).orElseThrow().offered());
        assertEquals(List.of("Widget"), catalog.offeredFacades().stream()
                .map(FacadeEntry::simpleName).toList());
    }

    /**
     * The claim {@link SourceOrder} exists for. Alphabetically this would be
     * {@code apply, click, findAny, zoom}.
     */
    @Test
    void memberOrderIsSourceOrderNotAlphabetical() {
        List<String> names = demo().facade(Widget.class).orElseThrow().members().stream()
                .map(m -> m.id().name()).distinct().toList();
        assertEquals(List.of("zoom", "apply", "click", "findAny"), names);
    }

    @Test
    void hiddenDropsTheWholeNameAndObjectOverridesNeedNoAnnotation() {
        FacadeEntry widget = demo().facade(Widget.class).orElseThrow();
        assertFalse(widget.offers("reset"));
        assertFalse(widget.offers("toString"));
    }

    /** A palette entry is a name: the lead is what the menu inserts, the rest are the submenu behind it. */
    @Test
    void paletteDefaultDecidesTheLeadOverParameterCount() {
        FacadeEntry widget = demo().facade(Widget.class).orElseThrow();
        assertEquals(List.of("(int,int)", "(java.lang.String)"), widget.overloads("click").stream()
                .map(m -> "(" + String.join(",", m.id().parameterTypeNames()) + ")").toList());
    }

    @Test
    void aLabelNamesTheWholeFamily() {
        assertEquals("Find any of…",
                demo().facade(Widget.class).orElseThrow().member("findAny").orElseThrow().displayLabel());
    }

    @Test
    void anUnmarkedClassIsReportedAndSkipped() {
        PaletteCatalog catalog = PaletteCatalog.of(Widget.class, Unmarked.class);
        assertTrue(catalog.offers(Widget.class), "the well-formed facade still builds");
        assertEquals(1, catalog.problems().size());
        assertTrue(catalog.problems().getFirst().contains("no @Palette"), catalog.problems().toString());
    }

    @Test
    void twoLabelsForOneCategoryAreReportedAndTheFirstWins() {
        PaletteCatalog catalog = PaletteCatalog.of(Widget.class, Disagrees.class);
        assertEquals("Demo", catalog.facade(Disagrees.class).orElseThrow().category().label());
        assertEquals(1, catalog.problems().size());
    }

    @Test
    void twoLeadsOnOneNameAreReportedAndTheCatalogStillBuilds() {
        PaletteCatalog catalog = PaletteCatalog.of(TwoLeads.class);
        assertTrue(catalog.facade(TwoLeads.class).orElseThrow().offers("go"));
        assertEquals(1, catalog.problems().size());
        assertTrue(catalog.problems().getFirst().contains("@PaletteDefault"), catalog.problems().toString());
    }

    @Test
    void mergingIsAdditiveAndTheLaterCatalogWinsTheEntry() {
        PaletteCatalog merged = PaletteCatalog.of(Widget.class).mergedWith(PaletteCatalog.of(Coordinate.class));
        assertEquals(List.of("Widget", "Coordinate"), merged.facades().stream()
                .map(FacadeEntry::simpleName).toList());
        assertEquals(merged, merged.mergedWith(PaletteCatalog.empty()));
    }
}
