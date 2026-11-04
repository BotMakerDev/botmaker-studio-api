package com.botmaker.plugin.api.source;

import java.time.Instant;
import java.util.Optional;

/**
 * One during-a-run parameter of the bot as the host reads it ({@link PluginValues#runParameters}), built by the host:
 * what a plugin shows of the values a run keeps and how old they are (2026-11-04). A parameter changed outside a run
 * is never one: the person's values, which may be secret, do not cross.
 *
 * @param group  the simple name of the group's class
 * @param name   the method's name
 * @param type   the type as the group writes it
 * @param value  the value, for showing
 * @param keptAt when a run last wrote it; empty when no run has
 */
public record RunParameter(String group, String name, String type, String value, Optional<Instant> keptAt) {}
