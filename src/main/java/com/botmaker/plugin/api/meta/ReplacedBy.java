package com.botmaker.plugin.api.meta;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Written on a deprecated type, method, constructor or field: <em>this is what to use instead</em>.
 *
 * <h2>What it is for</h2>
 *
 * <p>When a bot is upgraded to a newer version of a plugin, Studio has both jars in hand and diffs them. A
 * diff can see that {@code ImageClicker#click} is gone and {@code IClicker#tap} has appeared; it cannot see
 * that one became the other. Read as a removal, a rename turns into hundreds of calls replaced by default
 * values — the single worst thing an upgrade can do to someone's project.
 *
 * <p>It is read out of whichever jar still carries the old spelling, and it is the <b>only</b> half there is:
 *
 * <pre>{@code
 * // 1.2.0 — and every release after it, because the old spelling never goes away
 * @Deprecated(since = "1.2.0", forRemoval = true)
 * @ReplacedBy("com.botmaker.sdk.api.vision.IClicker#tap")
 * public boolean click(ImageTemplate t) { return tap(t); }
 *
 * public boolean tap(ImageTemplate t) { … }
 * }</pre>
 *
 * <h2>Why there is no back edge, and what pays for that</h2>
 *
 * <p>There was one until 2026-08-27 — {@code @Replaces}, written on the survivor, naming what it took over.
 * It existed because Studio holds only two jars at an upgrade, the bot's pin and the target, so a bot jumping
 * 1.0 → 3.0 could not see a pointer added in 2.0 on an element deleted in 3.0: neither jar has it.
 *
 * <p>That gap closes the moment a deprecated element is <b>never deleted</b>. The target jar then still
 * carries the old member and its forward pointer, so one end answers every upgrade, chained renames
 * included — {@code a → b} in 2.0 and {@code b → c} in 3.0 land a bot still spelling it {@code a} on
 * {@code c}, with no intermediate jar fetched. The premise is enforced rather than trusted: japicmp refuses
 * a removal from the SDK's {@code com.botmaker.sdk.api.**}, and the accepted price is that the package only
 * ever grows.
 *
 * <h2>The grammar</h2>
 *
 * <p>A target is {@code fqn} for a type, {@code fqn#member} for a method or field, {@code fqn#<init>} for a
 * constructor. An enum constant <em>is</em> a static field, so {@code …interaction.Key#ENTER} names one.
 * There is <b>no arity</b> in the string: the annotation sits on one specific overload, so the parameter
 * count of both ends is already known from the bytecode.
 *
 * <p>The target need not live in the same module as the element pointing at it. A type moving from a plugin
 * into this contract is an ordinary rename with an unusually long fully-qualified name, and the vocabulary's
 * own move — {@code com.botmaker.sdk.api.meta} to here — is written exactly that way.
 *
 * <h2>One old member, two new ones</h2>
 *
 * <p>{@link #value()} is an <b>array</b>, and a second one, {@link #whens()}, carries a sentence per
 * candidate. That is the whole of the split mechanism, and it is deliberately not a resolution mechanism:
 * the author states the alternatives and the condition each applies under, and the <em>user</em> picks, once
 * per call site, from the candidates that compile there. See {@link #value()} for why per-site is the only
 * correct granularity.
 *
 * <h2>The rules that make a pointer safe</h2>
 *
 * <ul>
 *   <li><b>An empty value is an explicit statement</b>, not an omission: nothing takes this element's place,
 *       {@code {}} and {@code {""}} being the same statement,
 *       and Studio should replace its uses with a default value and mark them for review. The annotation is
 *       required on every deprecated public element precisely so that the author decides rather than forgets
 *       — a build gate checks it.
 *   <li><b>Write it at the moment of the change</b>, in the release that deprecates the element, never
 *       reconstructed later. That is also when the element it names is still compilable, which is what lets
 *       the gate verify the link with nothing but this build.
 *   <li><b>A pointer is a suggestion Studio checks, not an instruction it obeys.</b> The two return types are
 *       both in hand: in statement position the target's type cannot matter and the redirect is always taken;
 *       in expression position the redirect is taken only when the new type fits where the old one did, and
 *       otherwise the call falls back to a default value. A pointer can therefore be wrong without producing
 *       a bot that compiles and misbehaves.
 *   <li><b>A pointer can be corrected</b> in a later release — it is an ordinary annotation, not an identity.
 * </ul>
 *
 * <h2>Why {@link RetentionPolicy#CLASS}</h2>
 *
 * <p>Studio reads this from the published jar with the ClassGraph scan it already runs over a plugin's types,
 * so it has to survive compilation. It does not need to be visible at runtime: nothing in a running bot ever
 * asks what a member used to be called.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR})
public @interface ReplacedBy {

    /**
     * What to use instead — each {@code fqn}, {@code fqn#member} or {@code fqn#<init>} — <b>in preference
     * order, first preferred</b>. Empty ({@code {}}, and equivalently {@code {""}}) means nothing takes this
     * element's place.
     *
     * <p>Almost every pointer names exactly one target, and {@code @ReplacedBy("…#tap")} is still written and
     * still means what it always did — an array-typed annotation element accepts a bare value. The array form
     * exists for the case the single-target model could not express at all: a <b>split</b>, where one old
     * member becomes two, and <em>which</em> one a given call meant is a property of that call rather than of
     * the member. {@code Mouse#scroll(int)} is the worked example — the sign of {@code notches} decides
     * whether it meant {@code scrollUp} or {@code scrollDown}, and no annotation can know the sign.
     *
     * <p>So a split does not resolve here; it is <b>offered</b>. Studio lists every call site with its own
     * combo of the candidates that compile <em>there</em>, preselected to the first that survives, and
     * {@link #whens()} is what makes that combo readable. A menu of bare member names is not a choice anybody
     * can make, which is why the gate refuses a multi-target pointer whose {@code whens()} is missing.
     */
    String[] value() default {};

    /**
     * One sentence per {@link #value() candidate}, in the same order and the same length — <em>"when
     * {@code notches} is positive"</em>, <em>"when negative"</em>. This is what the user actually chooses
     * between at a call site; the target's own name says what it is called, never when it applies.
     *
     * <p>Empty is the normal state of a one-target pointer, where there is nothing to distinguish. It is
     * refused on a split: the gate requires a non-blank sentence for every candidate as soon as there are two
     * or more, because the alternative is a dialog asking someone to pick between two method names on no
     * information.
     */
    String[] whens() default {};

    /**
     * The author's own sentence about this move, shown to the user <b>verbatim</b>.
     *
     * <p>Everything else here is machine-readable: {@link #value()} is a target, {@link #behaviourChanged()}
     * is a flag. Neither can say <em>why</em>, and Studio's generated sentence ("{@code click} is now
     * {@code tap}") only restates what the diff already showed. This is the one channel through which the
     * person who made the change speaks to the person whose bot it lands on — "the new one measures from the
     * template's centre, not its top-left" — so Studio prefers it over its own wording and never paraphrases
     * it, truncates it or folds it into a list.
     *
     * <p>Write it in the second person and keep it to a sentence or two; it is read in a dialog beside the
     * call sites it applies to, not in a changelog. Empty means "the pointer says everything there is to say".
     */
    String note() default "";

    /**
     * True when the replacement <b>does something different</b>, not merely something with a different name.
     *
     * <p>This is the one gap the redirect model cannot see by construction. Studio decides whether to take a
     * pointer by comparing the two <em>shapes</em>: a same-shape redirect is always safe to apply, so a
     * rename lands silently and correctly, which is the whole point. But "same shape, different behaviour" —
     * a click that now targets the match's centre, a wait that now counts from a different instant — is
     * exactly a same-shape redirect, and the bot compiles and quietly does something else. That is the worst
     * outcome the model admits, and nothing in the bytecode reveals it.
     *
     * <p>So the author says it. Studio marks every redirected call site {@link Refactor @Refactor} when this is
     * set, even where the shape did not move, with {@link #note()} as the mark's text — which is why the gate
     * refuses {@code behaviourChanged = true} with a blank {@code note}: a flag with no sentence tells the
     * user their bot changed and nothing about how.
     */
    boolean behaviourChanged() default false;
}
