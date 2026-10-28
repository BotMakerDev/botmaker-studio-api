package com.botmaker.plugin.api.meta;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Written on a static method, a <em>rule</em>: what one element of an API a bot used to call becomes. The host
 * never guesses a successor; a rule says it, written by the bot's author or by the plugin that owns the element.
 *
 * <p><b>In a plugin</b> (2026-10-28), a rule stands beside an element it marks
 * {@code @Deprecated(forRemoval = true)}, so it compiles against the element in the plugin's own build. The host
 * reads it from the plugin's {@code -sources.jar} (this annotation has source retention), in any class, and
 * rewrites a bot's uses with it; nothing is copied into the bot. The next release removes the element and its rule
 * together. The host reads the sources jar of the version the bot moves <em>from</em>, so an upgrade from the
 * release carrying the rule still finds it; a bot that moves from an earlier version straight past that release
 * gets no rule from the plugin, only the host's default repair.
 *
 * <p><b>In a bot</b>, a rule the author writes wins over a plugin's for the same element; two plugin rules for one
 * element are both refused.
 *
 * <pre>{@code
 * @Migrate(GameScreen.class)                                    // an instance method
 * static GameScreen marker(@Migrate.Self GameScreen self, ImageTemplate marker) {
 *     return self.markers(List.of(marker));
 * }
 *
 * @Migrate(Run.class)                                           // a method gone, nothing in its place
 * static Run popups(@Migrate.Self Run self, Runnable popups) {
 *     return self;
 * }
 *
 * @Migrate(ImageFinder.class)                                   // a static method, used as a statement
 * static void untilFind(ImageTemplate picture, Runnable step) {
 *     ImageFinder.repeatUntil(picture, step);
 * }
 *
 * @Migrate(value = Rect.class, kind = Migrate.Kind.CONSTRUCTOR)
 * static Rect rect(int x, int y) {
 *     return new Rect(x, y, 0, 0);
 * }
 *
 * @Migrate(value = State.class, kind = Migrate.Kind.FIELD)      // a constant
 * static State OFF() {
 *     return State.DISABLED;
 * }
 *
 * @Migrate(name = "com.example.Area", kind = Migrate.Kind.TYPE) // a type that is gone
 * static Rect area(int x, int y, int width, int height) {
 *     return new Rect(x, y, width, height);
 * }
 * }</pre>
 *
 * <p><b>The method's own signature names the old element</b>, so a rule holds no member name as text. Exactly
 * one of {@link #value()} and {@link #name()} is given; a host refuses a rule with neither or both.
 * <ul>
 *   <li>{@link Kind#METHOD}: the rule's name is the old method's, its parameter types pick the overload, and a
 *       first parameter marked {@link Self} makes it an instance method of the owner.</li>
 *   <li>{@link Kind#CONSTRUCTOR}: the rule's parameter types pick the owner's constructor; its name is free.</li>
 *   <li>{@link Kind#FIELD}: the rule's name is the old field's; it takes no parameter, or only its {@link Self}
 *       receiver for an instance field.</li>
 *   <li>{@link Kind#TYPE}: the owner <em>is</em> the old type; the parameters are an old value's parts, in its
 *       declaration's order, and the return type is the new type, which every declaration naming the old one
 *       takes too.</li>
 * </ul>
 * {@link #name()}, a binary name, stands in for {@link #value()} only when that type is gone and no class literal
 * of it compiles. An old parameter type that is itself gone is written as the type its own {@link Kind#TYPE}
 * rule returns.
 *
 * <p>The body is one {@code return} of an expression, or one statement for a {@code void} rule, written against
 * the API the bot moves to; a parameter stands for the site's part. <b>The host never runs it</b>: at each use of
 * the old element in the project it substitutes the site's parts into the body. A managed value is written beside
 * its original, marked {@link Migrated}, and the original stays; any other site is rewritten in place.
 *
 * <p>{@link RetentionPolicy#SOURCE}, like {@link Refactor}: the host finds it by class in the bot's source, or in
 * the plugin's sources jar.
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface Migrate {

    /** The old element's owner, or the old type of a {@link Kind#TYPE} rule; left out when {@link #name()} is. */
    Class<?> value() default void.class;

    /** The binary name standing in for {@link #value()} when that type is gone; empty otherwise. */
    String name() default "";

    /** What kind of element the rule replaces. */
    Kind kind() default Kind.METHOD;

    /** The kinds of element a rule replaces. */
    enum Kind {
        /** A method of the owner, static or, with a {@link Self} parameter, instance. */
        METHOD,
        /** A constructor of the owner; the rule's parameters are its parameters. */
        CONSTRUCTOR,
        /** A field or constant of the owner, named by the rule; a {@link Self} parameter for an instance field. */
        FIELD,
        /** The whole type: every declaration naming it takes the rule's return type, every value its body. */
        TYPE
    }

    /** On a rule's first parameter: the receiver of the old instance member, typed as its owner. */
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @Target(ElementType.PARAMETER)
    @interface Self {}
}
