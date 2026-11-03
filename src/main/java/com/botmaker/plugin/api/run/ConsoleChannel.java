package com.botmaker.plugin.api.run;

/**
 * {@link RunChannel#console()}: the channel's defaults, plus the debug switch they cannot hold. Names only this
 * module and the JDK, as it runs inside a bot.
 */
final class ConsoleChannel implements RunChannel {

    private volatile boolean debug;

    ConsoleChannel(boolean debug) {
        this.debug = debug;
    }

    @Override
    public boolean debug() {
        return debug;
    }

    @Override
    public void debug(boolean on) {
        debug = on;
    }
}
