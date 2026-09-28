package com.botmaker.plugin.api.slot;

import com.botmaker.plugin.api.value.Drawn;
import javafx.scene.Node;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * The steps of a {@link SlotEditor} declaration: {@link SlotEditor#onParameter}, {@link SlotEditor#forType} or
 * {@link SlotEditor#when} says which values the editor claims, and {@link Drawing#draw} says how it draws them.
 *
 * <p>The drawing is a {@link Drawn}, for the reason a type's editor is: building a plugin's editor list must
 * link no JavaFX, because a headless host builds it too.
 */
public final class EditorSteps {

    private EditorSteps() {}

    /** Values of {@code type}, wherever one is edited. */
    static Drawing forType(Class<?> type) {
        Objects.requireNonNull(type, "type");
        return new Drawing(ctx -> ctx.type().is(type));
    }

    /**
     * Arguments passed to a parameter carrying {@code marker}. Refused at once when the annotation cannot be
     * read at run time or cannot sit on a parameter, since either is an editor that would never appear.
     */
    static Drawing onParameter(Class<? extends Annotation> marker) {
        Objects.requireNonNull(marker, "marker");
        Retention retention = marker.getAnnotation(Retention.class);
        if (retention == null || retention.value() != RetentionPolicy.RUNTIME) {
            throw new IllegalArgumentException("@" + marker.getName()
                    + " is not @Retention(RUNTIME): javac keeps it out of reflection, so no parameter carries it");
        }
        Target target = marker.getAnnotation(Target.class);
        if (target != null && !Arrays.asList(target.value()).contains(ElementType.PARAMETER)) {
            throw new IllegalArgumentException("@" + marker.getName() + " cannot be put on a parameter");
        }
        String name = marker.getName();
        return new Drawing(ctx -> ctx.slot().flatMap(SlotContext::parameter)
                .map(parameter -> carries(parameter, name))
                .orElse(false));
    }

    /** Values {@code matches} accepts. */
    static Drawing when(Predicate<ValueContext> matches) {
        return new Drawing(Objects.requireNonNull(matches, "matches"));
    }

    /**
     * Compared by the annotation's binary name, never by {@code Class}: the call is loaded on the plugin's
     * loader, and a name is what is true across two.
     */
    private static boolean carries(Parameter parameter, String annotation) {
        for (Annotation each : parameter.getAnnotations()) {
            if (each.annotationType().getName().equals(annotation)) return true;
        }
        return false;
    }

    /** Which values the editor claims is said; how it draws them is next. */
    public static final class Drawing {

        private final Predicate<ValueContext> matches;

        private Drawing(Predicate<ValueContext> matches) {
            this.matches = matches;
        }

        /** The editor, drawn by {@code editor}: {@code () -> LaunchEditors::program}. */
        public SlotEditor draw(Drawn editor) {
            return build(Objects.requireNonNull(editor, "editor"), null);
        }

        /** The editor, with a {@link SlotEditor#preview} drawn by {@code preview}. */
        public SlotEditor draw(Drawn editor, Drawn preview) {
            return build(Objects.requireNonNull(editor, "editor"), Objects.requireNonNull(preview, "preview"));
        }

        private SlotEditor build(Drawn editor, Drawn preview) {
            Predicate<ValueContext> claims = matches;
            return new SlotEditor() {
                @Override
                public boolean matches(ValueContext ctx) {
                    return claims.test(ctx);
                }

                @Override
                public Node create(ValueContext ctx) {
                    return editor.draw(ctx);
                }

                @Override
                public Node preview(ValueContext ctx) {
                    return preview == null ? null : preview.draw(ctx);
                }
            };
        }
    }
}
