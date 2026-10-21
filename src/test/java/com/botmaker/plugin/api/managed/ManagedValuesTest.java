package com.botmaker.plugin.api.managed;

import com.botmaker.plugin.api.params.Param;
import com.botmaker.plugin.api.source.ManagedClasses;
import com.botmaker.plugin.api.source.ManagedMethod;
import com.botmaker.plugin.api.source.ManagedOpenSet;
import com.botmaker.plugin.api.source.ManagedParam;
import com.botmaker.plugin.api.source.ManagedValue;
import com.botmaker.plugin.api.source.ParamScope;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a bot's {@code main} gets for naming a plugin's values class, and what it is spared. Moved from
 * {@code botmaker-plugin-basics} with the class (2026-09-28), typed by the declaration since, and marked with a
 * plugin's own annotation since 2026-10-09.
 *
 * <p>Every test claims a <b>constant of its own</b> rather than resetting the registry between them: the claims
 * are static because a plugin registers them once.
 */
class ManagedValuesTest {

    /** A plugin's own marker, as {@link ManagedMarker} asks for it: its {@code value()} an enum nested in it. */
    @ManagedMarker
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface TestValue {

        Id value();

        enum Id {
            FLOW, CAPTURE, WRONG_TYPE, NOBODY_READS_THIS, NOT_PUBLIC, NOT_STATIC, TAKES_ARGUMENTS, PICTURES, OPENED,
            INTS, SET, ENUM_SET, THROWS, SURVIVES, GREETING, FAREWELL
        }
    }

    private static ManagedMethod<String> text(TestValue.Id id) {
        return ManagedValue.method(id).in("Values").holds(String.class, "").because("Mine.");
    }

    static final class Values {

        @TestValue(TestValue.Id.FLOW)
        public static String flow() {
            return "the flow";
        }

        @TestValue(TestValue.Id.CAPTURE)
        public static String capture() {
            return "the capture source";
        }
    }

    @Test
    void everyManagedMethodReachesWhoeverClaimedItsId() {
        List<String> taken = new ArrayList<>();
        ManagedValues.claim(text(TestValue.Id.FLOW), value -> taken.add("flow=" + value));
        ManagedValues.claim(text(TestValue.Id.CAPTURE), value -> taken.add("capture=" + value));

        ManagedValues.install(Values.class);

        // Name order, which is what install sorts by: capture before flow.
        assertEquals(List.of("capture=the capture source", "flow=the flow"), taken);
    }

    static final class WrongType {

        @TestValue(TestValue.Id.WRONG_TYPE)
        public static Integer count() {
            return 3;
        }
    }

    @Test
    void aValueOfAnotherTypeNeverReachesTheSink() {
        List<Object> taken = new ArrayList<>();
        ManagedValues.claim(text(TestValue.Id.WRONG_TYPE), taken::add);

        ManagedValues.install(WrongType.class);

        assertTrue(taken.isEmpty(), () -> "a String sink was handed " + taken);
    }

    static final class Unclaimed {

        @TestValue(TestValue.Id.NOBODY_READS_THIS)
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

        @TestValue(TestValue.Id.NOT_PUBLIC)
        static String hidden() {
            return "no";
        }

        @TestValue(TestValue.Id.NOT_STATIC)
        public String perInstance() {
            return "no";
        }

        @TestValue(TestValue.Id.TAKES_ARGUMENTS)
        public static String computed(String argument) {
            return argument;
        }

        public static String helper() {
            return "no";
        }
    }

    @Test
    void onlyAPublicStaticNoArgumentMethodIsAValue() {
        List<TestValue.Id> taken = new ArrayList<>();
        for (TestValue.Id id : List.of(TestValue.Id.NOT_PUBLIC, TestValue.Id.NOT_STATIC,
                TestValue.Id.TAKES_ARGUMENTS)) {
            ManagedValues.claim(text(id), value -> taken.add(id));
        }

        ManagedValues.install(NotValues.class);

        assertTrue(taken.isEmpty(), () -> "installed something that is not a value: " + taken);
    }

    @TestValue(TestValue.Id.PICTURES)
    static final class Pictures {

        public static final String COLLECT = "collect.png";
    }

    /** An open set cannot be claimed at all: {@code claim} takes a method value only. */
    @Test
    void aMarkedTypeInstallsNothing() {
        List<Object> taken = new ArrayList<>();
        ManagedValues.claim(text(TestValue.Id.PICTURES), taken::add);

        ManagedValues.install(Pictures.class);

        assertTrue(taken.isEmpty(), () -> "installed a marked type: " + taken);
    }

    static final class Opened {

        @TestValue(TestValue.Id.OPENED)
        public static String opened() {
            return "opened, never created";
        }
    }

    /** A value the host may only open is still a method's value: it was taken for an open set until 2026-10-05. */
    @Test
    void aValueTheHostNeverCreatesIsStillClaimed() {
        ManagedMethod<String> opened = ManagedValue.method(TestValue.Id.OPENED).openedOnly().holds(String.class)
                .because("Mine.");
        List<String> taken = new ArrayList<>();
        ManagedValues.claim(opened, taken::add);

        ManagedValues.install(Opened.class);

        assertNull(opened.holder());
        assertEquals(List.of("opened, never created"), taken);
    }

    @Test
    void aSetOfPrimitivesIsRefusedAsItIsDeclared() {
        assertThrows(IllegalArgumentException.class, () -> ManagedValue.openSet(TestValue.Id.INTS).of(int.class));
    }

    @Test
    void anOpenSetKnowsWhatEachConstantIs() {
        ManagedOpenSet.OfConstants<String> set = ManagedValue.openSet(TestValue.Id.SET).of(String.class).in("Set")
                .because("Mine.");

        assertFalse(set.isReferredByName());
        assertEquals(String.class, set.type());
        assertEquals("x", set.cast("x"));
        assertNull(set.cast(3));
    }

    /** What a bot's enum constant stands for, made from its name: the element of an enum set. */
    public interface Label {
        String name();
    }

    public record Named(String name) implements Label {}

    public static Label named(String name) {
        if (name.isBlank()) throw new IllegalArgumentException("no name");
        return new Named(name);
    }

    @Test
    void anEnumSetReadsEachConstantByItsName() {
        ManagedOpenSet.OneEnum<Label> set = ManagedValue.openSet(TestValue.Id.ENUM_SET)
                .ofEnum(Label.class, ManagedValuesTest::named).in("Labels").because("Mine.");

        assertEquals(new Named("WON"), set.byName("WON"));
        assertNull(set.byName(" "), "a name the plugin refuses reads as nothing");
        assertNull(set.byName(null));
    }

    /** What a reference to the constant {@code name} stands for; refuses a blank name. */
    public static String referenced(String name) {
        if (name.isBlank()) throw new IllegalArgumentException("no name");
        return "→" + name;
    }

    @Test
    void aSetReferredToByNameReadsAReferenceAsTheName() {
        ManagedOpenSet.OfConstants<String> set = ManagedValue.openSet(TestValue.Id.SET).of(String.class)
                .referredToAs(ManagedValuesTest::referenced).in("Set").because("Mine.");

        assertTrue(set.isReferredByName());
        assertEquals("→DAILY", set.byReference("DAILY"));
        assertNull(set.byReference(" "), "a name the plugin refuses reads as nothing");
        assertFalse(ManagedValue.openSet(TestValue.Id.SET).of(String.class).in("Set").because("Mine.")
                .isReferredByName());
        assertThrows(IllegalArgumentException.class, () -> ManagedValue.openSet(TestValue.Id.SET).of(String.class)
                .referredToAs(name -> "→" + name), "a lambda is refused");
    }

    /** Each entry point ends in its own shape's type, and a host tells them apart by type alone. */
    @Test
    void eachShapeIsItsOwnType() {
        ManagedMethod<String> method = text(TestValue.Id.GREETING);
        ManagedParam<Boolean> param = ManagedValue.param(Fields.Id.DO_SETUP).in("Sdk")
                .startsAs(ManagedValuesTest::doSetup).shown("", "", Param.Visibility.EDITOR).because("Mine.");
        ManagedOpenSet.OfConstants<String> constants = ManagedValue.openSet(TestValue.Id.SET).of(String.class)
                .in("Set").because("Mine.");
        ManagedOpenSet.OneEnum<Label> anEnum = ManagedValue.openSet(TestValue.Id.ENUM_SET)
                .ofEnum(Label.class, ManagedValuesTest::named).in("Labels").because("Mine.");
        ManagedOpenSet.EnumGroups<Label> enums = ManagedValue.openSet(TestValue.Id.ENUM_SET)
                .ofEnumGroups(Label.class, ManagedValuesTest::inGroup).groupedAs(Group.class, ManagedValuesTest::group)
                .in("Activities").because("Mine.");
        ManagedClasses<Sheet> classes = ManagedValue.openSet(TestValue.Id.ENUM_SET).ofClasses(Sheet.class)
                .fields(Cell.class, ManagedValuesTest::cell).setup(Sheet::setup).in("Sheets").because("Mine.");

        assertEquals(List.of("method", "param", "constants", "enum", "enums", "classes"),
                java.util.stream.Stream.of(method, param, constants, anEnum, enums, classes)
                        .map(ManagedValuesTest::shapeOf).toList());
        assertEquals("", method.initial());
        assertEquals(boolean.class, param.form());
        assertFalse(classes.isReferredByName());
        assertEquals(Cell.class, classes.fieldType());
    }

    private static String shapeOf(ManagedValue<?> value) {
        return switch (value) {
            case ManagedMethod<?> _ -> "method";
            case ManagedParam<?> _ -> "param";
            case ManagedOpenSet.OfConstants<?> _ -> "constants";
            case ManagedOpenSet.OneEnum<?> _ -> "enum";
            case ManagedOpenSet.EnumGroups<?> _ -> "enums";
            case ManagedClasses<?> _ -> "classes";
        };
    }

    @Test
    void anEnumSetNeedsAnInterfaceAndAMethodReference() {
        assertThrows(IllegalArgumentException.class, () -> ManagedValue.openSet(TestValue.Id.ENUM_SET)
                .ofEnum(Named.class, Named::new));
        assertThrows(IllegalArgumentException.class, () -> ManagedValue.openSet(TestValue.Id.ENUM_SET)
                .ofEnum(Label.class, name -> new Named(name)));
    }

    /** A whole nested enum, made from its simple name: the group of a grouped set. */
    public record Group(String name) {}

    public static Label inGroup(String group, String name) {
        if (name.isBlank()) throw new IllegalArgumentException("no name");
        return new Named(group + "." + name);
    }

    public static Group group(String name) {
        return new Group(name);
    }

    @Test
    void aGroupedSetReadsAConstantByItsEnumAndNameAndAnEnumByItsName() {
        ManagedOpenSet.EnumGroups<Label> set = ManagedValue.openSet(TestValue.Id.ENUM_SET)
                .ofEnumGroups(Label.class, ManagedValuesTest::inGroup)
                .groupedAs(Group.class, ManagedValuesTest::group)
                .in("Activities").because("Mine.");

        assertEquals(new Named("Collect.WON"), set.byName("Collect", "WON"));
        assertNull(set.byName("Collect", " "), "a name the plugin refuses reads as nothing");
        assertNull(set.byName(null, "WON"), "a constant outside any group reads as nothing");
        assertEquals(new Group("Collect"), set.byGroup(Group.class, "Collect").orElseThrow());
        assertEquals(new Group("Collect"), set.byGroup(Object.class, "Collect").orElseThrow(), "a supertype reads");
        assertTrue(set.byGroup(String.class, "Collect").isEmpty(), "another type reads as nothing");
        assertTrue(set.byGroup(Group.class, null).isEmpty());
    }

    @Test
    void aGroupedSetNeedsAnInterfaceAndMethodReferences() {
        assertThrows(IllegalArgumentException.class, () -> ManagedValue.openSet(TestValue.Id.ENUM_SET)
                .ofEnumGroups(Named.class, (g, n) -> new Named(n)));
        assertThrows(IllegalArgumentException.class, () -> ManagedValue.openSet(TestValue.Id.ENUM_SET)
                .ofEnumGroups(Label.class, ManagedValuesTest::inGroup).groupedAs(Group.class, name -> new Group(name)));
    }

    /** The element of a set of classes: what a bot's nested class extends. */
    public abstract static class Sheet {
        public abstract Label setup();

        public final Label helper() {
            return new Named("helper");
        }
    }

    /** What a reference to a whole class stands for, made from its constant's name. */
    public static final class NamedSheet extends Sheet {
        private final String name;

        NamedSheet(String name) {
            this.name = name;
        }

        @Override
        public Label setup() {
            return new Named(name);
        }
    }

    /** One field of a nested class, made from its class's constant and its own name. */
    public record Cell(String sheet, String name) {}

    public static Cell cell(String sheet, String name) {
        if (name.isBlank()) throw new IllegalArgumentException("no name");
        return new Cell(sheet, name);
    }

    public static Sheet sheet(String name) {
        return new NamedSheet(name);
    }

    @Test
    void aSetOfClassesReadsAFieldByItsConstantAndNameAndNamesItsSetup() throws Exception {
        ManagedClasses<Sheet> set = ManagedValue.openSet(TestValue.Id.ENUM_SET)
                .ofClasses(Sheet.class)
                .fields(Cell.class, ManagedValuesTest::cell)
                .setup(Sheet::setup)
                .referredToAs(ManagedValuesTest::sheet)
                .in("Sheets").because("Mine.");

        assertEquals(Cell.class, set.fieldType());
        assertEquals(new Cell("MARKET", "beerBuy"), set.byField("MARKET", "beerBuy"));
        assertNull(set.byField("MARKET", " "), "a name the plugin refuses reads as nothing");
        assertNull(set.byField(null, "beerBuy"));
        assertEquals(Sheet.class.getMethod("setup"), set.setup());
        assertTrue(set.isReferredByName(), "a class's constant is referred to by name");
        assertEquals("MARKET", ((Named) set.byReference("MARKET").setup()).name());
    }

    @Test
    void aSetOfClassesNeedsAnOpenClassAndAnOverridableSetup() {
        assertThrows(IllegalArgumentException.class, () -> ManagedValue.openSet(TestValue.Id.ENUM_SET)
                .ofClasses(Label.class), "an interface is no class to extend");
        assertThrows(IllegalArgumentException.class, () -> ManagedValue.openSet(TestValue.Id.ENUM_SET)
                .ofClasses(NamedSheet.class), "a final class cannot be extended");
        assertThrows(IllegalArgumentException.class, () -> ManagedValue.openSet(TestValue.Id.ENUM_SET)
                .ofClasses(Sheet.class).fields(Cell.class, (s, n) -> new Cell(s, n)), "a lambda is refused");
        assertThrows(IllegalArgumentException.class, () -> ManagedValue.openSet(TestValue.Id.ENUM_SET)
                .ofClasses(Sheet.class).fields(Cell.class, ManagedValuesTest::cell).setup(Sheet::helper),
                "a final method cannot be overridden");
    }

    static final class OneThrows {

        @TestValue(TestValue.Id.THROWS)
        public static String broken() {
            throw new IllegalStateException("the bot author's own code");
        }

        @TestValue(TestValue.Id.SURVIVES)
        public static String fine() {
            return "installed anyway";
        }
    }

    @Test
    void aValueThatThrowsCostsOnlyItself() {
        List<String> taken = new ArrayList<>();
        ManagedValues.claim(text(TestValue.Id.THROWS), value -> taken.add("throws=" + value));
        ManagedValues.claim(text(TestValue.Id.SURVIVES), value -> taken.add("survives=" + value));

        ManagedValues.install(OneThrows.class);

        assertEquals(List.of("survives=installed anyway"), taken);
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
    void anIdIsTheEnumsBinaryNameAndTheConstant() {
        ManagedValue<String> greeting = text(TestValue.Id.GREETING);
        ManagedValue<String> set = ManagedValue.openSet(TestValue.Id.SET).of(String.class).in("Set")
                .because("Mine.");

        String ids = TestValue.Id.class.getName();
        assertEquals(ids + ".GREETING", greeting.id());
        assertTrue(ids.endsWith("$TestValue$Id"), ids);
        assertEquals(TestValue.class.getName(), greeting.marker());
        assertEquals(ids + ".SET", set.id());
        assertEquals(TestValue.class.getName(), set.marker());
    }

    @Test
    void aMethodMarkedWithAPluginsAnnotationReachesWhoeverClaimedItsConstant() {
        List<String> taken = new ArrayList<>();
        ManagedValues.claim(text(TestValue.Id.GREETING), value -> taken.add("greeting=" + value));

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
    void anIdMustBeAConstantOfTheEnumInAMarkedAnnotation() {
        assertThrows(IllegalArgumentException.class, () -> ManagedValue.method(Loose.FLOW));
        assertThrows(IllegalArgumentException.class, () -> ManagedValue.method(Unmarked.Id.FLOW));
        assertThrows(IllegalArgumentException.class, () -> ManagedValue.openSet(WrongElement.Id.FLOW));
        assertThrows(IllegalArgumentException.class, () -> ManagedValue.method(NotAtRuntime.Id.FLOW));
        assertThrows(IllegalArgumentException.class, () -> ManagedValue.openSet(MethodsOnly.Id.PICTURES));
        ManagedValue.method(MethodsOnly.Id.PICTURES);
        assertThrows(IllegalArgumentException.class, () -> ManagedValue.method(null));
    }

    @ManagedMarker
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    @interface Fields {

        Id value();

        enum Id { SKIP, DO_SETUP }
    }

    public static java.util.Set<Class<? extends Number>> skip() {
        return java.util.Set.of(Integer.class);
    }

    public static boolean doSetup() {
        return true;
    }

    @Test
    void aParameterIsTypedByItsMethodAndStartsAsWhatItReturns() {
        ManagedParam<java.util.Set<Class<? extends Number>>> skip = ManagedValue.param(Fields.Id.SKIP).in("Sdk")
                .startsAs(ManagedValuesTest::skip).shown("Run", "What to skip", (Param.Visibility) null)
                .because("Mine.");
        assertEquals(java.util.Set.class, skip.type());
        assertEquals("java.util.Set<java.lang.Class<? extends java.lang.Number>>", skip.form().getTypeName());
        assertEquals(java.util.Set.of(Integer.class), skip.initial());
        assertEquals("Run", skip.category());
        assertEquals("What to skip", skip.description());
        assertEquals(Param.Visibility.EDITOR, skip.visibility());

        ManagedParam<Boolean> doSetup = ManagedValue.param(Fields.Id.DO_SETUP).in("Sdk")
                .startsAs(ManagedValuesTest::doSetup).shown(null, null, Param.Visibility.PUBLIC)
                .because("Mine.");
        assertEquals(Boolean.class, doSetup.type());
        assertEquals(boolean.class, doSetup.form());
        assertEquals(Param.Visibility.PUBLIC, doSetup.visibility());
        assertEquals("", doSetup.category());
    }

    /** The host places the holder and writes its name, so a qualified or unwritable one is refused at once. */
    @Test
    void aHolderIsASimpleClassName() {
        assertThrows(IllegalArgumentException.class, () -> ManagedValue.param(Fields.Id.DO_SETUP).in("com.bot.Sdk"));
        assertThrows(IllegalArgumentException.class, () -> ManagedValue.param(Fields.Id.DO_SETUP).in("My Sdk"));
        assertThrows(IllegalArgumentException.class, () -> ManagedValue.param(Fields.Id.DO_SETUP).in("1Sdk"));
        assertThrows(IllegalArgumentException.class, () -> ManagedValue.param(Fields.Id.DO_SETUP).in(" "));
        ManagedValue.param(Fields.Id.DO_SETUP).in("Sdk_2");
    }

    /** A parameter declared only while the bot's other value says so, read through the host's scope. */
    @Test
    void aConditionalParameterAppliesOnlyWhileItsConditionHolds() {
        ManagedParam<Boolean> doSetup = ManagedValue.param(Fields.Id.DO_SETUP).in("Sdk")
                .startsAs(ManagedValuesTest::doSetup).shown("", "", Param.Visibility.EDITOR).because("Mine.");
        ManagedParam<java.util.Set<Class<? extends Number>>> skip = ManagedValue.param(Fields.Id.SKIP).in("Sdk")
                .startsAs(ManagedValuesTest::skip).shown("Run", "What to skip", Param.Visibility.EDITOR)
                .when(scope -> scope.value(doSetup).orElse(false))
                .because("Mine.");
        ParamScope settingUp = new ParamScope() {
            @Override
            @SuppressWarnings("unchecked")
            public <V> java.util.Optional<V> value(ManagedValue<V> value) {
                return value == doSetup ? java.util.Optional.of((V) Boolean.TRUE) : java.util.Optional.empty();
            }
        };

        assertTrue(skip.appliesIn(settingUp));
        assertFalse(skip.appliesIn(ParamScope.NONE), "the other value says nothing");
        assertFalse(skip.appliesIn(null), "no scope reads as an empty one");
        assertTrue(doSetup.appliesIn(ParamScope.NONE), "an unconditional parameter always applies");

        ManagedParam<Boolean> throwing = ManagedValue.param(Fields.Id.DO_SETUP).in("Sdk")
                .startsAs(ManagedValuesTest::doSetup).shown("", "", Param.Visibility.EDITOR)
                .when(scope -> { throw new IllegalStateException("broken"); }).because("Mine.");
        assertTrue(throwing.appliesIn(ParamScope.NONE), "a condition that throws holds");
        // Only a parameter has a condition: a method value's steps offer no when.
    }

    @Test
    void aVisibilityIsTheAnnotationsSpellingAndAnythingElseIsTheEditorOnly() {
        assertEquals(Param.PUBLIC, Param.Visibility.PUBLIC.id());
        assertEquals(Param.EDITOR, Param.Visibility.EDITOR.id());
        assertEquals(Param.Visibility.PUBLIC, Param.Visibility.fromId("public"));
        assertEquals(Param.Visibility.EDITOR, Param.Visibility.fromId("nobody"));
        assertEquals(Param.Visibility.EDITOR, Param.Visibility.fromId(null));
    }

    /** A parameter is never claimed either: the bot reads it as a field, and {@code claim} takes a method value. */
    @Test
    void aParameterNeedsAMarkerFieldsMayCarry() {
        assertThrows(IllegalArgumentException.class, () -> ManagedValue.param(TestValue.Id.FLOW));
        assertThrows(IllegalArgumentException.class, () -> ManagedValue.param(Fields.Id.SKIP).in("Sdk")
                .startsAs(() -> java.util.Set.<Class<? extends Number>>of()));
    }

    @Test
    void namingNoValuesClassAtAllIsLegal() {
        ManagedValues.install();
        ManagedValues.install((Class<?>[]) null);
    }
}
