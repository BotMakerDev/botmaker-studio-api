package com.botmaker.plugin.api.value;

import com.botmaker.plugin.api.slot.ValueContext;
import javafx.scene.Node;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * How a node is drawn for a value, named without linking JavaFX: {@code () -> MyEditors::point}.
 *
 * <p><b>The extra arrow is load-bearing.</b> A method reference that returns a {@code Node} links JavaFX the
 * moment it is evaluated, which for a {@code static final} declaration is when the plugin's type list is
 * built. A headless host ({@code botmaker plugin validate}, the registry's CI) builds that list without
 * JavaFX and would report the plugin as not loading at all. The outer lambda returns a {@code Function},
 * which links nothing; the inner one is evaluated only when the host draws.
 *
 * <p><b>Name a method declared to return {@code Node}, in another class.</b> An inner lambda is still a
 * method of the declaring class, and one whose body answers a subclass — {@code ctx -> new Label(…)} — makes
 * the verifier load {@code Label} and {@code Node} the moment that class loads, which is the failure the
 * arrow exists to avoid.
 */
@FunctionalInterface
public interface Drawn extends Supplier<Function<ValueContext, Node>> {

    /** The node for {@code ctx}. */
    default Node draw(ValueContext ctx) {
        return get().apply(ctx);
    }
}
