package com.botmaker.plugin.api.source;

import com.botmaker.plugin.api.managed.ManagedMarker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

/**
 * A value your plugin keeps in the bot's own Java: Studio writes it into a class of the bot's, finds it again by
 * your plugin's mark, and stops the canvas from editing it any other way than yours.
 *
 * <p><b>Pick the shape by who changes the value</b>; each entry point walks you through its steps, and each shape
 * is its own type:
 *
 * <table>
 *   <caption>The three shapes</caption>
 *   <tr><th>Who changes it</th><th>Declare with</th><th>You get</th><th>In the bot</th></tr>
 *   <tr><td>the person running the bot, in the Runner or the Parameters window</td><td>{@link #param}</td>
 *       <td>{@link ManagedParam}</td><td>{@code @Param public static boolean doSetup = true;}</td></tr>
 *   <tr><td>a window of your plugin, one value</td><td>{@link #method}</td><td>{@link ManagedMethod}</td>
 *       <td>{@code public static Flow flow() { return …; }}</td></tr>
 *   <tr><td>a window of your plugin, a set the user grows</td><td>{@link #openSet}</td>
 *       <td>{@link ManagedOpenSet}</td><td>a class of constants, an enum, a class of enums or of classes</td></tr>
 * </table>
 *
 * <p>No window of your own yet? Declare a parameter: Studio writes it when a bot adds your plugin, and its
 * Parameters window edits it with your type's editor.
 *
 * <p><b>The mark.</b> Every managed value is named by a constant of an enum nested in an annotation of your
 * plugin's, meta-annotated {@link ManagedMarker} — {@code @SdkValue(SdkValue.Id.FLOW)} — so javac checks the id.
 * The bot carries the annotation, so it reaches the bot through your plugin at {@code compile}. The declaration
 * refuses a mark without {@code RUNTIME} retention, whose {@code @Target} leaves out where the shape puts it, or
 * whose {@code value()} is not that enum.
 *
 * <p><b>Declare each one once, as a {@code public static final} field</b>, and use the field everywhere: the
 * plugin's {@code .values(YourValues.class)} collects it, its runtime half claims it
 * ({@link com.botmaker.plugin.api.managed.ManagedValues#claim}), and its window opens it
 * ({@link PluginValues}). {@link #type()} lets each of them read a {@code T} without a cast.
 *
 * <p><b>Where it goes.</b> {@code .in("Sdk")} names the bot's class that holds it, by its simple name, because the
 * class is the bot's and your plugin never compiles against it. A bot without one gets it under
 * {@code plugins/<the last segment of your plugin's id>/}.
 *
 * <p>Plain data, like every other contribution: a host reads the id as text and matches it itself, so no plugin
 * code runs while a file is drawn. Only the steps build one, so a new part is a new step and never a changed
 * constructor.
 *
 * @param <T> the value's class; for an open set, the class of each member
 */
public abstract sealed class ManagedValue<T> permits ManagedMethod, ManagedParam, ManagedOpenSet {

    /**
     * Where the marker annotation goes. A host that has not moved to the subtypes reads it here; a new one switches
     * on {@link ManagedMethod}, {@link ManagedParam} and {@link ManagedOpenSet} instead.
     */
    public enum Shape {
        /** On a method: one value, the expression the method returns. */
        METHOD("method", "a managed value"),
        /** On a type: a class of constants the plugin adds to, renames in and removes from. */
        OPEN_SET("open-set", "a managed set"),
        /** On a field that is also a {@code @Param}: its declaration the plugin's, its value the runner's. */
        PARAM("param", "a plugin's parameter");

        private final String id;
        private final String displayName;

        Shape(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        /** The stable key: {@code "method"}, {@code "open-set"}, {@code "param"}. */
        public String id() {
            return id;
        }

        /** For a sentence: {@code "a managed set"}. */
        public String displayName() {
            return displayName;
        }
    }

    /** The id and the binary name of the plugin's marker annotation. */
    record Key(String id, String marker) {

        static Key of(Enum<?> constant, ElementType on) {
            if (constant == null) throw new IllegalArgumentException("A managed value needs an id");
            return new Key(idOf(constant), markerOf(constant, on));
        }
    }

    private final Key key;
    private final String holder;
    private final Class<T> type;
    private final String reason;

    ManagedValue(Key key, String holder, Class<T> type, String reason) {
        this.key = key;
        this.holder = holder;
        this.type = type;
        this.reason = reason;
    }

    Key key() {
        return key;
    }

    /**
     * <b>A parameter of your plugin's</b>: a {@code @Param} field in the bot that the person running it sets, and
     * your plugin's runtime reads by its mark — {@code Bot.runFlow(Sdk.class)} reads {@code Sdk.flow}:
     *
     * <pre>{@code
     * public static final ManagedParam<Boolean> DO_SETUP = ManagedValue.param(SdkValue.Id.DO_SETUP)
     *         .in("Sdk")
     *         .startsAs(SdkParams::doSetup)           // its type, and the value a written field starts as
     *         .shown("Run", "Set the game up first", Param.Visibility.PUBLIC)
     *         .because("The SDK reads this parameter. Change its value in the Runner or Project ▸ Parameters.");
     * }</pre>
     *
     * <p>The field is named after the id, camel-cased — {@code DO_SETUP} is {@code doSetup} — and marked with it,
     * {@code @SdkValue(SdkValue.Id.DO_SETUP) @Param(…) public static boolean doSetup = true;}. The host appends it
     * to the holder whenever the holder lacks a field carrying the id, and never lets the Parameters window
     * rename, retype, re-describe or remove it: the bot's own code names the field.
     *
     * @throws IllegalArgumentException when the enum is not nested in such an annotation, or its {@code @Target}
     *                                  leaves out fields
     */
    public static ManagedParam.Steps param(Enum<?> id) {
        return new ManagedParam.Steps(Key.of(id, ElementType.FIELD));
    }

    /**
     * <b>One value a window of your plugin edits</b>: a method in the bot marked with the id, whose one
     * {@code return} the host rewrites — {@code @SdkValue(SdkValue.Id.FLOW)}:
     *
     * <pre>{@code
     * public static final ManagedMethod<Flow> FLOW = ManagedValue.method(SdkValue.Id.FLOW)
     *         .in("Sdk")                              // the class that holds it | .openedOnly()
     *         .holds(Flow.class, Flow.NONE)           // what it returns, and what a created one starts as
     *         .because("This is the bot's activity flow. Draw it in 🔀 Activity Flow.");
     * }</pre>
     *
     * @throws IllegalArgumentException when the enum is not nested in such an annotation
     */
    public static ManagedMethod.Steps method(Enum<?> id) {
        return new ManagedMethod.Steps(Key.of(id, ElementType.METHOD));
    }

    /**
     * <b>A set the user grows through a window of your plugin</b>: a class of the bot's marked with the id —
     * {@code @SdkValue(SdkValue.Id.PICTURES)} — whose members the rest of the bot names, created empty. The next
     * step picks what a member is ({@link ManagedOpenSet} lists the four):
     *
     * <pre>{@code
     * public static final ManagedOpenSet.OfConstants<ImageTemplate> PICTURES = ManagedValue.openSet(SdkValue.Id.PICTURES)
     *         .of(ImageTemplate.class)                // the class of each constant
     *         .in("Pictures")
     *         .because("Picture constants are managed in 🖼 Manage Pictures.");
     *
     * public static final ManagedOpenSet.EnumGroups<Outcome> ACTIVITIES = ManagedValue.openSet(SdkValue.Id.ACTIVITIES)
     *         .ofEnumGroups(Outcome.class, BotConstants::outcome)  // enums nested in the holder: enum Collect { NEXT }
     *         .groupedAs(Activity.class, BotConstants::activity)
     *         .in("Activities")
     *         .because("Activities are managed in 🔀 Activity Flow.");
     *
     * public static final ManagedClasses<Screen> SCREEN_MAP = ManagedValue.openSet(SdkValue.Id.SCREEN_MAP)
     *         .ofClasses(Screen.class)                      // classes nested in the holder: class Market extends Screen
     *         .fields(Input.class, BotConstants::input)     // their fields: Screens.MARKET.beerBuy
     *         .setup(Screen::setup)                         // the method each overrides with one return
     *         .referredToAs(BotConstants::screen)
     *         .in("Screens")
     *         .because("The screen map is managed in 🗺 Screens.");
     * }</pre>
     *
     * @throws IllegalArgumentException when the enum is not nested in a {@link ManagedMarker} annotation
     */
    public static ManagedOpenSet.Steps openSet(Enum<?> id) {
        return new ManagedOpenSet.Steps(Key.of(id, ElementType.TYPE));
    }

    /** The id: {@link #idOf} its constant — {@code "com.botmaker.sdk.api.bot.SdkValue$Id.FLOW"}. */
    public String id() {
        return key.id();
    }

    /**
     * The binary name of the plugin's {@link ManagedMarker} annotation a bot marks this value with —
     * {@code "com.botmaker.sdk.api.bot.SdkValue"}.
     */
    public String marker() {
        return key.marker();
    }

    /**
     * The id of a typed value: the binary name of {@code constant}'s enum, a dot, and the constant's name —
     * {@code "com.botmaker.sdk.api.bot.SdkValue$Id.FLOW"}. The enum's name is the <b>binary</b> one, with
     * {@code $} before a nested class: a host reading a bot's annotation off JDT takes the constant's
     * declaring type's {@code getBinaryName()}, never its {@code getQualifiedName()}, and the runtime reads the
     * same through reflection.
     */
    public static String idOf(Enum<?> constant) {
        return constant.getDeclaringClass().getName() + "." + constant.name();
    }

    /**
     * The binary name of the annotation {@code constant}'s enum is nested in, refused unless that annotation
     * has the shape {@link ManagedMarker} asks for — marked, runtime-retained, placeable {@code on} a method, a
     * field or a type as the value's shape needs, its {@code value()} that enum — checked once, when the plugin
     * declares the value.
     */
    private static String markerOf(Enum<?> constant, ElementType on) {
        Class<?> ids = constant.getDeclaringClass();
        Class<?> marker = ids.getEnclosingClass();
        String where = ids.getName() + "." + constant.name();
        if (marker == null || !marker.isAnnotation() || !marker.isAnnotationPresent(ManagedMarker.class)) {
            throw new IllegalArgumentException(where + ": the enum must be nested in an annotation marked "
                    + "@ManagedMarker");
        }
        Retention retention = marker.getAnnotation(Retention.class);
        if (retention == null || retention.value() != RetentionPolicy.RUNTIME) {
            throw new IllegalArgumentException(where + ": " + marker.getName() + " needs "
                    + "@Retention(RetentionPolicy.RUNTIME), or a bot's runtime never sees it");
        }
        Target target = marker.getAnnotation(Target.class);
        if (target != null && !Arrays.asList(target.value()).contains(on)) {
            throw new IllegalArgumentException(where + ": " + marker.getName() + "'s @Target must include "
                    + "ElementType." + on + ", where a bot puts it");
        }
        try {
            if (marker.getDeclaredMethod("value").getReturnType() != ids) {
                throw new IllegalArgumentException(where + ": " + marker.getName() + ".value() must be "
                        + ids.getSimpleName());
            }
        } catch (NoSuchMethodException none) {
            throw new IllegalArgumentException(where + ": " + marker.getName() + " has no value()");
        }
        return marker.getName();
    }

    /** The sentence the host shows when it refuses an edit: what owns this and where to go instead. */
    public String reason() {
        return reason;
    }

    /**
     * The simple name of the bot's class that holds it — {@code "Sdk"}, {@code "Pictures"} — or null for a method
     * value the host may open and never create ({@link ManagedMethod.Steps#openedOnly}).
     */
    public String holder() {
        return holder;
    }

    /**
     * The class of the value — for an open set, of each member; for a parameter, the field's class, boxed when it
     * is a primitive.
     */
    public Class<T> type() {
        return type;
    }

    /**
     * {@code value} as this value's type — for an open set, one member's — or null when it is not one: how a
     * sink or a window reads what the host or the bot handed over without writing a cast of its own.
     */
    public T cast(Object value) {
        return type.isInstance(value) ? type.cast(value) : null;
    }

    @Override
    public String toString() {
        return "ManagedValue[" + key.id() + (holder == null ? "" : " in " + holder) + "]";
    }

    /**
     * The last step of a method value or an open set: the sentence shown when the canvas refuses an edit.
     *
     * @param <V> the value it ends in
     */
    public static final class Reason<V> {

        private final Function<String, V> build;

        Reason(Function<String, V> build) {
            this.build = build;
        }

        /** What owns this value and where to change it instead. */
        public V because(String reason) {
            return build.apply(requireText(reason, "reason"));
        }
    }

    static String requireText(String text, String what) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("A managed value needs a " + what);
        return text;
    }

    /** Calls a plugin's function, a refusal or an answer that is no {@code T} reading as null. */
    T made(Function<String, ?> function, String name) {
        if (function == null || name == null) return null;
        try {
            return cast(function.apply(name));
        } catch (RuntimeException refused) {
            return null;
        }
    }

    // The accessors below read one shape's parts on any value and answer nothing for the other shapes; the
    // subtype that has the part overrides its accessor. They stay until every host switches on the subtype.

    /** Whether the annotation goes on a method, a field or the holder; switch on the subtype instead. */
    public Shape shape() {
        return switch (this) {
            case ManagedMethod<T> _ -> Shape.METHOD;
            case ManagedParam<T> _ -> Shape.PARAM;
            case ManagedOpenSet<T> _ -> Shape.OPEN_SET;
        };
    }

    /** Whether this is a {@link ManagedParam}. */
    public boolean isParam() {
        return this instanceof ManagedParam;
    }

    /** Whether this is a {@link ManagedOpenSet}. */
    public boolean isOpenSet() {
        return this instanceof ManagedOpenSet;
    }

    /** Whether this is a {@link ManagedOpenSet.OneEnum}. */
    public boolean isEnum() {
        return this instanceof ManagedOpenSet.OneEnum;
    }

    /** Whether this is a {@link ManagedOpenSet.EnumGroups}. */
    public boolean isGrouped() {
        return this instanceof ManagedOpenSet.EnumGroups;
    }

    /** Whether this is a {@link ManagedClasses}. */
    public boolean isClasses() {
        return this instanceof ManagedClasses;
    }

    /** {@link ManagedParam#form()}; {@link #type()} for any other shape. */
    public Type form() {
        return type;
    }

    /** {@link ManagedMethod#initial()} or {@link ManagedParam#initial()}; null for an open set. */
    public T initial() {
        return null;
    }

    /** {@link ManagedParam#category()}; {@code ""} for any other shape. */
    public String category() {
        return "";
    }

    /** {@link ManagedParam#description()}; {@code ""} for any other shape. */
    public String description() {
        return "";
    }

    /** {@link ManagedParam#visibility()}; {@code ""} for any other shape. */
    public String visibility() {
        return "";
    }

    /** {@link ManagedParam#conditional()}; false for any other shape. */
    public boolean conditional() {
        return false;
    }

    /** {@link ManagedParam#appliesIn}; true for any other shape. */
    public boolean appliesIn(ParamScope scope) {
        return true;
    }

    /** {@link ManagedOpenSet.OneEnum#byName}; null for any other shape. */
    public T byName(String name) {
        return null;
    }

    /** {@link ManagedOpenSet.EnumGroups#byName}; null for any other shape. */
    public T byName(String group, String name) {
        return null;
    }

    /** {@link ManagedOpenSet.EnumGroups#groupType()}; null for any other shape. */
    public Class<?> groupType() {
        return null;
    }

    /** {@link ManagedOpenSet.EnumGroups#byGroup}; empty for any other shape. */
    public <G> Optional<G> byGroup(Class<G> as, String group) {
        return Optional.empty();
    }

    /** {@link ManagedClasses#fieldType()}; null for any other shape. */
    public Class<?> fieldType() {
        return null;
    }

    /** {@link ManagedClasses#byField}; null for any other shape. */
    public Object byField(String member, String field) {
        return null;
    }

    /** {@link ManagedClasses#setup()}; null for any other shape. */
    public Method setup() {
        return null;
    }

    /**
     * {@link ManagedOpenSet.OfConstants#isReferredByName()} or {@link ManagedClasses#isReferredByName()}; false
     * for any other shape.
     */
    public boolean isReferredByName() {
        return false;
    }

    /**
     * {@link ManagedOpenSet.OfConstants#byReference} or {@link ManagedClasses#byReference}; null for any other
     * shape.
     */
    public T byReference(String name) {
        return null;
    }
}
