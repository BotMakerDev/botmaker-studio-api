package com.botmaker.plugin.api.overlay;

import com.botmaker.plugin.api.toolbar.ActionContext.Area;
import com.botmaker.plugin.api.value.Ref;

import java.awt.image.BufferedImage;
import java.lang.reflect.Executable;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * What an {@link OverlayTool}'s pane is given each time the panel opens.
 *
 * <p>Everything a tool needs to turn what is on screen into a block: the frame, a pick on it, and an insert at
 * the user's caret. The insert names the call by method reference and hands the arguments as values; the host
 * spells them, so the tool writes no Java, and the edit is one undo step through the same gate as the user's.
 */
public interface OverlayToolContext extends OverlayContext {

    /** The watched screen now, empty when it cannot be captured. Any thread. */
    Optional<BufferedImage> frame();

    /** Where {@link #frame()} sits on the desktop, in desktop pixels. */
    Optional<Area> watchedArea();

    /** Lets the user drag a box over the watched screen; empty when cancelled. Completes on the FX thread. */
    CompletionStage<Optional<Area>> pickRegion(String prompt);

    /** Lets the user click a point on the watched screen; empty when cancelled. Completes on the FX thread. */
    CompletionStage<Optional<Pixel>> pickPoint(String prompt);

    /** The boxes the host draws over the watched screen for this tool. */
    default Marks marks() {
        return Marks.NONE;
    }

    /** Runs {@code action} once, on the FX thread, when the panel closes. */
    void onClosed(Runnable action);

    /**
     * Inserts a call of {@code call} with {@code arguments} at the caret, each argument a value the host
     * writes ({@code Pictures.ORE}'s {@code ManagedValue} constant writes as the constant). Empty when inserted;
     * otherwise the sentence saying why not (no caret, a value the host cannot write, the edit would not
     * compile). On the FX thread.
     *
     * <p>The method behind the {@code insert} overloads, for the one call a reference cannot name
     * ({@link Ref#member}).
     */
    Optional<String> insertMember(Executable call, Object... arguments);

    /** {@link #insertMember} of the method {@code call} names. */
    default <R> Optional<String> insert(Ref.Of0<R> call) {
        return insertMember(Ref.resolve(call));
    }

    /** {@link #insertMember} of the method {@code call} names. */
    default <A, R> Optional<String> insert(Ref.Of1<A, R> call, A a) {
        return insertMember(Ref.resolve(call), a);
    }

    /** {@link #insertMember} of the method {@code call} names. */
    default <A, B, R> Optional<String> insert(Ref.Of2<A, B, R> call, A a, B b) {
        return insertMember(Ref.resolve(call), a, b);
    }

    /** {@link #insertMember} of the method {@code call} names. */
    default <A, B, C, R> Optional<String> insert(Ref.Of3<A, B, C, R> call, A a, B b, C c) {
        return insertMember(Ref.resolve(call), a, b, c);
    }

    /** {@link #insertMember} of the method {@code call} names. */
    default <A, B, C, D, R> Optional<String> insert(Ref.Of4<A, B, C, D, R> call, A a, B b, C c, D d) {
        return insertMember(Ref.resolve(call), a, b, c, d);
    }

    /**
     * {@link #insertMember} of the {@code void} method {@code call} names. Its own name rather than an
     * {@code insert} overload, because javac cannot tell an overloaded {@code void} method's reference from
     * a returning one's: {@code insertVoid(Mouse::click, point)}.
     */
    default Optional<String> insertVoid(Ref.Void0 call) {
        return insertMember(Ref.resolve(call));
    }

    /** {@link #insertVoid(Ref.Void0)}, one argument. */
    default <A> Optional<String> insertVoid(Ref.Void1<A> call, A a) {
        return insertMember(Ref.resolve(call), a);
    }

    /** {@link #insertVoid(Ref.Void0)}, two arguments. */
    default <A, B> Optional<String> insertVoid(Ref.Void2<A, B> call, A a, B b) {
        return insertMember(Ref.resolve(call), a, b);
    }

    /** {@link #insertVoid(Ref.Void0)}, three arguments. */
    default <A, B, C> Optional<String> insertVoid(Ref.Void3<A, B, C> call, A a, B b, C c) {
        return insertMember(Ref.resolve(call), a, b, c);
    }

    /** {@link #insertVoid(Ref.Void0)}, four arguments. */
    default <A, B, C, D> Optional<String> insertVoid(Ref.Void4<A, B, C, D> call, A a, B b, C c, D d) {
        return insertMember(Ref.resolve(call), a, b, c, d);
    }
}
