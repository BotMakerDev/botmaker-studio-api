package com.botmaker.plugin.api.value;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an instance method of a plugin's own type as a link the host writes after the type's factory: it answers
 * a copy of the value with one thing changed. Every declaration of the type ({@link ComponentType#part},
 * {@link PluginType#value}) then chains it with no {@code .with} or {@code .flag} step.
 *
 * <pre>{@code
 * public record Tree(List<Node> nodes, boolean isDragged, int wheel, …) {
 *     public static Tree of(List<Node> nodes) { … }
 *     @Link public Tree dragged() { … }        // a flag, read by isDragged()
 *     @Link public Tree wheel(int notches) { … }  // a with, read by wheel()
 * }
 *
 * static final DeclaredCall<Tree> TREE = ComponentType.part(Tree.class).writtenAs(Tree::of);
 * }</pre>
 *
 * <p><b>How a link finds its part</b>, by name and type on the compiled class:
 *
 * <ul>
 *   <li>{@code x(A)} — a with — is read by the public no-argument method {@code x()} answering an {@code A}, or
 *       {@code boolean isX()} when {@code A} is {@code boolean}.</li>
 *   <li>{@code x()} — a flag, which only turns a thing on ({@link Wither}) — is read by {@code boolean isX()}.</li>
 * </ul>
 *
 * <p>The mark may sit on the method or on one it overrides, so an interface marks the links all its
 * implementations share once. Links are written in the order of their readers among a record's components, then
 * by name. A method marked here with no reader, or answering something other than the type, is refused when the
 * declaration is built — in the plugin's own tests ({@code PluginTests.assertDeclared}), never in a bot.
 *
 * <p>A link whose part is read under another name, or held as something the method does not take, is declared
 * explicitly with {@link DeclaredCall#with} or {@link DeclaredCall#flag}; that step replaces the inferred link in
 * its place. A type the plugin does not own (the JDK's) has no mark and is always declared explicitly.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Link {
}
