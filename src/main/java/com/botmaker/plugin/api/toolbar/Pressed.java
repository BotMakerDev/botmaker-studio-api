package com.botmaker.plugin.api.toolbar;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * What pressing a toolbar item does, named without linking the window it opens:
 * {@code () -> BotSettingsWindow::open}.
 *
 * <p><b>The extra arrow is load-bearing</b>, for the reason it is on {@link com.botmaker.plugin.api.value.Drawn}.
 * A method reference is linked the moment it is evaluated, which for a {@code static final} item is when the
 * plugin's toolbar list is built — and linking {@code BotSettingsWindow::open} loads a class full of JavaFX.
 * A headless host ({@code botmaker plugin validate}, the registry's CI) builds that list without JavaFX. The
 * outer lambda returns a {@code Consumer}, which links nothing; the inner reference is evaluated on the press.
 */
@FunctionalInterface
public interface Pressed extends Supplier<Consumer<ActionContext>> {

    /** Runs the press. */
    default void press(ActionContext context) {
        get().accept(context);
    }
}
