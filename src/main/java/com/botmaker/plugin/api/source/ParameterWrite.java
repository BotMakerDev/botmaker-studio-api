package com.botmaker.plugin.api.source;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A during-a-run parameter a plugin asks the host to add to the bot's group ({@link PluginValues#addRunParameter}):
 * a method in {@code params/} and its starting copy that a run writes, {@code Progress.gold()} (2026-11-04).
 *
 * <p>Only what the host cannot know is here: the group, the name, the starting value that gives the method its type,
 * and the plugin's own annotations on the method and on the group. No Java text crosses: an annotation is named by
 * its class, its members are values, and a class among them is a member of one of the plugin's sets.
 */
public final class ParameterWrite {

    private final String group;
    private final String name;
    private final Object start;
    private final List<Mark> marks;
    private final List<Mark> groupMarks;

    private ParameterWrite(String group, String name, Object start, List<Mark> marks, List<Mark> groupMarks) {
        this.group = Objects.requireNonNull(group, "group");
        this.name = Objects.requireNonNull(name, "name");
        this.start = start;
        this.marks = List.copyOf(marks);
        this.groupMarks = List.copyOf(groupMarks);
    }

    /** A {@code String} parameter of {@code group} called {@code name}, starting at {@code start}. */
    public static ParameterWrite of(String group, String name, String start) {
        return new ParameterWrite(group, name, Objects.requireNonNull(start, "start"), List.of(), List.of());
    }

    /** An {@code int} parameter. */
    public static ParameterWrite of(String group, String name, int start) {
        return new ParameterWrite(group, name, start, List.of(), List.of());
    }

    /** A {@code boolean} parameter. */
    public static ParameterWrite of(String group, String name, boolean start) {
        return new ParameterWrite(group, name, start, List.of(), List.of());
    }

    /** This parameter with {@code mark} on its method. */
    public ParameterWrite marked(Mark mark) {
        List<Mark> more = new ArrayList<>(marks);
        more.add(Objects.requireNonNull(mark, "mark"));
        return new ParameterWrite(group, name, start, more, groupMarks);
    }

    /** This parameter with {@code mark} on its group, where the group does not carry it yet. */
    public ParameterWrite groupMarked(Mark mark) {
        List<Mark> more = new ArrayList<>(groupMarks);
        more.add(Objects.requireNonNull(mark, "mark"));
        return new ParameterWrite(group, name, start, marks, more);
    }

    /** The simple name of the group's class: {@code Progress}. */
    public String group() {
        return group;
    }

    /** The method's name. */
    public String name() {
        return name;
    }

    /** The starting value, a {@code String}, an {@code Integer} or a {@code Boolean}; its class gives the type. */
    public Object start() {
        return start;
    }

    /** The annotations on the method. */
    public List<Mark> marks() {
        return marks;
    }

    /** The annotations on the group. */
    public List<Mark> groupMarks() {
        return groupMarks;
    }

    /** One of the plugin's own annotations, by its class's name, with the values of its members. */
    public static final class Mark {

        private final String annotation;
        private final Map<String, Object> members;
        private final Class<? extends Annotation> type;

        private Mark(Class<? extends Annotation> type, Map<String, Object> members) {
            this.type = type;
            this.annotation = type.getName();
            this.members = members;
        }

        /** The annotation {@code type}, with no member set. */
        public static Mark of(Class<? extends Annotation> type) {
            return new Mark(Objects.requireNonNull(type, "type"), Map.of());
        }

        /** This annotation with its member {@code member} set to {@code text}. */
        public Mark with(String member, String text) {
            return set(member, Objects.requireNonNull(text, "text"));
        }

        /** This annotation with its member {@code member} set to {@code number}. */
        public Mark with(String member, int number) {
            return set(member, number);
        }

        /**
         * This annotation with its member {@code member}, a {@code Class}, set to the class of {@code constant} in
         * the plugin's set {@code classes}; the host resolves it in the bot's source.
         */
        public Mark withClassOf(String member, ManagedClasses<?> classes, PluginValues.Member constant) {
            return set(member, new ClassOf(Objects.requireNonNull(classes, "classes"),
                    Objects.requireNonNull(constant, "constant")));
        }

        private Mark set(String member, Object value) {
            try {
                type.getDeclaredMethod(member);
            } catch (NoSuchMethodException none) {
                throw new IllegalArgumentException(type.getName() + " has no member " + member, none);
            }
            Map<String, Object> more = new LinkedHashMap<>(members);
            more.put(member, value);
            return new Mark(type, Map.copyOf(more));
        }

        /** The binary name of the annotation. */
        public String annotation() {
            return annotation;
        }

        /** Each member's value: a {@code String}, an {@code Integer} or a {@link ClassOf}. */
        public Map<String, Object> members() {
            return members;
        }
    }

    /** A class-valued member: the class of {@code constant} in the set {@code classes}. */
    public record ClassOf(ManagedClasses<?> classes, PluginValues.Member constant) {}
}
