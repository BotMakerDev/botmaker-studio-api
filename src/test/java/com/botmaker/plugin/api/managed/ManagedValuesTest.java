package com.botmaker.plugin.api.managed;

import com.botmaker.plugin.api.source.ManagedValue;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        return ManagedValue.of(id, "Values", String.class, "", "Mine.");
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
        ManagedValues.claim(ManagedValue.openSet("test.pictures", "Pictures", "Mine."), taken::add);
        ManagedValues.claim(text("test.pictures"), taken::add);

        ManagedValues.install(Pictures.class);

        assertTrue(taken.isEmpty(), () -> "installed a type-level @Managed: " + taken);
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

    @Test
    void namingNoValuesClassAtAllIsLegal() {
        ManagedValues.install();
        ManagedValues.install((Class<?>[]) null);
    }
}
