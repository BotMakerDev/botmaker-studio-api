package com.botmaker.plugin.api.value;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Supplier;

/**
 * The steps of a type declaration, in the only order they can be taken. Each step offers exactly the moves
 * that are valid next, so the compiler walks an author through a declaration and an incomplete one does not
 * compile:
 *
 * <pre>{@code
 * PluginType.value(Point.class)                  // what the type is
 *         .fresh(() -> new Point(0, 0))          // what a fresh one is   | .firstConstant() | .filledBy(Owner::m)
 *         .editor(() -> MyEditors::point)        // how a person edits it
 *         .preview(() -> MyEditors::pointChip)   // optional: how it is shown without being edited
 *         .writtenAsRecord();                    // how its Java is written | .writtenAs(…) | .writtenAsEach(…)
 *                                                //   | .writtenAsLiteral() | .writtenAsConstant() | .writtenAsParts()
 *
 * ComponentType.part(Flow.Edge.class)            // a part of a value, never picked on its own
 *         .writtenAs(Flow::edge, Flow.Edge::from, Flow.Edge::to, Flow.Edge::outcome);
 * }</pre>
 *
 * <p>Every check a step can make it makes where the declaration is built — an enum's first constant on a
 * class that is no enum, a literal of a class the host has no literal for, a lambda where a method reference
 * belongs — so a mistake fails the plugin's own class initialisation, never a bot's file.
 */
public final class TypeSteps {

    private TypeSteps() {}

    /** The first step: what a fresh value is. */
    public static final class Fresh<T> {

        private final Class<T> type;

        Fresh(Class<T> type) {
            if (type == null) throw new IllegalArgumentException("No type given");
            this.type = type;
        }

        /** A fresh value is what {@code fresh} answers, asked every time one is seeded; it must not throw. */
        public Drawing<T> fresh(Supplier<? extends T> fresh) {
            if (fresh == null) throw new IllegalArgumentException(type.getName() + ": no fresh value given");
            return new Drawing<>(type, fresh);
        }

        /** An enum whose fresh value is its first constant, which is what an enum with no default means by unset. */
        public Drawing<T> firstConstant() {
            T[] constants = type.getEnumConstants();
            if (constants == null || constants.length == 0) {
                throw new IllegalArgumentException(type.getName() + " is not an enum with a constant");
            }
            return new Drawing<>(type, () -> constants[0]);
        }

        /**
         * A type the bot fills in rather than a person: a fresh value is a call to {@code method}, which the
         * host writes as {@code Owner.method()} — {@code Vision::lastMatch}. See {@link PluginType#freshCall()}.
         */
        public <R extends T> Filled<T> filledBy(Ref.Of0<R> method) {
            Executable found = Ref.resolve(method);
            if (!(found instanceof Method m) || !Modifier.isStatic(m.getModifiers())
                    || !Modifier.isPublic(m.getModifiers())) {
                throw new IllegalArgumentException(found + " is not a public static method");
            }
            return new Filled<>(type, m);
        }
    }

    /** After {@link Fresh#filledBy}: how a value the bot fills in is shown, as both its editor and preview. */
    public static final class Filled<T> {

        private final Class<T> type;
        private final Method freshCall;

        Filled(Class<T> type, Method freshCall) {
            this.type = type;
            this.freshCall = freshCall;
        }

        /** Drawn as {@code shown} wherever it appears; nothing about it is edited. */
        public DeclaredType<T> shownAs(Drawn shown) {
            if (shown == null) throw new IllegalArgumentException(type.getName() + ": nothing to show it with");
            return new DeclaredType<>(type, null, freshCall, shown, shown);
        }
    }

    /** The second step: how a person edits one. */
    public static final class Drawing<T> {

        private final Class<T> type;
        private final Supplier<? extends T> fresh;

        Drawing(Class<T> type, Supplier<? extends T> fresh) {
            this.type = type;
            this.fresh = fresh;
        }

        /** The editor, named without linking JavaFX: {@code () -> MyEditors::point}. See {@link Drawn}. */
        public Writing<T> editor(Drawn editor) {
            if (editor == null) throw new IllegalArgumentException(type.getName() + ": no editor given");
            return new Writing<>(type, fresh, editor, null);
        }
    }

    /** The last step of a type: how its Java is written. */
    public static final class Writing<T> extends CallSteps<T, DeclaredCallType<T>> {

        private final Supplier<? extends T> fresh;
        private final Drawn editor;
        private final Drawn preview;

        Writing(Class<T> type, Supplier<? extends T> fresh, Drawn editor, Drawn preview) {
            super(type);
            this.fresh = fresh;
            this.editor = editor;
            this.preview = preview;
        }

        /** Shown as {@code preview} where the host lists a value without editing it. Optional. */
        public Writing<T> preview(Drawn preview) {
            return new Writing<>(type, fresh, editor, preview);
        }

        /** A literal the host writes itself: text, a number, a character or a yes/no. */
        public DeclaredType<T> writtenAsLiteral() {
            Class<?> boxed = type.isPrimitive() ? java.lang.invoke.MethodType.methodType(type).wrap().returnType() : type;
            boolean literal = boxed == String.class || boxed == Boolean.class || boxed == Character.class
                    || Number.class.isAssignableFrom(boxed) && boxed.getName().startsWith("java.lang.");
            if (!literal) throw new IllegalArgumentException(type.getName() + " has no Java literal");
            return declared();
        }

        /** An enum, written as its constant's name. */
        public DeclaredType<T> writtenAsConstant() {
            if (!type.isEnum()) throw new IllegalArgumentException(type.getName() + " is not an enum");
            return declared();
        }

        /**
         * An interface or abstract class written as whichever of its parts built the value — each declared on
         * its own with {@link ComponentType#part} and listed among the plugin's parts. The capture source is one.
         */
        public DeclaredType<T> writtenAsParts() {
            if (!type.isInterface() && !Modifier.isAbstract(type.getModifiers())) {
                throw new IllegalArgumentException(type.getName() + " is concrete; declare the call that writes it");
            }
            return declared();
        }

        @Override
        DeclaredCallType<T> finish(CallShape<T> shape) {
            return new DeclaredCallType<>(declared(), new DeclaredCall<>(shape));
        }

        private DeclaredType<T> declared() {
            return new DeclaredType<>(type, fresh, null, editor, preview);
        }
    }

    /** A part's only step: how its Java is written. See {@link ComponentType#part}. */
    public static final class Part<T> extends CallSteps<T, DeclaredCall<T>> {

        Part(Class<T> type) {
            super(type);
            if (type == null) throw new IllegalArgumentException("No type given");
        }

        @Override
        DeclaredCall<T> finish(CallShape<T> shape) {
            return new DeclaredCall<>(shape);
        }
    }
}
