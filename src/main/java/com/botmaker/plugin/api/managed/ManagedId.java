package com.botmaker.plugin.api.managed;

/**
 * What the id enum of a {@link ManagedMarker} annotation implements, naming the enum of the bot's classes its values
 * live in — {@code enum Id implements ManagedId<Holder>} beside {@code enum Holder { SDK, PICTURES }} — so a
 * declaration's {@code .in(…)} takes one of that plugin's holders and javac refuses any other:
 *
 * <pre>{@code
 * public @interface SdkValue {
 *     Id value();
 *
 *     enum Id implements ManagedId<Holder> { SETTINGS, PICTURES }
 *
 *     enum Holder { SDK, PICTURES }
 * }
 * }</pre>
 *
 * <p>A holder constant is UPPER_SNAKE and names the class by its parts, each keeping its first letter:
 * {@code LAUNCH_TARGETS} is the class {@code LaunchTargets}, {@code SDK} is {@code Sdk}. A declaration refuses a constant of
 * any other spelling, and an enum two of whose constants name classes that differ only by case.
 *
 * @param <H> the holder enum
 */
public interface ManagedId<H extends Enum<H>> {
}
