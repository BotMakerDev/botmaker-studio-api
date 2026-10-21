package com.botmaker.plugin.api.source;

import com.botmaker.plugin.api.value.Ref;

import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.function.Function;

/**
 * A set the user grows through a window of your plugin, kept as a class of the bot's marked with the id —
 * {@code @SdkValue(SdkValue.Id.PICTURES) public final class Pictures { … }} — whose members the rest of the bot names
 * at its use sites. The whole class is your window's: the canvas may not add to it, rename in it or delete from it.
 * Declared with {@link ManagedValue#openSet}, whose next step picks what a member is:
 *
 * <table>
 *   <caption>The four kinds of open set</caption>
 *   <tr><th>Step</th><th>You get</th><th>The bot's class</th><th>A member</th></tr>
 *   <tr><td>{@link Steps#of of}</td><td>{@link OfConstants}</td>
 *       <td>{@code class Pictures { static final ImageTemplate CLOSE = …; }}</td>
 *       <td>a constant, its initialiser its value</td></tr>
 *   <tr><td>{@link Steps#ofEnum ofEnum}</td><td>{@link OneEnum}</td>
 *       <td>{@code enum Outcomes implements Outcome { WON, LOST }}</td>
 *       <td>a bare name: {@code Outcomes.WON}</td></tr>
 *   <tr><td>{@link Steps#ofEnumGroups ofEnumGroups}</td><td>{@link EnumGroups}</td>
 *       <td>{@code class Activities { enum Collect implements Outcome { NEXT } enum Battle … }}</td>
 *       <td>a bare name in one group: {@code Activities.Collect.NEXT}</td></tr>
 *   <tr><td>{@link Steps#ofClasses ofClasses}</td><td>{@link ManagedClasses}</td>
 *       <td>{@code class Screens { static final class Market extends Screen { … } static final Market MARKET … }}</td>
 *       <td>a class with one instance, whose fields other values name: {@code Screens.MARKET.beerBuy}</td></tr>
 * </table>
 *
 * <p>{@link #type()} is the class of each member. The host adds, renames and removes members through
 * {@link PluginValues}, and creates the class empty when a project has none.
 *
 * @param <T> the class of each member
 */
public abstract sealed class ManagedOpenSet<T> extends ManagedValue<T>
        permits ManagedOpenSet.OfConstants, ManagedOpenSet.OneEnum, ManagedOpenSet.EnumGroups, ManagedClasses {

    private final Ref.Of1<String, ? extends T> referred;

    ManagedOpenSet(Key key, String holder, Class<T> type, Ref.Of1<String, ? extends T> referred, String reason) {
        super(key, holder, type, reason);
        this.referred = referred;
    }

    /**
     * Whether a reference to one of the set's constants reads as the constant's name
     * ({@link ReferableStep#referredToAs}) rather than as what its initialiser holds — a set of constants or of
     * classes that said so. Never for a set of enums, whose constants hold nothing but their name and read through
     * {@code byName}.
     */
    public boolean isReferredByName() {
        return referred != null;
    }

    /**
     * The value a reference to the constant {@code name} stands for — what {@code Flows.DAILY} inside another value,
     * or {@code Screens.MARKET}, reads as — made by the plugin from the name alone. Null when the set is not referred
     * to by name, the plugin refused, or it answered something other than {@link #type()}.
     */
    public T byReference(String name) {
        return referred == null ? null : made(referred::call, name);
    }

    /**
     * Constants of the element class, each a {@code public static final} field whose initialiser is its value:
     * {@code public static final ImageTemplate CLOSE = …;}.
     *
     * @param <T> the class of each constant
     */
    public static final class OfConstants<T> extends ManagedOpenSet<T> {

        private OfConstants(Key key, String holder, Class<T> type, Ref.Of1<String, ? extends T> referred,
                            String reason) {
            super(key, holder, type, referred, reason);
        }
    }

    /**
     * One enum of the bot's, the holder itself, implementing the element interface, each member a bare name:
     * {@code public enum Outcomes implements Outcome { WON, LOST }}.
     *
     * @param <T> the interface each constant implements
     */
    public static final class OneEnum<T> extends ManagedOpenSet<T> {

        private final Ref.Of1<String, ? extends T> byName;

        private OneEnum(Key key, String holder, Class<T> type, Ref.Of1<String, ? extends T> byName, String reason) {
            super(key, holder, type, null, reason);
            this.byName = byName;
        }

        /**
         * The value the constant {@code name} stands for, made by the plugin from the name alone — what a host that
         * cannot load the bot's enum reads {@code Outcomes.WON} as. Null when the plugin's function refused the
         * name, or it answered something other than {@link #type()}.
         */
        public T byName(String name) {
            return made(byName::call, name);
        }
    }

    /**
     * Enums nested in the holder class, one per group, each implementing the element interface:
     * {@code public final class Activities { public enum Collect implements Outcome { NEXT } }}. A member is a
     * constant of one group, and a whole group reads as a value of its own.
     *
     * @param <T> the interface each constant implements
     */
    public static final class EnumGroups<T> extends ManagedOpenSet<T> {

        private final Ref.Of2<String, String, ? extends T> byName;
        private final Class<?> groupType;
        private final Ref.Of1<String, ?> byGroup;

        private EnumGroups(Key key, String holder, Class<T> type, Ref.Of2<String, String, ? extends T> byName,
                           Class<?> groupType, Ref.Of1<String, ?> byGroup, String reason) {
            super(key, holder, type, null, reason);
            this.byName = byName;
            this.groupType = groupType;
            this.byGroup = byGroup;
        }

        /**
         * The value the constant {@code name} of the nested enum {@code group} stands for —
         * {@code Activities.Collect.WON} read by a host that cannot load the bot's enum is what this makes of
         * {@code ("Collect", "WON")}. Null when the plugin refused, or it answered something other than
         * {@link #type()}.
         */
        public T byName(String group, String name) {
            if (group == null) return null;
            return made(constant -> byName.call(group, constant), name);
        }

        /**
         * The plugin's class for a whole nested enum — what a class literal of one, {@code Activities.Collect.class},
         * reads as wherever a value is read.
         */
        Class<?> groupType() {
            return groupType;
        }

        /**
         * The value the nested enum {@code group} stands for — what {@code Activities.Collect.class} reads as — made
         * by the plugin from the enum's simple name, as {@code as}: the plugin's class for a nested enum, or a supertype of it. Empty
         * when the plugin refused, or the value is not an {@code as}.
         */
        public <G> Optional<G> byGroup(Class<G> as, String group) {
            if (group == null || as == null) return Optional.empty();
            try {
                Object made = byGroup.call(group);
                return groupType.isInstance(made) && as.isInstance(made)
                        ? Optional.of(as.cast(made)) : Optional.empty();
            } catch (RuntimeException refused) {
                return Optional.empty();
            }
        }
    }

    /** After {@link ManagedValue#openSet}: what each member is. */
    public static final class Steps {

        private final Key key;

        Steps(Key key) {
            this.key = key;
        }

        /**
         * Constants of the class {@code element} — what {@link PluginValues#add} accepts for this set, and what
         * a constant's initialiser reads as.
         */
        public <E> ReferableStep<E, OfConstants<E>> of(Class<E> element) {
            if (element == null) throw new IllegalArgumentException(key.id() + ": no element type given");
            if (element.isPrimitive()) {
                throw new IllegalArgumentException(key.id() + ": a constant is an object; use the boxed class of "
                        + element.getName());
            }
            return new ReferableStep<>(key, (holder, referred) -> reason ->
                    new OfConstants<>(key, holder, element, referred, reason));
        }

        /**
         * One enum of the bot's own, the holder itself, implementing the interface {@code element}, each constant a
         * bare name — {@code public enum Outcomes implements Outcome { WON, LOST }} — which the host creates, adds to,
         * renames in and removes from as it does a class's fields, and creates as an empty enum implementing
         * {@code element}.
         *
         * <p>A host cannot load the bot's enum, so it reads {@code Outcomes.WON} as what {@code byName} makes of
         * {@code "WON"}: a value of the plugin's own that equals itself by name, which the type's declaration
         * writes back and the host spells as that constant. The name is the whole member; nothing else about it
         * is written, so a plugin derives anything it shows (a label) from the name.
         *
         * @param byName a method reference from a constant's name to the value it stands for —
         *               {@code BotConstants::outcome}
         * @throws IllegalArgumentException when {@code element} is no interface, or {@code byName} is no method
         *                                  reference
         */
        public <E> HolderStep<OneEnum<E>> ofEnum(Class<E> element, Ref.Of1<String, ? extends E> byName) {
            requireInterface(element);
            Ref.resolve(byName);
            return new HolderStep<>(key, holder -> reason -> new OneEnum<>(key, holder, element, byName, reason));
        }

        /**
         * Enums of the bot's own <b>nested in the holder class</b>, each implementing the interface
         * {@code element} and each a set of bare names — one enum per group:
         *
         * <pre>{@code
         * @SdkValue(SdkValue.Id.ACTIVITIES)
         * public final class Activities {
         *     public enum Collect implements Outcome { NEXT, NOTHING_LEFT }
         *     public enum Battle implements Outcome { NEXT, WON, LOST }
         * }
         * }</pre>
         *
         * <p>For a set whose members belong to something: here each enum is one activity, and its constants are
         * what that activity can report, so a method returning {@code Activities.Collect} cannot return
         * {@code Battle}'s. The host adds, renames and removes whole enums ({@link PluginValues#addGroup}) as well
         * as the constants in one, and reads {@code Activities.Collect.WON} as what {@code byName} makes of
         * {@code ("Collect", "WON")}. The holder is created as an empty {@code public final class}.
         *
         * @param byName a method reference from an enum's simple name and a constant's name to the value the
         *               constant stands for — {@code BotConstants::outcome}
         * @throws IllegalArgumentException when {@code element} is no interface, or {@code byName} is no method
         *                                  reference
         */
        public <E> GroupStep<E> ofEnumGroups(Class<E> element, Ref.Of2<String, String, ? extends E> byName) {
            requireInterface(element);
            Ref.resolve(byName);
            return new GroupStep<>(key, element, byName);
        }

        /**
         * Classes of the bot's own <b>nested in the holder class</b>, each a {@code public static final} subclass of
         * {@code element} with one constant holding its one instance — its members are instance fields, and one
         * method it overrides says the rest of it:
         *
         * <pre>{@code
         * @SdkValue(SdkValue.Id.SCREEN_MAP)
         * public final class Screens {
         *     public static final class Market extends Screen {
         *         public final Button beerBuy = Button.of(Pictures.BEER_BUY);
         *         public final Button close = Button.of(Pictures.CLOSE);
         *         @Override public Setup setup() {
         *             return Setup.named("Market").from(Screens.HOME.market).back(Screens.MARKET.close);
         *         }
         *     }
         *     public static final Market MARKET = new Market();
         * }
         * }</pre>
         *
         * <p>For a set whose members have parts other values point at one by one: a method can take
         * {@code Screens.MARKET.beerBuy}, and javac refuses a field the class has not got. The constant
         * ({@code MARKET}) is the member; the host adds, renames and removes a whole class with its constant
         * ({@link PluginValues#addClass}, {@link PluginValues#renameGroup}, {@link PluginValues#removeGroup}), and a
         * field with the operations on one constant, as {@code Member.of("MARKET", "beerBuy")}. Each field's
         * initialiser and the setup's {@code return} are values the grammar reads and writes, as any constant's
         * initialiser is. The holder is created as an empty {@code public final class}.
         *
         * @param element the class every nested one extends — {@code Screen}; a class, neither final nor an
         *                interface, so a bot's class can extend it
         * @throws IllegalArgumentException when {@code element} is missing, an interface, final, primitive or an
         *                                  array
         */
        public <E> ManagedClasses.FieldsStep<E> ofClasses(Class<E> element) {
            if (element == null) throw new IllegalArgumentException(key.id() + ": no element type given");
            if (element.isInterface() || element.isPrimitive() || element.isArray()
                    || Modifier.isFinal(element.getModifiers())) {
                throw new IllegalArgumentException(key.id() + ": a bot's class extends its element type, so "
                        + element.getName() + " must be a class that is not final");
            }
            return new ManagedClasses.FieldsStep<>(key, element);
        }

        private void requireInterface(Class<?> element) {
            if (element == null) throw new IllegalArgumentException(key.id() + ": no element type given");
            if (!element.isInterface()) {
                throw new IllegalArgumentException(key.id() + ": a bot's enum implements its element type, so "
                        + element.getName() + " must be an interface");
            }
        }
    }

    /** After {@link Steps#ofEnumGroups}: what a whole nested enum reads as. */
    public static final class GroupStep<E> {

        private final Key key;
        private final Class<E> element;
        private final Ref.Of2<String, String, ? extends E> byName;

        private GroupStep(Key key, Class<E> element, Ref.Of2<String, String, ? extends E> byName) {
            this.key = key;
            this.element = element;
            this.byName = byName;
        }

        /**
         * A class literal of one nested enum — {@code Activities.Collect.class} — reads as what {@code byGroup}
         * makes of its simple name, a {@code group}: how a plugin's value names a whole enum, as the SDK's flow
         * names an activity, without the host loading the bot's class. The host writes such a value back as
         * the class literal it was read from.
         *
         * @param group   the plugin's class for one enum — {@code Activity}
         * @param byGroup a method reference from an enum's simple name to that value — {@code BotConstants::activity}
         * @throws IllegalArgumentException when {@code group} is missing or {@code byGroup} is no method reference
         */
        public <G> HolderStep<EnumGroups<E>> groupedAs(Class<G> group, Ref.Of1<String, ? extends G> byGroup) {
            if (group == null) throw new IllegalArgumentException(key.id() + ": no group type given");
            Ref.resolve(byGroup);
            return new HolderStep<>(key, holder -> reason ->
                    new EnumGroups<>(key, holder, element, byName, group, byGroup, reason));
        }
    }

    /**
     * After {@link Steps#of} or {@link ManagedClasses.SetupStep#setup}: how a reference to a member reads, if by
     * name, then the class that is the set.
     *
     * @param <E> the class of each member
     * @param <V> the value the declaration ends in
     */
    public static final class ReferableStep<E, V> {

        /** What the declaration builds, given its holder and how a reference reads (null: by its initialiser). */
        interface Build<E, V> {
            Function<String, V> with(String holder, Ref.Of1<String, ? extends E> referred);
        }

        private final Key key;
        private final Build<E, V> build;

        ReferableStep(Key key, Build<E, V> build) {
            this.key = key;
            this.build = build;
        }

        /**
         * A reference to one of the set's constants — {@code Flows.DAILY} in {@code Flow.subflow(Flows.DAILY)}, or a
         * parameter {@code = Flows.DAILY} — reads as what {@code byReference} makes of the constant's name, wherever
         * it is read, not as what its initialiser holds; the host writes a value equal to that back as the
         * reference. A constant's own initialiser is still read as its value ({@link PluginValues#open(String,
         * String)}). For a set whose constants are told apart by name, not by value: two flows that both start
         * empty, or one just duplicated from another, are still two flows, and a value that names one must keep
         * naming it. The initialiser is not read either, so one constant naming another never reads the other's.
         * For a set of classes, {@code Screens.MARKET} names the class whose one instance it holds.
         *
         * <p>The plugin's value must be of the element type and equal itself by name alone.
         *
         * @param byReference a method reference from a constant's name to the value that stands for it —
         *                    {@code BotConstants::flow}
         * @throws IllegalArgumentException when {@code byReference} is no method reference
         */
        public HolderStep<V> referredToAs(Ref.Of1<String, ? extends E> byReference) {
            if (byReference == null) throw new IllegalArgumentException(key.id() + ": no reference given");
            Ref.resolve(byReference);
            return new HolderStep<>(key, holder -> build.with(holder, byReference));
        }

        /**
         * The class {@code holder} — {@code "Pictures"} — created empty when a project has none.
         *
         * @throws IllegalArgumentException when {@code holder} is not a class's simple name
         */
        public Reason<V> in(String holder) {
            return new Reason<>(build.with(requireHolder(key, holder), null));
        }
    }

    /**
     * After the steps that say what a set's member is: the class that is the set.
     *
     * @param <V> the value the declaration ends in
     */
    public static final class HolderStep<V> {

        private final Key key;
        private final Function<String, Function<String, V>> build;

        private HolderStep(Key key, Function<String, Function<String, V>> build) {
            this.key = key;
            this.build = build;
        }

        /**
         * The class {@code holder} — {@code "Pictures"} — created empty when a project has none.
         *
         * @throws IllegalArgumentException when {@code holder} is not a class's simple name
         */
        public Reason<V> in(String holder) {
            return new Reason<>(build.apply(requireHolder(key, holder)));
        }
    }
}
