package com.botmaker.plugin.api.meta;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Written on a bot's managed value by a host: <em>a {@link Migrate} method wrote this one, and nobody has edited
 * it since</em>.
 *
 * <p>Running a migration again rewrites only the values still marked, from their untouched originals. Editing
 * one in the host removes the mark, so the edit is the author's from then on and a rerun leaves it alone; a
 * value of the same name the author made never carries it and is never touched.
 *
 * <p>{@link RetentionPolicy#SOURCE}, like {@link Refactor}.
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface Migrated {}
