package com.botmaker.plugin.api.overlay;

/**
 * What a call would answer now, without running the bot: {@code ImageFinder.find(Pictures.ORE)} probed on the
 * live frame says whether the ore is on screen and where.
 *
 * <p>Declared per call by method reference,
 * {@code OverlayPart.of().probe(ImageFinder::find, ImageTemplate.class, SdkProbes::find)}, and a method reference
 * itself by the contract's rule. A probe only ever reads: the host evaluates it whenever it likes.
 *
 * <p><b>Declaring a probe says the call is read-only</b>, so ▶ Try may compute a local from it rather than
 * asking for one. A call that acts — a click — may still have a probe that says what it <em>would</em> do;
 * declare that one {@link #acting}, {@code probe(ImageClicker::click, ImageTemplate.class,
 * Probe.acting(SdkProbes::wouldClick))}, and the call stays one Try runs rather than computes.
 *
 * <p>A probe that throws answers {@link ProbeResult.State#UNKNOWN} with the exception's message.
 */
@FunctionalInterface
public interface Probe {

    ProbeResult probe(ProbeContext context);

    /** Whether the probed call only reads, so its value may be computed by probing. {@code true} unless {@link #acting}. */
    default boolean readsOnly() {
        return true;
    }

    /** {@code probe}, for a call that acts: what it would do, never a value to compute a local from. */
    static Probe acting(Probe probe) {
        if (probe == null) throw new IllegalArgumentException("No probe given");
        return new Probe() {
            @Override
            public ProbeResult probe(ProbeContext context) {
                return probe.probe(context);
            }

            @Override
            public boolean readsOnly() {
                return false;
            }
        };
    }
}
