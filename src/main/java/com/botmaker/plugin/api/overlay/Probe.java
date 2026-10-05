package com.botmaker.plugin.api.overlay;

/**
 * What a call would answer now, without running the bot: {@code ImageFinder.find(Pictures.ORE)} probed on the
 * live frame says whether the ore is on screen and where.
 *
 * <p>Declared per call by method reference, {@code OverlayPart.of().probe(ImageFinder::find, ImageTemplate.class, SdkProbes::find)},
 * and a method reference itself by the contract's rule. <b>Declaring a probe says the call is read-only</b>:
 * the host may evaluate it whenever it likes, and may compute a local from it when trying a later statement.
 * A call that acts — a click — may still have a probe that says what it <em>would</em> do, as long as the
 * probe itself only reads.
 *
 * <p>A probe that throws answers {@link ProbeResult.State#UNKNOWN} with the exception's message.
 */
@FunctionalInterface
public interface Probe {

    ProbeResult probe(ProbeContext context);
}
