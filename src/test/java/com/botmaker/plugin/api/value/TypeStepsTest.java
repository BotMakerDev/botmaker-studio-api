package com.botmaker.plugin.api.value;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The declaration steps: a method reference names the factory the host writes, the accessors take a value
 * apart in the factory's order, and invoking the factory builds it back — so {@code build(components(v))}
 * is {@code v} with nothing written by hand.
 */
class TypeStepsTest {

    /** A record whose Java is its canonical constructor. */
    public record Spot(int x, int y) {}

    /** A varargs factory, and a chain on an instance. */
    public record Keys(List<String> names, int held) {
        public static Keys of(String... names) {
            return new Keys(List.of(names), 0);
        }

        public Keys held(int ms) {
            return new Keys(names, ms);
        }
    }

    private static final Drawn NONE = () -> ctx -> null;

    @Test
    void aRecordIsItsCanonicalConstructor() {
        DeclaredCallType<Spot> spot = PluginType.value(Spot.class).fresh(() -> new Spot(0, 0)).editor(NONE)
                .writtenAsRecord();
        assertInstanceOf(Constructor.class, spot.factory());
        assertEquals(List.of(int.class, int.class), spot.componentTypes());
        Spot value = new Spot(3, 4);
        assertEquals(value, spot.build(spot.components(value)));
        assertEquals(new Spot(0, 0), spot.fresh());
    }

    @Test
    void theAccessorsPickAnOverloadedFactory() {
        DeclaredCallType<LocalDate> date = PluginType.value(LocalDate.class).fresh(() -> LocalDate.of(2000, 1, 1))
                .editor(NONE)
                .writtenAs(LocalDate::of, LocalDate::getYear, LocalDate::getMonthValue, LocalDate::getDayOfMonth);
        Method factory = (Method) date.factory();
        assertEquals("of", factory.getName());
        assertEquals(List.of(int.class, int.class, int.class), date.componentTypes());
        LocalDate value = LocalDate.of(2026, 9, 28);
        assertEquals(value, date.build(date.components(value)));
    }

    @Test
    void partsAreCoercedToTheParameterTheyFill() {
        DeclaredCallType<Duration> duration = PluginType.value(Duration.class).fresh(() -> Duration.ZERO)
                .editor(NONE).writtenAs(Duration::ofMillis, Duration::toMillis);
        // The host reads a literal 1500 as an Integer; the factory takes a long.
        assertEquals(Duration.ofMillis(1500), duration.build(List.of(1500)));
        assertNull(duration.build(List.of("1500")), "a part of the wrong kind is not this call");
        assertNull(duration.build(List.of()), "too few parts is not this call");
    }

    @Test
    void aFactoryThatRefusesItsPartsBuildsNothing() {
        DeclaredCallType<LocalDate> date = PluginType.value(LocalDate.class).fresh(() -> LocalDate.EPOCH)
                .editor(NONE)
                .writtenAs(LocalDate::of, LocalDate::getYear, LocalDate::getMonthValue, LocalDate::getDayOfMonth);
        assertNull(date.build(List.of(2026, 13, 1)));
    }

    @Test
    void aVarargsFactoryTakesItsRunAsOneCollection() {
        DeclaredCallType<Keys> keys = PluginType.value(Keys.class).fresh(() -> Keys.of("CTRL"))
                .editor(NONE).writtenAsEach(Keys::of, Keys::names);
        assertEquals(List.of(String.class), keys.componentTypes(), "the varargs part is its element type");
        Keys value = Keys.of("CTRL", "S");
        assertEquals(List.of("CTRL", "S"), keys.components(value));
        assertEquals(value, keys.build(keys.components(value)));
    }

    @Test
    void aChainTakesItsReceiverAsPartZero() {
        DeclaredCall<Keys> held = ComponentType.part(Keys.class).writtenAs(Keys::held, k -> k.held(0), Keys::held);
        assertEquals(List.of(Keys.class, int.class), held.componentTypes());
        Keys value = Keys.of("A").held(200);
        assertEquals(value, held.build(held.components(value)));
    }

    @Test
    void aFactoryMayAnswerTheTypesSupertype() {
        DeclaredCall<ZoneOffset> utc = ComponentType.part(ZoneOffset.class)
                .writtenAs(ZoneOffset::ofHoursMinutes, o -> o.getTotalSeconds() / 3600, o -> (o.getTotalSeconds() % 3600) / 60)
                .constants(ZoneOffset.UTC);
        assertEquals("UTC", utc.constants().getFirst().getName());
        assertEquals(ZoneOffset.ofHoursMinutes(2, 30), utc.build(List.of(2, 30)));
    }

    @Test
    void aConstantThatIsNotOneIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> ComponentType.part(ZoneOffset.class)
                .writtenAs(ZoneOffset::ofHours, o -> o.getTotalSeconds() / 3600)
                .constants(ZoneOffset.ofHours(3)));
    }

    /** A plugin's own reference shape, for a factory past {@link Ref.Of10}. */
    interface MyOf2<A, B, R> extends Ref {
        R call(A a, B b);
    }

    @Test
    void aPluginsOwnReferenceShapeTakesAnyNumberOfParts() {
        MyOf2<Integer, Integer, ZoneOffset> factory = ZoneOffset::ofHoursMinutes;
        DeclaredCall<ZoneOffset> offset = ComponentType.part(ZoneOffset.class)
                .writtenAs(factory, o -> o.getTotalSeconds() / 3600, o -> (o.getTotalSeconds() % 3600) / 60);
        assertEquals("ofHoursMinutes", offset.factory().getName());
        assertEquals(ZoneOffset.ofHoursMinutes(1, 30), offset.build(offset.components(ZoneOffset.ofHoursMinutes(1, 30))));
        assertThrows(IllegalArgumentException.class, () -> ComponentType.part(ZoneOffset.class)
                .writtenAs(factory, o -> o.getTotalSeconds() / 3600), "one accessor per part, counted");
    }

    @Test
    void aLambdaIsNotAFactory() {
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> ComponentType.part(Duration.class).writtenAs((Long ms) -> Duration.ofMillis(ms), Duration::toMillis));
        assertTrue(refused.getMessage().contains("method reference"), refused.getMessage());
    }

    @Test
    void anEnumIsItsConstantAndStartsAtTheFirst() {
        DeclaredType<DayOfWeek> day = PluginType.value(DayOfWeek.class).firstConstant().editor(NONE)
                .writtenAsConstant();
        assertEquals(DayOfWeek.MONDAY, day.fresh());
        assertThrows(IllegalArgumentException.class, () -> PluginType.value(String.class).firstConstant());
        assertThrows(IllegalArgumentException.class,
                () -> PluginType.value(Duration.class).fresh(() -> Duration.ZERO).editor(NONE).writtenAsConstant());
    }

    @Test
    void onlyAJdkLeafIsALiteral() {
        assertEquals(0, PluginType.value(int.class).fresh(() -> 0).editor(NONE).writtenAsLiteral().fresh());
        assertThrows(IllegalArgumentException.class,
                () -> PluginType.value(Duration.class).fresh(() -> Duration.ZERO).editor(NONE).writtenAsLiteral());
    }

    @Test
    void aFilledTypeIsACallTheBotMakes() {
        DeclaredType<LocalDate> today = PluginType.value(LocalDate.class).filledBy(LocalDate::now).shownAs(NONE);
        assertNull(today.fresh());
        assertEquals("now", today.freshCall().getName());
        assertEquals(0, today.freshCall().getParameterCount());
    }
}
