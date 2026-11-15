package com.botmaker.plugin.api.run;

import com.botmaker.plugin.api.StudioServices;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * A way to have more of a {@link PluginPool}'s candidates, offered when fewer runs can start than were asked for:
 * a title and what your plugin does when the user picks it, {@code PoolOffer.of("Create 2 instances",
 * SdkPools::createInstances)}.
 *
 * <p><b>The host shows the title and runs nothing by itself.</b> It knows nothing of what the offer creates: only
 * the user's click runs it, and an assistant is told the titles and can run none. The action is yours whole — open
 * your own window, queue your own work, ask what it needs in your own prompts — and returns at once; the host
 * deals again when the user asks.
 */
public final class PoolOffer {

    private final String title;
    private final Consumer<StudioServices> action;

    private PoolOffer(String title, Consumer<StudioServices> action) {
        this.title = title;
        this.action = action;
    }

    /**
     * An offer the host shows as {@code title} (stripped), a short command saying what it adds.
     *
     * @param action what runs on the user's click, handed the host's services: in a host with windows, on the
     *               JavaFX thread. A host with none (the assistant's, a command line) lists the titles and runs no
     *               offer, so the action may open a window
     * @throws IllegalArgumentException for a blank title
     * @throws NullPointerException     for no action
     */
    public static PoolOffer of(String title, Consumer<StudioServices> action) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("An offer needs a title");
        return new PoolOffer(title.strip(), Objects.requireNonNull(action, "action"));
    }

    /** What the host shows on the offer. */
    public String title() {
        return title;
    }

    /** Runs the offer: the host calls it only on the user's click. */
    public void run(StudioServices services) {
        action.accept(services);
    }

    @Override
    public String toString() {
        return "PoolOffer[" + title + "]";
    }
}
