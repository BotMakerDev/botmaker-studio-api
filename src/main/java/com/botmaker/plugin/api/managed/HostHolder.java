package com.botmaker.plugin.api.managed;

/**
 * What a holder enum implements when its classes hold <b>this computer's</b> values, not a bot's: what your plugin
 * knows about the PC it runs on, the same for every bot — its emulators, their instances, the accounts kept for
 * them.
 *
 * <p>A marker's {@code value()} is one id enum, so this PC's values have a {@link ManagedMarker} annotation of their
 * own beside the bot's:
 *
 * <pre>{@code
 * @ManagedMarker @Retention(RUNTIME) @Target({TYPE, METHOD})
 * public @interface SdkHostValue {
 *     Id value();
 *     enum Id implements ManagedId<Holder> { EMULATORS, INSTANCES }
 *     enum Holder implements HostHolder { EMULATORS, INSTANCES }    // this PC's classes
 * }
 *
 * ManagedValue.method(SdkHostValue.Id.EMULATORS).in(SdkHostValue.Holder.EMULATORS)…
 * }</pre>
 *
 * <p>A value of a host holder is declared, listed in your plugin's {@code .values(…)} and edited exactly as a bot's
 * is, by its {@link com.botmaker.plugin.api.source.ManagedValue} constant, but through
 * {@link com.botmaker.plugin.api.StudioServices#hostValues()} rather than {@code pluginValues()}; the host refuses
 * either one handed the other's value. Since {@code .in(…)} takes only its id enum's own holder enum, javac already
 * keeps a bot's value out of a host class and the reverse.
 *
 * <p><b>Where it is.</b> Each host holder is one class in your plugin's folder of this PC, under BotMaker's own
 * configuration folder ({@code plugins/<group>/<artifact>/}, named as {@code StudioServices.resourcesDir()} is), in
 * the package {@code <group>.<artifact>}. The host writes it as it writes a bot's values, checks that it compiles
 * against your plugin, and keeps it out of every bot: it is never committed, published, put in a {@code .bmk}, moved
 * to another PC, or on a run's classpath. <b>No bot run reads it</b>; your plugin's windows and assistant tools do.
 * A run that needs a fact of this PC still gets it as a parameter of the bot.
 *
 * <p><b>Not for a secret.</b> No password or token is a value, here or in a bot.
 *
 * <p><b>Not for a parameter.</b> A parameter is a bot's user's ({@code ManagedValue.param}), so it is refused on a
 * host holder.
 */
public interface HostHolder {
}
