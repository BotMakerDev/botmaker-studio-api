package com.botmaker.plugin.api;

/**
 * Where a plugin says what happened, attributed to it by the host — {@link StudioServices#log()}.
 *
 * <p>A plugin has no console of its own: what it printed went to Studio's standard error, unattributed and
 * unseen. The host shows these lines under the plugin's name. Not a channel to the user: a line the user must
 * act on is a {@link Dialogs dialog}, and a running commentary is {@link StudioServices#status(String)}.
 *
 * <p>Implemented by the host only, so it may gain members.
 */
public interface PluginLog {

    /** Writes to standard error, unattributed: what a host that keeps no log answers. */
    PluginLog STANDARD_ERROR = new PluginLog() {
        @Override
        public void info(String message) {
            System.err.println(message);
        }

        @Override
        public void warn(String message) {
            System.err.println("WARN " + message);
        }

        @Override
        public void error(String message, Throwable cause) {
            System.err.println("ERROR " + message);
            if (cause != null) cause.printStackTrace();
        }
    };

    /** Something worth knowing when a plugin is asked why it behaved as it did. */
    void info(String message);

    /** Something went wrong, and the plugin carried on. */
    void warn(String message);

    /** Something failed; {@code cause} may be {@code null}. */
    void error(String message, Throwable cause);
}
