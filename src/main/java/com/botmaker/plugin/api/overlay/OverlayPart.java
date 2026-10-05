package com.botmaker.plugin.api.overlay;

import com.botmaker.plugin.api.toolbar.Pressed;
import com.botmaker.plugin.api.value.Ref;

import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A plugin's part of the overlay editor — see the package comment for the four things it answers.
 *
 * <pre>{@code
 * static final OverlayPart PART = OverlayPart.of()
 *         .targets(SdkOverlay::targets)
 *         .watched(SdkOverlay::watched).changeWatched(() -> SdkOverlay::changeWatched)
 *         .tool(SdkTools.PICTURE).tool(SdkTools.POINT)
 *         .probe(ImageFinder::find, ImageTemplate.class, SdkProbes::find)
 *         .probe(ImageClicker::click, ImageTemplate.class, SdkProbes::wouldClick);
 * }</pre>
 *
 * <p>Declared through {@code PluginDeclaration.overlay(() -> SdkOverlay.PART)}, one per plugin. Every step is
 * optional and answers a copy. {@link #targetsFor} and {@link #watchedFor} are asked again whenever the bot's code
 * changes, on the FX thread, and must be quick: read the plugin's own values, never the screen.
 *
 * <p><b>A probe is keyed by the call it probes</b>, named by method reference plus the class of each of its
 * parameters (a receiver counts as the first). The classes are what pick an overloaded call: a bare
 * {@code ImageFinder::find} names two methods, and javac cannot choose between them from the reference alone.
 * {@link #probeMember} takes the call a reference cannot name ({@link Ref#member}). Declaring the same call
 * twice is refused, as is a tool id used twice.
 */
public final class OverlayPart {

    /** The bot methods worth editing from the overlay, in chip order. A method reference. */
    @FunctionalInterface
    public interface Targets {
        List<OverlayTarget> targets(OverlayContext context);
    }

    /** The screen the bot watches, empty when the plugin cannot say. A method reference. */
    @FunctionalInterface
    public interface WatchedBy {
        Optional<Watched> watched(OverlayContext context);
    }

    /** A declared probe and the call it probes. */
    public record ProbedCall(Executable call, Probe probe) {
    }

    private static final OverlayPart EMPTY = new OverlayPart(null, null, null, List.of(), List.of());

    private final Targets targets;
    private final WatchedBy watched;
    private final Pressed changeWatched;
    private final List<OverlayTool> tools;
    private final List<ProbedCall> probes;

    private OverlayPart(Targets targets, WatchedBy watched, Pressed changeWatched, List<OverlayTool> tools,
                        List<ProbedCall> probes) {
        this.targets = targets;
        this.watched = watched;
        this.changeWatched = changeWatched;
        this.tools = List.copyOf(tools);
        this.probes = List.copyOf(probes);
    }

    /** A part that answers nothing yet. */
    public static OverlayPart of() {
        return EMPTY;
    }

    /** Where blocks go. */
    public OverlayPart targets(Targets targets) {
        return new OverlayPart(Objects.requireNonNull(targets, "targets"), watched, changeWatched, tools, probes);
    }

    /** Which screen the bot watches. */
    public OverlayPart watched(WatchedBy watched) {
        return new OverlayPart(targets, Objects.requireNonNull(watched, "watched"), changeWatched, tools, probes);
    }

    /**
     * The plugin's own picker for the watched screen, opened by the panel's ⇄ Change. A supplier of the
     * handler, as a toolbar item's press is, so declaring it loads no window class.
     */
    public OverlayPart changeWatched(Pressed change) {
        return new OverlayPart(targets, watched, Objects.requireNonNull(change, "change"), tools, probes);
    }

    /** A pane in the panel's tool tabs, after the ones already declared. */
    public OverlayPart tool(OverlayTool tool) {
        Objects.requireNonNull(tool, "tool");
        for (OverlayTool declared : tools) {
            if (declared.id().equals(tool.id())) {
                throw new IllegalArgumentException("Overlay tool " + tool.id() + " is declared twice");
            }
        }
        List<OverlayTool> more = new ArrayList<>(tools);
        more.add(tool);
        return new OverlayPart(targets, watched, changeWatched, more, probes);
    }

    /** {@code probe} answers for {@code call}, which takes nothing: {@code probe(Vision::lastMatch, SdkProbes::lastMatch)}. */
    public <R> OverlayPart probe(Ref.Of0<R> call, Probe probe) {
        return probeMember(Ref.resolve(call), probe);
    }

    /**
     * {@code probe} answers for {@code call}, whose parameter is an {@code a}:
     * {@code probe(ImageFinder::find, ImageTemplate.class, SdkProbes::find)}. The class picks the overload.
     */
    public <A, R> OverlayPart probe(Ref.Of1<A, R> call, Class<A> a, Probe probe) {
        return probeMember(Ref.resolve(call), probe);
    }

    /** {@code probe} answers for {@code call}, whose parameters are an {@code a} and a {@code b}. */
    public <A, B, R> OverlayPart probe(Ref.Of2<A, B, R> call, Class<A> a, Class<B> b, Probe probe) {
        return probeMember(Ref.resolve(call), probe);
    }

    /** {@code probe} answers for {@code call}, whose parameters are {@code a} to {@code c}. */
    public <A, B, C, R> OverlayPart probe(Ref.Of3<A, B, C, R> call, Class<A> a, Class<B> b, Class<C> c,
                                          Probe probe) {
        return probeMember(Ref.resolve(call), probe);
    }

    /** {@code probe} answers for {@code call}, whose parameters are {@code a} to {@code d}. */
    public <A, B, C, D, R> OverlayPart probe(Ref.Of4<A, B, C, D, R> call, Class<A> a, Class<B> b, Class<C> c,
                                             Class<D> d, Probe probe) {
        return probeMember(Ref.resolve(call), probe);
    }

    /** {@code probe} answers for {@code call}, found by {@link Ref#member} — for the call a reference cannot name. */
    public OverlayPart probeMember(Executable call, Probe probe) {
        Objects.requireNonNull(call, "call");
        Objects.requireNonNull(probe, "probe");
        for (ProbedCall declared : probes) {
            if (declared.call().equals(call)) throw new IllegalArgumentException(call + " has two probes");
        }
        List<ProbedCall> more = new ArrayList<>(probes);
        more.add(new ProbedCall(call, probe));
        return new OverlayPart(targets, watched, changeWatched, tools, more);
    }

    /** The targets for the open project; empty when none are declared or the plugin answers {@code null}. */
    public List<OverlayTarget> targetsFor(OverlayContext context) {
        List<OverlayTarget> answered = targets == null ? null : targets.targets(context);
        return answered == null ? List.of() : answered.stream().filter(Objects::nonNull).toList();
    }

    /** The watched screen for the open project; empty when not declared or the plugin cannot say. */
    public Optional<Watched> watchedFor(OverlayContext context) {
        Optional<Watched> answered = watched == null ? null : watched.watched(context);
        return answered == null ? Optional.empty() : answered;
    }

    /** The plugin's picker for the watched screen, if it declared one. */
    public Optional<Pressed> changeWatched() {
        return Optional.ofNullable(changeWatched);
    }

    /** The tool panes, in declared order. */
    public List<OverlayTool> tools() {
        return tools;
    }

    /** The declared probes, in declared order. */
    public List<ProbedCall> probes() {
        return probes;
    }

    /** The probe declared for {@code call}, compared as the host compares a call: declaring class, name, parameter types by name. */
    public Optional<Probe> probeFor(Executable call) {
        if (call == null) return Optional.empty();
        for (ProbedCall declared : probes) {
            if (sameCall(declared.call(), call)) return Optional.of(declared.probe());
        }
        return Optional.empty();
    }

    /** Nothing declared at all. */
    public boolean isEmpty() {
        return targets == null && watched == null && changeWatched == null && tools.isEmpty() && probes.isEmpty();
    }

    private static boolean sameCall(Executable a, Executable b) {
        if (!a.getDeclaringClass().getName().equals(b.getDeclaringClass().getName())) return false;
        if (!a.getName().equals(b.getName())) return false;
        Class<?>[] pa = a.getParameterTypes();
        Class<?>[] pb = b.getParameterTypes();
        if (pa.length != pb.length) return false;
        for (int i = 0; i < pa.length; i++) {
            if (!pa[i].getName().equals(pb[i].getName())) return false;
        }
        return true;
    }
}
