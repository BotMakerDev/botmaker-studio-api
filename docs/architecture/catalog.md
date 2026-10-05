# The catalog, and why it is reflection

**The host discovers the classes**: when a plugin's `catalog()` is empty, the default,
`botmaker-plugin-host`'s `Palettes.of(plugin)` finds every `@Palette` class in that plugin's jar and hands
them to `PaletteCatalog.of(…)`. No plugin lists its classes, and none annotates a value type: what an offered
member takes or returns from the same jar (by code source; the JDK's classes have none) is catalogued, not
offered, transitively. **Members are discovered, never named**: every
public declared method of a `@Palette` class is offered unless something on it says otherwise, grouped by
name, lead shape chosen by `@PaletteDefault` or else fewest parameters, labels from `@PaletteLabel`, whole
name dropped if any overload is `@Hidden`. Reflection needs no build configuration, which is why it replaced
an annotation processor a plugin's pom could silently omit.

- **It degrades, never throws.** Two `@PaletteDefault`s on one name, a `@PaletteLabel` on a `@Hidden` member,
  a class with no `@Palette`, a facade whose members cannot be read (`LinkageError` from an optional
  dependency the host did not resolve) — each is collected into **`problems()`** and the rest of the catalog
  is built. **No malformed catalog may be the reason a project will not open.**
- **Member order is the class file's, not `getDeclaredMethods()`'s.** `SourceOrder` parses the constant pool
  and methods table to recover the author's ordering; every failure path returns an empty list and the
  caller sorts alphabetically. **Reflection promises no order** — where order matters to a human, recover the
  author's; where a thing is addressed by key, sort it.
- **Constructors are not catalogued**: a palette entry inserts a call. `MemberId` keeps `of(Constructor)` for
  a plugin that wants one.

**A project's structure belongs to the user; a plugin contributes methods a user calls.** No surface here
writes files into a user's source tree (the scaffold/seed surface that did was deleted on 2026-08-29), and a
file a plugin ships is copied once, from its template, and is the user's from then on.
