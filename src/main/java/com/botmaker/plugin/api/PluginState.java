package com.botmaker.plugin.api;

import java.nio.file.Path;
import java.util.Locale;

/**
 * Where a plugin keeps what this machine learned about one bot: {@code <bot>/.botmaker/plugins/<group>/<artifact>/}
 * (2026-11-03). A bot's state is its plugins' states, each in its own folder; the host owns where the folder is and
 * how long it lives, the plugin owns what is in it.
 *
 * <p>Both sides name it the same way: the host from the open project ({@link StudioServices#stateDir()}), a plugin's
 * runtime from the bot's working folder — a run's {@code user.dir} is the project folder — and its own coordinate.
 * Nothing under {@code .botmaker/} is committed, published or put in a {@code .bmk}.
 */
public final class PluginState {

    /** The folder under a bot that holds every plugin's state, {@code .botmaker/plugins}. */
    public static final String ROOT = ".botmaker/plugins";

    private PluginState() {
    }

    /**
     * The state folder of the plugin {@code groupId:artifactId} in the bot at {@code botDir}; nothing is created.
     * Each part is {@link #part named} as the plugin's resources folder is ({@link StudioServices#resourcesDir()}).
     */
    public static Path folder(Path botDir, String groupId, String artifactId) {
        return botDir.resolve(ROOT).resolve(part(groupId)).resolve(part(artifactId));
    }

    /**
     * {@code text} as one folder name of a plugin's: lower case, every character a Java name cannot hold made
     * {@code _}, and a {@code _} before a leading digit — {@code com.github.BotMakerDev} is
     * {@code com_github_botmakerdev}. The rule the host names a plugin's resources and holders by.
     */
    public static String part(String text) {
        StringBuilder out = new StringBuilder();
        for (char c : text.toLowerCase(Locale.ROOT).toCharArray()) {
            out.append(Character.isJavaIdentifierPart(c) && c != '$' ? c : '_');
        }
        if (!out.isEmpty() && Character.isDigit(out.charAt(0))) out.insert(0, '_');
        return out.toString();
    }
}
