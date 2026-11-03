package com.botmaker.plugin.api.run;

import com.botmaker.plugin.api.TraceLine;

import java.util.Objects;
import java.util.Optional;

/**
 * A chart of a bot's runs that a plugin offers in the host's reports, by title: which of the plugin's own line
 * values it groups by ({@link TraceLine#values()}), how it is drawn, and which lines it counts.
 *
 * <p>Built from {@link #of} and steps, each answering a copy: {@code ReportView.of("Failed moves", Chart.HEATMAP,
 * "from").against("to").only(Level.ERROR)}. Named by a {@link com.botmaker.plugin.api.ReportViewHolder}. A view
 * names keys, never values: the host counts whatever the bot's lines carried under them, and says so when the
 * reports read carry none.
 */
public final class ReportView {

    /** How a view is drawn. */
    public enum Chart {
        /** A bar per value, stacked by level. */
        BARS("Bars"),
        /** A bar per day, stacked by value. */
        BARS_BY_DAY("Bars by day"),
        /** A line per value over the days. */
        LINES("Lines"),
        /** A value × day grid, or with {@link #against} a value × value grid. */
        HEATMAP("Heatmap"),
        /** Each value's share. */
        DONUT("Donut"),
        /** Every value with its counts, sortable. */
        TABLE("Table");

        private final String displayName;

        Chart(String displayName) {
            this.displayName = displayName;
        }

        /** What a picker shows. */
        public String displayName() {
            return displayName;
        }
    }

    /** Which of a run's lines a view counts. */
    public enum Lines {
        /** Its warnings and errors, what runners' issues carry too; one lacking the value counts as unknown. */
        PROBLEMS,
        /** Every line carrying the view's value, at any level; only a run's own log has them, never an issue. */
        EVERY_LINE
    }

    private final String title;
    private final Chart chart;
    private final String dimension;
    private final String against;
    private final TraceLine.Level level;
    private final Lines lines;
    private final String weight;

    private ReportView(String title, Chart chart, String dimension, String against, TraceLine.Level level,
                       Lines lines, String weight) {
        this.title = title;
        this.chart = chart;
        this.dimension = dimension;
        this.against = against;
        this.level = level;
        this.lines = lines;
        this.weight = weight;
    }

    /**
     * A view titled {@code title} drawing {@code chart} of the values the plugin's lines carry under
     * {@code dimension}, counting {@link Lines#PROBLEMS} of every level.
     *
     * @throws IllegalArgumentException when the title or the key is blank
     * @throws NullPointerException     when the chart is null
     */
    public static ReportView of(String title, Chart chart, String dimension) {
        return new ReportView(text(title, "title"), Objects.requireNonNull(chart, "chart"), text(dimension, "dimension"),
                null, null, Lines.PROBLEMS, null);
    }

    /** This view as a grid: a row per value of its dimension, a column per value of {@code to}; for a heatmap. */
    public ReportView against(String to) {
        return new ReportView(title, chart, dimension, text(to, "against"), level, lines, weight);
    }

    /** This view counting only the lines at {@code level}. */
    public ReportView only(TraceLine.Level level) {
        return new ReportView(title, chart, dimension, against, Objects.requireNonNull(level, "level"), lines, weight);
    }

    /** This view counting {@code lines}. */
    public ReportView counting(Lines lines) {
        return new ReportView(title, chart, dimension, against, level, Objects.requireNonNull(lines, "lines"), weight);
    }

    /**
     * This view adding up, per value, the number each line carries under {@code key} instead of counting lines: a
     * time spent, a number of visits. A line whose {@code key} is no whole number adds nothing.
     */
    public ReportView weighedBy(String key) {
        return new ReportView(title, chart, dimension, against, level, lines, text(key, "weighedBy"));
    }

    /** What the host's picker shows. */
    public String title() {
        return title;
    }

    public Chart chart() {
        return chart;
    }

    /** The key of the plugin's line values it groups by. */
    public String dimension() {
        return dimension;
    }

    /** The second key of a grid, its columns; empty for one dimension. */
    public Optional<String> against() {
        return Optional.ofNullable(against);
    }

    /** The one level counted; empty for every level. */
    public Optional<TraceLine.Level> level() {
        return Optional.ofNullable(level);
    }

    public Lines lines() {
        return lines;
    }

    /** The key whose numbers are added up; empty when lines are counted. */
    public Optional<String> weight() {
        return Optional.ofNullable(weight);
    }

    private static String text(String text, String what) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("a report view's " + what + " is blank");
        return text;
    }

    @Override
    public String toString() {
        return "ReportView[" + title + "]";
    }
}
