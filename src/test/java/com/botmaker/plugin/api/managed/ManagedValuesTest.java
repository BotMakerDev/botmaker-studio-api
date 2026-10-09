package com.botmaker.plugin.api.managed;

import com.botmaker.plugin.api.source.ManagedValue;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a bot's {@code main} gets for naming a plugin's values class, and what it is spared. Moved from
 * {@code botmaker-plugin-basics} with the class (2026-09-28), and typed by the declaration since.
 *
 * <p>Every test claims an <b>id of its own</b> rather than resetting the registry between them: the claims are
 * static because a plugin registers them once, and distinct ids assert that two plugins' ids do not collide.
 */
class ManagedValuesTest {

    private static ManagedValue<String> text(String id) {
        return ManagedValue.method(id).in("Values").holds(String.class, "").because("Mine.");
    }

    static final class Values {

        @Managed("test.flow")
        public static String flow() {
            return "the flow";
        }

        @Managed("test.capture")
        public static String capture() {
            return "the capture source";
        }
    }

    @Test
    void everyManagedMethodReachesWhoeverClaimedItsId() {
        List<String> taken = new ArrayList<>();
        ManagedValues.claim(text("test.flow"), value -> taken.add("flow=" + value));
        ManagedValues.claim(text("test.capture"), value -> taken.add("capture=" + value));

        ManagedValues.install(Values.class);

        // Name order, which is what install sorts by: capture before flow.
        assertEquals(List.of("capture=the capture source", "flow=the flow"), taken);
    }

    static final class WrongType {

        @Managed("test.wrong-type")
        public static Integer count() {
            return 3;
        }
    }

    @Test
    void aValueOfAnotherTypeNeverReachesTheSink() {
        List<Object> taken = new ArrayList<>();
        ManagedValues.claim(text("test.wrong-type"), taken::add);

        ManagedValues.install(WrongType.class);

        assertTrue(taken.isEmpty(), () -> "a String sink was handed " + taken);
    }

    static final class Unclaimed {

        @Managed("test.nobody-reads-this")
        public static String value() {
            return "ignored";
        }
    }

    @Test
    void anIdNoPluginClaimsIsIgnoredRatherThanThrown() {
        ManagedValues.install(Unclaimed.class);
    }

    @SuppressWarnings("unused")
    static final class NotValues {

        @Managed("test.not-public")
        static String hidden() {
            return "no";
        }

        @Managed("test.not-static")
        public String perInstance() {
            return "no";
        }

        @Managed("test.takes-arguments")
        public static String computed(String argument) {
            return argument;
        }

        public static String helper() {
            return "no";
        }
    }

    @Test
    void onlyAPublicStaticNoArgumentMethodIsAValue() {
        List<String> taken = new ArrayList<>();
        for (String id : List.of("test.not-public", "test.not-static", "test.takes-arguments")) {
            ManagedValues.claim(text(id), value -> taken.add(id));
        }

        ManagedValues.install(NotValues.class);

        assertTrue(taken.isEmpty(), () -> "installed something that is not a value: " + taken);
    }

    @Managed("test.pictures")
    static final class Pictures {

        public static final String COLLECT = "collect.png";
    }

    @Test
    void managedOnATypeInstallsNothingAndAnOpenSetCannotBeClaimed() {
        List<Object> taken = new ArrayList<>();
        ManagedValues.claim(ManagedValue.openSet("test.pictures").of(String.class).in("Pictures").because("Mine."),
                taken::add);
        ManagedValues.claim(text("test.pictures"), taken::add);

        ManagedValues.install(Pictures.class);

        assertTrue(taken.isEmpty(), () -> "installed a type-level @Managed: " + taken);
    }

    static final class Opened {

        @Managed("test.opened")
        public static String opened() {
            return "opened, never created";
        }
    }

    /** A value the host may only open is still a method's value: it was taken for an open set until 2026-10-05. */
    @Test
    void aValueTheHostNeverCreatesIsStillClaimed() {
        ManagedValue<String> opened = ManagedValue.method("test.opened").openedOnly().holds(String.class)
                .because("Mine.");
        List<String> taken = new ArrayList<>();
        ManagedValues.claim(opened, taken::add);

        ManagedValues.install(Opened.class);

        assertEquals(ManagedValue.Shape.METHOD, opened.shape());
        assertEquals(List.of("opened, never created"), taken);
    }

    /** The 0.3 steps a released plugin's static initialiser calls still link, untyped and never claimed. */
    @Test
    @SuppressWarnings("deprecation")
    void theUntypedStepsOfAnOlderContractStillBuildAValue() {
        ManagedValue<Void> set = ManagedValue.openSet("test.old-set").in("Old").because("Mine.");
        ManagedValue<Void> opened = ManagedValue.method("test.old-opened").notCreated().because("Mine.");
        List<Object> taken = new ArrayList<>();
        ManagedValues.claim(opened, taken::add);

        assertTrue(set.isOpenSet());
        assertNull(set.type());
        assertNull(set.cast("x"));
        assertEquals(ManagedValue.Shape.METHOD, opened.shape());
        assertNull(opened.type());
        assertTrue(taken.isEmpty());
    }

    @Test
    void aSetOfPrimitivesIsRefusedAsItIsDeclared() {
        assertThrows(IllegalArgumentException.class, () -> ManagedValue.openSet("test.ints").of(int.class));
    }

    @Test
    void anOpenSetKnowsWhatEachConstantIs() {
        ManagedValue<String> set = ManagedValue.openSet("test.set").of(String.class).in("Set").because("Mine.");

        assertTrue(set.isOpenSet());
        assertEquals(String.class, set.type());
        assertEquals("x", set.cast("x"));
        assertNull(set.cast(3));
    }

    static final class OneThrows {

        @Managed("test.throws")
        public static String broken() {
            throw new IllegalStateException("the bot author's own code");
        }

        @Managed("test.survives")
        public static String fine() {
            return "installed anyway";
        }
    }

    @Test
    void aValueThatThrowsCostsOnlyItself() {
        List<String> taken = new ArrayList<>();
        ManagedValues.claim(text("test.throws"), value -> taken.add("throws=" + value));
        ManagedValues.claim(text("test.survives"), value -> taken.add("survives=" + value));

        ManagedValues.install(OneThrows.class);

        assertEquals(List.of("survives=installed anyway"), taken);
    }

    /** A plugin's own marker, as {@link ManagedMarker} asks for it: its {@code value()} an enum nested in it. */
    @ManagedMarker
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface TestValue {

        Id value();

        enum Id { GREETING, FAREWELL, SET }
    }

    static final class Typed {

        @TestValue(TestValue.Id.GREETING)
        public static String greeting() {
            return "hello";
        }

        @TestValue(TestValue.Id.FAREWELL)
        public static String farewell() {
            return "bye";
        }
    }

    @Test
    void aTypedIdIsTheEnumsBinaryNameAndTheConstant() {
        ManagedValue<String> greeting = ManagedValue.method(TestValue.Id.GREETING).in("Values")
                .holds(String.class, "").because("Mine.");
        ManagedValue<String> set = ManagedValue.openSet(TestValue.Id.SET).of(String.class).in("Set")
                .because("Mine.");

        String ids = TestValue.Id.class.getName();
        assertEquals(ids + ".GREETING", greeting.id());
        assertEquals(TestValue.class.getName(), greeting.marker());
        assertEquals(ids + ".SET", set.id());
        assertEquals(TestValue.class.getName(), set.marker());
        assertNull(text("test.untyped").marker());
    }

    @Test
    void aMethodMarkedWithAPluginsAnnotationReachesWhoeverClaimedItsConstant() {
        List<String> taken = new ArrayList<>();
        ManagedValues.claim(ManagedValue.method(TestValue.Id.GREETING).in("Values").holds(String.class, "")
                .because("Mine."), value -> taken.add("greeting=" + value));

        ManagedValues.install(Typed.class);

        // FAREWELL is unclaimed: reported, never thrown.
        assertEquals(List.of("greeting=hello"), taken);
    }

    enum Loose { FLOW }

    @Retention(RetentionPolicy.RUNTIME)
    @interface Unmarked {

        Id value();

        enum Id { FLOW }
    }

    @ManagedMarker
    @Retention(RetentionPolicy.RUNTIME)
    @interface WrongElement {

        String value();

        enum Id { FLOW }
    }

    /** Class retention, the default: declared fine, and a bot's runtime would never see it. */
    @ManagedMarker
    @interface NotAtRuntime {

        Id value();

        enum Id { FLOW }
    }

    @ManagedMarker
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface MethodsOnly {

        Id value();

        enum Id { PICTURES }
    }

    @Test
    void aTypedIdMustBeAConstantOfTheEnumInAMarkedAnnotation() {
        assertThrows(IllegalArgumentException.class, () -> ManagedValue.method(Loose.FLOW));
        assertThrows(IllegalArgumentException.class, () -> ManagedValue.method(Unmarked.Id.FLOW));
        assertThrows(IllegalArgumentException.class, () -> ManagedValue.openSet(WrongElement.Id.FLOW));
        assertThrows(IllegalArgumentException.class, () -> ManagedValue.method(NotAtRuntime.Id.FLOW));
        assertThrows(IllegalArgumentException.class, () -> ManagedValue.openSet(MethodsOnly.Id.PICTURES));
        ManagedValue.method(MethodsOnly.Id.PICTURES);
        assertThrows(IllegalArgumentException.class, () -> ManagedValue.method((Enum<?>) null));
    }

    static final class Both {

        @Managed("test.stale")
        @TestValue(TestValue.Id.SET)
        public static String both() {
            return "typed wins";
        }
    }

    @Test
    void aTypedIdWinsOverAStaleManagedBesideIt() {
        List<String> taken = new ArrayList<>();
        ManagedValues.claim(ManagedValue.method(TestValue.Id.SET).in("Values").holds(String.class, "")
                .because("Mine."), taken::add);
        ManagedValues.claim(text("test.stale"), value -> taken.add("stale=" + value));

        ManagedValues.install(Both.class);

        assertEquals(List.of("typed wins"), taken);
    }

    @Test
    void namingNoValuesClassAtAllIsLegal() {
        ManagedValues.install();
        ManagedValues.install((Class<?>[]) null);
    }
}
