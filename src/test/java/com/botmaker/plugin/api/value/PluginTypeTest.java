package com.botmaker.plugin.api.value;

import com.botmaker.plugin.api.slot.ValueContext;
import javafx.scene.Node;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two laws a declared type has to keep, checked against a type declared the way a plugin declares one.
 *
 * <p>There is nothing else here to test — every method is abstract, so a type that forgot one does not
 * compile, which is the whole reason the four old declarations became one. What the compiler cannot ask for
 * is that the two halves <em>agree</em>: that taking a value apart and putting it back gives the value
 * again, and that a fresh one survives that trip. A type whose {@code build} does not invert its
 * {@code components} writes a user's file and then reads it back as something else, which is exactly what
 * {@code ValueCodec.literal(parse(…))} did to a colour.
 *
 * <p>{@code botmaker plugin validate} makes the same two checks over every type a plugin declares, which is
 * the check the deleted id-uniqueness one could never make.
 */
class PluginTypeTest {

    record Point(int x, int y) {}

    /** What a plugin writes: one class, both interfaces, no id and no text anywhere. */
    static final class PointType implements PluginType<Point>, ComponentType<Point> {
        @Override public Class<Point> type()   { return Point.class; }
        @Override public Point fresh()         { return new Point(0, 0); }
        @Override public Node editor(ValueContext ctx) { return null; }

        @Override public List<Class<?>> componentTypes()  { return List.of(int.class, int.class); }
        @Override public List<Object> components(Point p) { return List.of(p.x(), p.y()); }
        @Override public Point build(List<Object> parts) {
            return new Point((int) parts.get(0), (int) parts.get(1));
        }
    }

    private static final PointType POINT = new PointType();

    /** The law: {@code build(components(v))} is {@code v}, for a fresh one and for an edited one. */
    @Test
    void aComponentTypeRoundTripsItsOwnValues() {
        assertEquals(POINT.fresh(), POINT.build(POINT.components(POINT.fresh())));

        Point moved = new Point(12, -40);
        assertEquals(moved, POINT.build(POINT.components(moved)));
    }

    /** A fresh value is a real one, every time it is asked for — never cached, never null. */
    @Test
    void freshIsAValueAndNotAnExpression() {
        assertEquals(new Point(0, 0), POINT.fresh());
        assertEquals(POINT.fresh(), POINT.fresh());
    }

    /** The components are typed, and they line up with what {@code componentTypes} promised. */
    @Test
    void theComponentsLineUpWithTheTypesTheyWerePromisedAs() {
        assertEquals(POINT.componentTypes().size(), POINT.components(POINT.fresh()).size());
        assertEquals(List.of(0, 0), POINT.components(POINT.fresh()));
    }

    /** The wildcard the host holds every declaration behind, and the one cast that makes it usable. */
    @Test
    void theHostReadsAValueItHoldsAsObject() {
        ComponentType<?> held = POINT;
        Object value = new Point(3, 4);

        assertEquals(List.of(3, 4), held.componentsOf(value));
    }

    /** A constructor is the default spelling; a factory says its name and nothing else. */
    @Test
    void aConstructorIsTheDefaultSpelling() {
        assertEquals("", POINT.factory());
        assertSame(Point.class, POINT.factoryOwner());
    }

    /** A type that is picked but whose Java needs no taking apart declares one interface, not two. */
    @Test
    void aTypeMayBePickedWithoutBeingComposite() {
        PluginType<String> text = new PluginType<>() {
            @Override public Class<String> type() { return String.class; }
            @Override public String fresh() { return ""; }
            @Override public Node editor(ValueContext ctx) { return null; }
        };

        assertEquals("", text.fresh());
        assertTrue(!(text instanceof ComponentType<?>), "a JDK literal has no components");
        assertEquals(null, text.preview(null), "and no preview until it wants one");
    }
}
