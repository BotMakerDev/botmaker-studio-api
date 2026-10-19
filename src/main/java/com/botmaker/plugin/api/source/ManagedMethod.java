package com.botmaker.plugin.api.source;

/**
 * One value a window of your plugin edits, kept as a method in the bot: {@code @SdkValue(SdkValue.Id.FLOW) public
 * static Flow flow() { return …; }}. The host rewrites only the expression the method returns, and never adds or
 * removes a method but to write it into a project that lacks it ({@link PluginValues#create}). Declared with
 * {@link ManagedValue#method}.
 *
 * <p>The bot's runtime hands the method's value to whoever claimed it
 * ({@link com.botmaker.plugin.api.managed.ManagedValues#claim}); the plugin's window reads and writes it through
 * {@link PluginValues}.
 *
 * @param <T> the class the method returns
 */
public final class ManagedMethod<T> extends ManagedValue<T> {

    private final T initial;

    ManagedMethod(Key key, String holder, Class<T> type, T initial, String reason) {
        super(key, holder, type, reason);
        this.initial = initial;
    }

    /**
     * What a created method first returns, or null for the type's own fresh value — which a type declared only by
     * its parts ({@code ComponentType}) has not got. A method with no {@link #holder()} is never created.
     */
    @Override
    public T initial() {
        return initial;
    }

    /** After {@link ManagedValue#method}: where the value lives. */
    public static final class Steps {

        private final Key key;

        Steps(Key key) {
            this.key = key;
        }

        /** In the class {@code holder} — {@code "Sdk"} — which the host writes when a project has none. */
        public TypeStep in(String holder) {
            return new TypeStep(key, requireText(holder, "holder"));
        }

        /** Somewhere the host may open and never create: {@code .openedOnly().holds(T.class)}. */
        public OpenedStep openedOnly() {
            return new OpenedStep(key);
        }
    }

    /** After {@link Steps#in}: what the method returns. */
    public static final class TypeStep {

        private final Key key;
        private final String holder;

        private TypeStep(Key key, String holder) {
            this.key = key;
            this.holder = holder;
        }

        /**
         * A method returning a {@code type}, first returning {@code initial} when the host creates it — or,
         * with {@code null}, the type's own fresh value.
         */
        public <T> Reason<ManagedMethod<T>> holds(Class<T> type, T initial) {
            if (type == null) throw new IllegalArgumentException(key.id() + ": no type given");
            return new Reason<>(reason -> new ManagedMethod<>(key, holder, type, initial, reason));
        }
    }

    /** After {@link Steps#openedOnly}: what the method returns. */
    public static final class OpenedStep {

        private final Key key;

        private OpenedStep(Key key) {
            this.key = key;
        }

        /** A method returning a {@code type}, which the host opens and never writes. */
        public <T> Reason<ManagedMethod<T>> holds(Class<T> type) {
            if (type == null) throw new IllegalArgumentException(key.id() + ": no type given");
            return new Reason<>(reason -> new ManagedMethod<>(key, null, type, null, reason));
        }
    }
}
