# Changelog

What each released version of `botmaker-studio-api` changes, in a few bullets. `ROADMAP.md` stays the
detailed engineering log; this is the short answer, and it is what `release.sh` publishes as the GitHub
Release body.

**`release.sh` refuses to cut a version with no section here** (`check_changelog`, decide pass, before
anything is tagged). If the top section still says `## [Unreleased]`, rename it to the version being cut and
date it.

Sections are `## [x.y.z] — YYYY-MM-DD`, newest first.

**This module's compatibility rule is stricter than any other in the repository, and every entry below should
be read against it:** a plugin's compiled `.class` files cannot be rewritten by anybody, so a change here
that an already-built plugin cannot survive is a **major** change, and one that only a Studio major release
is allowed to make. Additions arrive as `default` methods.

## [Unreleased]

**Release this as `0.2.0`.** Everything under *Removed* below came after `v0.1.6`, which was cut and
pushed on 2026-09-21 and still holds `ValueCodec`, `ValueType`, `ValueCatalog`, `SourceSeed`,
`PluginSource` and `ParameterGroup`. By this module's own rule above that is a breaking release, and in
`0.x` the minor digit is the breaking one. `botmaker.japicmp.baseline` is pinned to `v0.2.0` already, so
the gate reports the missing tag and passes until it exists — the same move `v0.1.6` used.

### Added

- **Recording is the host's, and a plugin annotates** (`com.botmaker.plugin.api.record`). `Gesture` is what
  the host recognises — clicks, a drag, the wheel, typing, a key, a combo, a pause, and a pause that ends on
  something nameable. `@Records(Gesture, rank)` on a public static method says that method writes the gesture
  down; the host finds it on the plugin's `@Palette` classes and fills its parameters by type.
  `RecordedValue<T>` answers a parameter type only the plugin understands (a picture under the pointer), and
  `StudioPlugin.recordedValues()` lists them.
- **`PluginType<T>` and `ComponentType<T>`** (`com.botmaker.plugin.api.value`), replacing four declarations
  per type with one class whose every method is abstract. `PluginType` is the class a plugin owns, a
  `fresh()` that returns a real `T` rather than a Java expression as text, and the editor for it;
  `ComponentType` sits beside it for a type whose Java is a call, and says what goes in the brackets as
  *values*. `build(components(v))` equals `v` is the law, and `botmaker plugin validate` checks it.
- **`StudioPlugin.types()`**, the one surface that replaces `valueTypes()` and `sourceSeeds()`.
- **`StudioPlugin.componentTypes()`**, for a `ComponentType` that is not also a `PluginType` — the parts of
  a value, never picked on their own. The SDK's `Flow` is written as calls to `Flow.activity`, `Flow.edge`,
  `Flow.preset` and `Flow.limits` inside `Flow.of`, and those four declarations had no way to reach the
  host, so a flow could not be decoded at all.
- **`ValueContext.value(Class<T>)` and `set(Object)`** — a value crosses as a value. Every plugin used to
  parse the Java source itself, which produced three numeric-literal strippers, two argument splitters and
  one string unescaper across two modules, none of them agreeing.
- **`PluginType.freshCall()`**, `default null`, for a type whose fresh form is a *call the bot
  re-evaluates* rather than a constant. The SDK's `MatchResult` starts as `Vision.lastMatch()` — the last
  match the bot actually found. Freezing it into a value changes what the declaration means, and calling
  `Vision.lastMatch()` to obtain one would run the vision stack inside a headless validator. It answers a
  `java.lang.reflect.Method` — public, static, no parameters, returning `type()` — and the host writes the
  call and its import, so no Java text crosses; `botmaker plugin validate` checks the shape.
  It is the one thing the deleted `SourceSeed` said that `fresh()` cannot, and answering it is what makes a
  type declarable without making it editable — `editor(ctx)` may answer `null`, and the host then shows the
  expression read-only exactly as it does for a type no plugin declares.

### Changed

- **An empty `StudioPlugin.catalog()` means "the host discovers the palette"**: the host catalogues every
  `@Palette` class in the plugin's own jar. A plugin no longer lists its palette classes; overriding
  `catalog()` remains the way to build one by hand.
- **`SlotRun` crosses values.** `elements()` answers `SlotRun.Element(value, source)` — the value the host
  read, `null` when it could not, and the source for display — `allowed()` answers values, and
  `replace(List<?>)` takes values, or an `Element` to keep as written. Every element was Java text the plugin
  split and wrote itself.
- **`SlotEditor.forType(Class, …)` and `SlotEditor.forCall(Class, int, …, String…)`**, which is the
  toolkit's `CallSites` moved onto the contract: *which slot an editor claims* is contract vocabulary, and
  the helper was predicate construction with no UI in it.
- **`@Param` (`…api.params`) and `@Managed` (`…api.managed`)**, moved from `botmaker-plugin-basics`. They
  sit on a **bot's** own declarations and were kept out only because this module was `provided` on the SDK
  and so absent from a bot's classpath; that scope is `compile` now. `PluginLoader` stays parent-first for
  `com.botmaker.plugin.api.**`, so a plugin still links against the host's copy.

### Changed

- **`@Param`'s `min` and `max` are `double`**, defaulting to negative and positive infinity. They were
  strings so a duration bound could be written `"30s"` and a codec would parse it — and no plugin parses
  anything now. `ParameterRow.bounds()` becomes `min()`/`max()`/`isBounded()` for the same reason.
- **`ParameterRow` carries the type as written** (`typeName()`), not a `ValueForm`. A form is how the host
  walks `Map<String, List<Point>>`; nothing outside the host ever walked one. A row's consumers use the
  spelling.
- **`StudioPlugin.catalog(String pinnedVersion)` → `catalog()`.** No implementation ever read the argument:
  the toolkit's base class memoised the answer ignoring it, and the one plugin in existence recorded its
  per-version curation ending on 2026-08-26.

### Removed

- **`ValueContext.setSource(String, Class<?>...)`, `SlotContext.enclosingCall()` and
  `SlotContext.replaceEnclosingCall(String, String...)`** — a plugin writing Java text, and reading the call
  around a slot as text to split. A value is written with `set(Object)`. The one user of the pair was the
  SDK's duration picker turning `Wait.time(x)` into `Wait.between(a, b)`, and that toggle went with them.
  What still crosses as text is `ValueContext.source()`, for showing what the host could not read, and
  nothing else: a fresh call is a `Method` (`PluginType.freshCall()`).
- **`ActionContext.insertAtCursor(String...)`** — Java as text from a plugin's recorder. Its one caller was
  the SDK's recorder, and recording is the host's now: the host writes the call from a `@Records` method.
- **`ValueCodec`, `ValueType`, `ValueCatalog`, `ValueForm`, `ValueContainer`, `HostContainers`, `Range`,
  `SourceSeed`, `StudioPlugin.valueTypes()`, `StudioPlugin.sourceSeeds()` and `ValueContext.form()`** — the
  whole codec and grammar layer. The codec half was dead: storage stopped being text when a parameter
  became a `@Param` field and a plugin's values became `@Managed` methods, so `parse`, `store` and
  `defaultWire` had no caller outside their own plumbing. What was still wired was wrong — a leaf
  round-tripped Java through wire text through `literal(parse(java))`, and a `java.awt.Color` parameter
  opened and closed with no edit came back rewritten. The grammar was never a plugin's to read and is
  `botmaker-studio`'s `com.botmaker.studio.plugin.grammar` now.

- **`StudioPlugin.pluginSources()` and `PluginSource`**, one day after they landed. A plugin handed the host
  a class's whole text and the host copied it into the project on every bind. What they were *for* survives
  untouched — a bot holds a plugin's values as `@Managed` methods in its own source, and the host rewrites
  nothing but the expression one returns — but a project gets that file from the template it was created
  from, and the skeleton the SDK was shipping shrank to two `@Managed` methods once
  `Bot.run(anchor, goHome, Sdk.class)` made a hand-written `install()` unnecessary. Two methods is not worth
  a contract surface, a copy on every `PluginHost.bind` and a `${package}` rewriter.
  **A plugin that wants to add its file to an existing project writes it from its own window**; nothing in
  the contract is needed for that, and nothing here replaces the removed pair.
  **A removal after `v0.1.6`**, which was cut and pushed on 2026-09-21 and still contains `PluginSource` —
  see the note on japicmp at the top of this section.

- **The whole parameter-data surface: `ParameterGroup`, `ParameterEdit`, `StudioPlugin.parameters(String)`,
  `parameterRows(String)` and `parameterEdited(ParameterEdit)`.** A plugin declared a section and the host
  asked it for that section's rows. **Nothing ever declared one** — the SDK's group was the only
  implementation in existence and it declared no rows, and `botmaker-plugin-basics`' `ParameterStore.declare`
  had no caller anywhere — so `parameterRows` answered out of a pre-2026-09-17 project's JSON and empty for
  every project created since. A second reader of a format nothing writes is the thing the umbrella
  `CLAUDE.md` forbids by name.
  What replaced it had already replaced it: a parameter is a `@Param` static field in the bot's own Java
  (2026-09-17), read and written off the syntax tree. **A plugin that wants a row of its own puts a `@Param`
  field in the file it ships**, and the host's ordinary walk of the bot's sources finds it — so the surface
  is not replaced by a smaller one, it is not needed.
  **`ParameterRow` stays**: it is still the shape one row crosses in, and the window still draws its value
  with the slot editor the canvas uses. It no longer refers to a group, and its category is documented as the
  free text it always was.
  **A removal after `v0.1.6`**, like the one above.

## [0.1.6] — 2026-09-21

### Changed

- **The contract has a package per contribution surface.** The root package held 22 types while `catalog`,
  `palette`, `value` and `meta` were tidy, so the root had become the place every new type landed. Sixteen
  moved; repoint an import with the table below and nothing else changes — no type, method or component was
  renamed, added or removed.

  | Type | Was | Is |
  |---|---|---|
  | `SlotEditor`, `SlotContext`, `SlotRun`, `ValueContext`, `TypeRef` | `com.botmaker.plugin.api` | `com.botmaker.plugin.api.slot` |
  | `ParameterGroup`, `ParameterRow`, `ParameterEdit` | `com.botmaker.plugin.api` | `com.botmaker.plugin.api.parameters` |
  | `ToolbarItem`, `ToolbarGroup`, `EnabledWhen`, `ActionContext` | `com.botmaker.plugin.api` | `com.botmaker.plugin.api.toolbar` |
  | `PluginSource`, `SourceSeed`, `ManagedValue`, `PluginValues` | `com.botmaker.plugin.api` | `com.botmaker.plugin.api.source` |

  `StudioPlugin`, `StudioServices`, `Dialogs`, `Theme`, `Runs` and `Sources` stay at the root: they are what
  `StudioServices` hands back, which a plugin reads as one facility rather than four surfaces. `catalog`,
  `palette`, `value` and `meta` are untouched.

- **This is a binary-incompatible break, taken deliberately and once.** A package move is a removal to
  japicmp and to a classloader alike, so a plugin compiled against v0.1.5 will not load against this one. It
  was allowed because the only implementors are the SDK and plugin-basics, both in this repository and both
  recompiled the same day; the window closes the day a third-party plugin exists.
  `docs/refactor/25-compatibility.md` §2 carries the dated reasoning. The gate was not weakened:
  `botmaker.japicmp.baseline` is pinned to `v0.1.6`, the tag this is released as, so the comparison resumes
  from the first build after it.

## [0.1.5] — 2026-09-21

No source changes since v0.1.4; re-released for updated upstream pins.

### Added

- **A `ValueContainer` may have arity zero — a fixed shape.** `arity()` had to be at least one, so a record
  with named components and no type arguments could only be registered as an opaque leaf, which is a string,
  which is what `ValueForm` exists to leave behind. A zero-arity container answers a fixed list from
  `partForms` and ignores the (empty) arguments; everything else works unchanged, because every other method
  already asked `partForms` rather than counting arguments. `ValueForm.Of.sourceName()` writes `Flow` rather
  than `Flow<>` for one, and `Of.last()` answers `null` instead of throwing. The SDK's `Flow`, `Activity`,
  `Edge` and `Limits` are the first four.

- **`ManagedValue(String id, String reason)` and `StudioPlugin.managedValues()`** — which of a plugin's
  values the code canvas may not edit, and the sentence to show when it refuses. A plugin declares the ids
  it answers to; the bot's own Java carries them on a `@Managed("id")` method or class
  (`com.botmaker.plugin.basics.managed.Managed`), and the host matches the two without knowing what either
  means. These are also the ids `PluginValues.open` will answer for.

- **`PluginValues`, off `StudioServices.pluginValues()`, and `PluginSource`** — a plugin's own values, kept
  as **compiled Java in the bot's own source** instead of JSON. The plugin ships one file
  (`StudioPlugin.pluginSources()`), the host copies it into the project once when the plugin is added, and
  from then on rewrites nothing but the expression a `@Managed` method returns. `ids()` and
  `open(String id)` are the whole capability: `open` hands back a `ValueContext`, which is the interface a
  slot on the canvas and a row of the Parameters window are already edited through, so a plugin's own window
  reads and writes its value with no second way to edit a value existing. A value the user has hand-edited
  into something that is not a single `return <expression>;` answers empty and is shown read-only with the
  reason. `default`, answering `PluginValues.NONE`, so a host with no source tree behind it — the `botmaker`
  CLI's validator — is unaffected. Design: `docs/refactor/33-plugin-java.md`.
- **`ValueContext.form()`, `source()` and `set(String, String...)`** — a value crosses as the **Java that
  writes it**, everywhere. A `ValueContext` spoke a `List<String>` wire form until now, which could say a
  leaf and a flat list of leaves and nothing else, so an editor could never be handed `Map<String,
  List<Point>>` even though a bot may declare such a field; and it was a second encoding of a value beside
  the Java one, kept in step by hand. `form()` is the whole `ValueForm` tree, so an editor may claim a
  composite rather than a leaf.
- **`ValueForm`** — the type of a value as a tree: a catalogued `Leaf`, an `Of` over a `ValueContainer`, or a
  `Declared` class the bot itself writes. Nested to any depth, so a field declared `Map<String, List<Point>>`
  is something the vocabulary can now say at all. It **replaces `ValueChoice` and `ValueShape`**, which are
  deleted below. `ValueForm.leaf()` is the one question that survived them: the single type a form's values
  are typed as, which is what a declared set of choices and a declared range are asked of, and `null` for a
  form with more than one. Design: `docs/refactor/32-generic-values.md`.
- **`ValueContainer<C>`** — a composite a value may be built out of, registered in a `ValueCatalog` exactly
  as a `ValueType` and its `ValueCodec` are. **A plugin may now contribute its own** — `Set`, `Optional`,
  an `Either<L, R>` — with no contract change. A container takes a composite apart into typed parts
  (`parts`) and puts it back together from typed parts (`build`), and declares the static factory's name;
  **the host owns every character of syntax**, written once for all containers as `Owner.factory(p₁, …, pₙ)`.
  Nothing about a container is a string function, which is the rule `docs/refactor/33-plugin-java.md` sets
  for the whole design. `arity()` counts *type* arguments and `parts` counts *values*, with `partForms`
  carrying the static type of each part so the recursion stays typed.
- **`ValueCatalog.initializer(ValueForm, Object)`, `valueOf(ValueForm, String)` and `imports(ValueForm)`** —
  one writer and one reader over the whole type tree, both total, both over **live values** rather than a
  wire encoding. A composite's canonical form is its Java initialiser and nothing else, so there is no second
  spelling to keep in step. The flat `initializer(ValueChoice, List<String>)` and `valueOfInitializer` now
  delegate to them and keep their signatures and their behaviour exactly, which is what pins the new
  implementation against the old tests.
- **Three containers seeded into every catalog** — `java.util.List` (`of`), `java.util.Map` (`ofEntries`)
  and `java.util.Map.Entry` (`entry`) — so a project with no plugin installed still has a list and a map.
  They are ordinary registrations with no privilege, which is what keeps the built-ins honest about the
  interface a plugin's container will use. **A map is always written `Map.ofEntries(Map.entry(k, v), …)`**,
  never `Map.of`: one spelling means one rule for the writer and one for the reader, and no behaviour change
  at the eleventh entry.

### Changed

- **`ParameterRow` carries a `ValueForm`, and its value is one source string.** `named(String, ValueForm)`
  is the only way a row is built and `form()` is what it holds. `value()` was a `List<String>` of wires — one entry for an ordinary row,
  one per item for a list-shaped one — and that list was only ever there to carry the *list*: a form says
  `Map<String, List<Duration>>`, which no flat list of wires can encode. It crosses as the Java initialiser
  a field of that form takes, which is what `ValueCatalog.initializer` writes and `valueOf` reads back.
  `ParameterEdit.value()` changes the same way and for the same reason, so one spelling crosses in both
  directions. `singleValue()` is gone from both. `32-generic-values.md` decision 6, breaking, taken while no
  third-party plugin exists.

- **`ValueCatalog.partsOfInitializer`, `initializerOfParts` and `itemOfLiteral`** — the grammar as an
  *editor* needs it. `valueOf` answers a composite as one live object, which a generator wants and a value
  cell cannot use: a cell edits one part at a time, and a part its codec refuses must be drawn as written
  rather than dropped from the answer. So a composite is taken apart one level at a time into parts that are
  themselves source, each with its own form, and composed back through the container's own factory. A blank
  part or a count the container cannot hold declines, exactly as everything else here declines rather than
  guesses. `itemOfLiteral` is `literal` read backwards for one leaf — the only place a second spelling of a
  value still exists, because a leaf's own control has text where a composite has none.

- **`ValueCatalog.containerForJava(String)` and `defaultValue(ValueForm)`** — the container a written type
  *name* means (the counterpart of `forJava(String)`, for a host reading a bot's source with no bindings),
  and the value a fresh field of a form starts with: the leaf's own default, or an **empty** composite. A
  container a user has not filled in has no parts, so seeding one item is not the default a list should get.

- **`ValueCodec.wireOfLiteral(String)` is replaced by `valueOfLiteral(String) → Optional<T>`, and it is
  abstract.** The old method answered the *stored text* a literal came from and was a `default` returning
  empty, so a type could be registered with no reader and nothing said so — nine of the seventeen shipped
  types implemented it and eight silently did not, which is a value the editor writes into a user's Java and
  thereafter refuses to edit. The new one answers the **value** directly, which removes the wire as a
  way-station now that a composite's canonical form is its Java initialiser; and it has no default, so the
  inverse is something the compiler asks for at the one moment the author has the literal in front of them.
  Declining a *particular* source is unchanged and still the honest answer for an initialiser a plugin never
  emits. This is a breaking change to the contract, taken while no third-party plugin exists.

- **`StudioPlugin.managedFields()` and `ManagedField`** — a plugin says which `static final` constants it
  keeps in step through its own window (its type, and the sentence to show instead). The host draws them,
  gives their value the plugin's `SlotEditor.preview`, and refuses a canvas edit; a class holding nothing but
  such constants is refused whole. It is what lets a picture class be read-only without the host knowing what
  a picture is. A `default` returning nothing, so an older plugin manages nothing and every field stays as
  editable as it was.

### Removed

- **`ManagedField` and `StudioPlugin.managedFields()`**, replaced by `ManagedValue` above. A managed field
  claimed every `static final` field whose declared *type* matched, which cannot tell two same-typed classes
  apart, and the host inferred from it that a class of nothing but such constants was managed whole. Both
  were guesses about shape: a bot keeping one picture beside ordinary code was locked out of it, and a
  second class of pictures could not be told from the first. An annotation is a statement.

- **`ValueChoice` and `ValueShape`.** A choice was a `ValueType` plus a four-constant shape, and it could
  not say `Map<String, Duration>`, `List<List<Point>>` or anything else with two type arguments or two
  levels — a field javac accepts perfectly well, read as unknown and refused. The shape also answered two
  unrelated questions at once, *how many* and *out of what set*, and the second was never a property of a
  type: it is what the declaration wrote down, which is `ParameterRow.options()` and has been all along.
  `ValueForm` says the first and nothing says the second twice. `ParameterRow.type()` and
  `named(String, ValueChoice)` go with them, as does `ValueForm.asChoice(boolean)`, the one-directional
  bridge that existed so callers could move across one at a time.

- **`ValueCatalog.imports(ValueChoice)`**, replaced by `imports(ValueForm)`, which walks the whole tree.
  The flat writer and reader are **renamed rather than deleted**: `initializer(ValueChoice, List<String>)`
  and `valueOfInitializer(ValueChoice, String)` are now `initializerOfWires(ValueForm, List<String>)` and
  `wiresOfInitializer(ValueForm, String)`. They compose nothing of their own — each delegates to
  `initializer`/`valueOf` — and exist for the one kind of caller that still holds a value as one string per
  item, a plugin whose own JSON file keeps it that way. They are named for the wire so that nothing reaches
  for them by accident, and they go when those files do.

- **`ValueContext.value()` and `set(List<String>)`**, replaced by `source()` and
  `set(String, String...)` above. With them go **`SlotContext.currentSource()`, `replaceWith`,
  `enclosingClass()`, `enclosingMethod()` and `enclosingSource()`** — the first two are now what every value
  answers rather than only a slot, and the last three are the nullable halves of
  `enclosingClassName()`/`enclosingMethodName()`/`enclosingCall()`, which become abstract.
  **`SlotContext.asSlot()`, `run()`, `SlotRun.allowed()`, `ActionContext.projectName()` and
  `Dialogs.owner()`** go the same way: each was a nullable member kept beside the `Optional` sibling that
  replaced it, and a deprecated member in an unreleased API is a second spelling nobody has yet written
  against.

- **`Models`, `StudioServices.models()`, `StudioPlugin.modelCalls()` and the whole
  `com.botmaker.plugin.api.model` package** — `ModelCall`, `ModelStatement`, `MethodRef`. Shipped earlier the
  same day and **withdrawn before any host implemented it**: a model as a sequence of statements made the
  host the author of a compilation unit, which forces it to own the package, the class name, the imports and
  the ordering. `PluginValues` above is the replacement, and the difference is that the plugin writes the
  file and the host rewrites one expression inside it.

  Breaking, like everything above, and taken while no third-party plugin exists.

## [0.1.4] — 2026-09-19

### Added

- **`StudioPlugin.managedFields()` and `ManagedField`** — a plugin says which `static final` constants it
  keeps in step through its own window (its type, and the sentence to show instead). The host draws them,
  gives their value the plugin's `SlotEditor.preview`, and refuses a canvas edit; a class holding nothing but
  such constants is refused whole. It is what lets a picture class be read-only without the host knowing what
  a picture is. A `default` returning nothing, so an older plugin manages nothing and every field stays as
  editable as it was.

## [0.1.3] — 2026-09-19

No source changes since v0.1.2; re-released for updated upstream pins.

No source changes since v0.1.1; re-released for updated upstream pins.

### Added

- **`ValueCodec.wireOfLiteral(String)`**, `default` and declining by default — `literal` read backwards.
  A user parameter is a field in the bot's own Java now (`@Param` in `botmaker-plugin-basics`), so its
  value *is* that field's initialiser: an editor that can write one but not read one can only offer to
  overwrite. `literal` writes the parsed value structurally (`java.time.Duration.ofMillis(3000L)`,
  `new java.awt.Color(255, 0, 0)`) so a bot cannot throw while starting, and nothing but the codec can undo
  that spelling. Empty means *not recognised*, and the host then shows the source read-only rather than
  guessing — which is also the right answer for a hand-written initialiser no plugin would emit.
  The round trip is the contract: `wireOfLiteral(literal(parse(wire)))` equals `store(parse(wire))`.
- **`ValueCatalog.valueOfInitializer(ValueChoice, String)`** — the shape-aware composition of it, mirroring
  `initializer` in the other direction. A `java.util.List.of(…)` source answers one wire per item, and an
  item the codec declines makes the whole answer empty: half a list is not a value.

### Removed

- **`StudioPlugin.parameterDeclared(ParameterDeclaration)` and the `ParameterDeclaration` record.** They
  shipped on 2026-09-10 and are gone eight days later, for the reason they existed: a user parameter is not
  a row a plugin stores any more, it is a `@Param` static field in the bot's own Java, and the host adds,
  renames, retypes, refiles and removes one by editing the syntax tree. A plugin's rows are the plugin's own
  — an activity's enable flag, a capture target — and a plugin declares those in its own code, where a wire
  form for *here is the row I want* buys nothing. `parameterRows(String)`, `parameterEdited(ParameterEdit)`,
  `ParameterRow`, `ParameterEdit` and `ParameterGroup` are unchanged: read the rows, change a value.
- **This is a breaking change**, and it is allowed here only because there are no third-party plugins yet
  (umbrella `CLAUDE.md`, *Compatibility §2 is suspended*). A plugin that overrode the method fails to
  compile, which is the honest outcome — a `default` kept as a courtesy would have been a surface the host
  no longer calls, and a plugin writing user parameters nobody reads.

## [0.1.2] — 2026-09-18

No source changes since v0.1.1; re-released for updated upstream pins.

### Added

- **`ValueCodec.wireOfLiteral(String)`**, `default` and declining by default — `literal` read backwards.
  A user parameter is a field in the bot's own Java now (`@Param` in `botmaker-plugin-basics`), so its
  value *is* that field's initialiser: an editor that can write one but not read one can only offer to
  overwrite. `literal` writes the parsed value structurally (`java.time.Duration.ofMillis(3000L)`,
  `new java.awt.Color(255, 0, 0)`) so a bot cannot throw while starting, and nothing but the codec can undo
  that spelling. Empty means *not recognised*, and the host then shows the source read-only rather than
  guessing — which is also the right answer for a hand-written initialiser no plugin would emit.
  The round trip is the contract: `wireOfLiteral(literal(parse(wire)))` equals `store(parse(wire))`.
- **`ValueCatalog.valueOfInitializer(ValueChoice, String)`** — the shape-aware composition of it, mirroring
  `initializer` in the other direction. A `java.util.List.of(…)` source answers one wire per item, and an
  item the codec declines makes the whole answer empty: half a list is not a value.

### Removed

- **`StudioPlugin.parameterDeclared(ParameterDeclaration)` and the `ParameterDeclaration` record.** They
  shipped on 2026-09-10 and are gone eight days later, for the reason they existed: a user parameter is not
  a row a plugin stores any more, it is a `@Param` static field in the bot's own Java, and the host adds,
  renames, retypes, refiles and removes one by editing the syntax tree. A plugin's rows are the plugin's own
  — an activity's enable flag, a capture target — and a plugin declares those in its own code, where a wire
  form for *here is the row I want* buys nothing. `parameterRows(String)`, `parameterEdited(ParameterEdit)`,
  `ParameterRow`, `ParameterEdit` and `ParameterGroup` are unchanged: read the rows, change a value.
- **This is a breaking change**, and it is allowed here only because there are no third-party plugins yet
  (umbrella `CLAUDE.md`, *Compatibility §2 is suspended*). A plugin that overrode the method fails to
  compile, which is the honest outcome — a `default` kept as a courtesy would have been a surface the host
  no longer calls, and a plugin writing user parameters nobody reads.

## [0.1.1] — 2026-09-17

### Added

- **`ValueCodec.wireOfLiteral(String)`**, `default` and declining by default — `literal` read backwards.
  A user parameter is a field in the bot's own Java now (`@Param` in `botmaker-plugin-basics`), so its
  value *is* that field's initialiser: an editor that can write one but not read one can only offer to
  overwrite. `literal` writes the parsed value structurally (`java.time.Duration.ofMillis(3000L)`,
  `new java.awt.Color(255, 0, 0)`) so a bot cannot throw while starting, and nothing but the codec can undo
  that spelling. Empty means *not recognised*, and the host then shows the source read-only rather than
  guessing — which is also the right answer for a hand-written initialiser no plugin would emit.
  The round trip is the contract: `wireOfLiteral(literal(parse(wire)))` equals `store(parse(wire))`.
- **`ValueCatalog.valueOfInitializer(ValueChoice, String)`** — the shape-aware composition of it, mirroring
  `initializer` in the other direction. A `java.util.List.of(…)` source answers one wire per item, and an
  item the codec declines makes the whole answer empty: half a list is not a value.

### Removed

- **`StudioPlugin.parameterDeclared(ParameterDeclaration)` and the `ParameterDeclaration` record.** They
  shipped on 2026-09-10 and are gone eight days later, for the reason they existed: a user parameter is not
  a row a plugin stores any more, it is a `@Param` static field in the bot's own Java, and the host adds,
  renames, retypes, refiles and removes one by editing the syntax tree. A plugin's rows are the plugin's own
  — an activity's enable flag, a capture target — and a plugin declares those in its own code, where a wire
  form for *here is the row I want* buys nothing. `parameterRows(String)`, `parameterEdited(ParameterEdit)`,
  `ParameterRow`, `ParameterEdit` and `ParameterGroup` are unchanged: read the rows, change a value.
- **This is a breaking change**, and it is allowed here only because there are no third-party plugins yet
  (umbrella `CLAUDE.md`, *Compatibility §2 is suspended*). A plugin that overrode the method fails to
  compile, which is the honest outcome — a `default` kept as a courtesy would have been a surface the host
  no longer calls, and a plugin writing user parameters nobody reads.

## [0.1.0] — 2026-09-16

### Added

- `ToolbarGroup.OVERLAY` — the overlay editor's own item row. A plugin contributes to it through the
  `toolbarItems()` it already has; the host filters by group, so there is no second surface.
- `ActionContext.overWindowTitle()`, `overBounds()` and `insertAtCursor(String...)`, all `default`. The only
  host facts an overlay action cannot get for itself: which window the HUD is drawn over, where it is, and the
  editor's insertion cursor. `insertAtCursor` closes the loss recorded when the macro recorder became a
  plugin — recorded actions could no longer land at the overlay's cursor.
- `ActionContext.Area` — four ints, for `overBounds()`.

- **The declaration half — `StudioPlugin.parameterDeclared(ParameterDeclaration)`.** The host says *here is
  the row I want under this name*, and the owner answers with the row as it stored it. That one call is
  adding a parameter, deleting one, renaming it, retyping it, changing its declared choices, its range, its
  category, its note or who it is offered to — nine things a window does, and no verb the contract has to
  learn. A verb would have made this module know what retyping means, which is a rule about a plugin's own
  value types; a `kind` enum would have frozen today's list of verbs. `ParameterDeclaration` is a record the
  **host** constructs, so it may grow a component, exactly like `ParameterEdit` and `Sources.Use`.
  `Optional.empty()` covers a removal, a group the plugin does not own and a refusal alike — the host redraws
  the section from `parameterRows` afterwards either way.

- **Parameter data — `StudioPlugin.parameterRows(String)` and `StudioPlugin.parameterEdited(ParameterEdit)`,
  with `ParameterRow` and `ParameterEdit`.** The seventh contribution surface, and the first where a plugin
  hands over *project data* rather than something it decided at build time. `ParameterGroup` already said
  that a plugin declares the section and a user declares the values in it; what was missing was a way for
  the values to reach the window without the host parsing one plugin's file. Both methods are `default` and
  answer nothing, so a plugin compiled against an earlier release contributes no rows and is asked for
  nothing.
- **`StudioPlugin.projectOpened(StudioServices)`** — the mirror of `projectClosing()`, and what makes the
  data surfaces answerable. `parameterRows` takes a group id and nothing else, deliberately: rows are asked
  for every time a window is drawn, and a surface handed the host on every call would make every future one
  take it too. So the host says *here is the project* once per bind, and the plugin reads its own file from
  `projectDir()`/`resourcesDir()` when it is asked. A capability rather than a surface, for the reason
  `projectClosing()` already gives from the other end: a plugin is constructed once and then serves whatever
  is bound to it, so which project it has is the one fact it cannot establish for itself. `default`, and
  nothing expensive belongs in it — a project open must not pay for a window nobody has looked at.
- **`ParameterRow`** — one row as its owner hands it over: a name, a `ValueChoice`, the stored
  `List<String>`, a description, a category out of the group's declared set, a `Visibility`, the declared
  options and a `Range`. Every component is vocabulary this module already owns, the value is text exactly
  as `ValueCodec` stores it, and no `Class<?>` crosses. **A final class with a builder, not a record**: a
  plugin constructs it, so a component added later would throw `NoSuchMethodError` in every plugin already
  compiled (compatibility trap #2).
- **`ParameterEdit`** — the group, the name and the new stored text, on the way back. **A record, and it may
  grow**: the *host* constructs it and a plugin only reads it, which is the same call `Use` makes.
  `parameterEdited` answers the row as stored, or `Optional.empty()` for a row it does not own — one return
  type that carries a clamp, a normalisation and a refusal without the host modelling any of them.

- **`ValueCatalog.forJava(Class<?>)` — a type is asked for by the Java class it is.** `forJava(Duration.class)`
  answers the `DURATION` registration; `forJava(String)` does the same from a type name. The id stays the
  persisted identity and stays what a project file holds; what changes is that nobody outside the plugin
  registering a type has to write it down. A wrapper finds its primitive, so `Integer.class` and `int.class`
  are one type — which is how a list of them is asked for. The class is read and never loaded: only its names
  are taken off the object the caller already holds, because two classloaders make a `Class` comparison
  meaningless.
- **`ValueType.javaName()`** — the import when there is one, the source spelling otherwise. What the index
  above is keyed on.
- **`ValueCatalog.javaClashesWith(ValueCatalog)`** — the Java types two plugins both claim, reported the way
  `clashesWith` reports two claims on one id. Merging still never throws and drops no registration: the loser
  keeps its id, so a project that stored values of it still reads them.

### Changed

- **`ValueCatalog.Builder.add` refuses two types claiming one Java type**, exactly as it already refuses one
  id registered twice. It was true of the SDK's seventeen types by accident rather than by construction, and
  an index built on an accident answers whichever registration happened to come first.

- **An `Optional` sibling for every nullable member a plugin receives**, and the old member is deprecated
  with a `@ReplacedBy` pointing at it. Nothing changes behaviour and the host still implements the original
  in every case — the siblings are `default` views over them, so an older host and a newer one answer the
  same thing.

  | was | now | `null` meant |
  |---|---|---|
  | `SlotRun.allowed()` | `allowedSources()` | **anything goes** — while `List.of()` means *nothing allowed* |
  | `ValueContext.asSlot()` | `slot()` | a Parameters row, not a slot |
  | `SlotContext.run()` | `siblingRun()` | the slot stands alone |
  | `SlotContext.enclosingSource()` | `enclosingCall()` | not an argument of a call |
  | `SlotContext.enclosingClass()` | `enclosingClassName()` | the host could not resolve the call |
  | `SlotContext.enclosingMethod()` | `enclosingMethodName()` | as above |
  | `ActionContext.projectName()` | `openProjectName()` | no project is open |
  | `Dialogs.owner()` | `ownerWindow()` | the editor is not yet in a scene |

  **`allowed()` is the one that earned the change.** Two opposite absences shared one `List` type there and
  the javadoc named the `null` as the ordinary case, so `for (String s : run.allowed())` — the shape an
  author writes without thinking — throws on nearly every slot, and the one shape that does *not* throw is
  the one that must offer nothing. An `Optional` cannot be iterated by accident.

  Every other entry is the same hazard at lower stakes: each `null` is a state that never occurs while the
  editor is being written (against a slot, in a resolved call, with a project open, attached to a scene) and
  is ordinary afterwards.

- `OptionalSiblingsTest` holds the delegation. The interesting half of each case is the **present** one — a
  broken delegation is silent, and shows up as an editor that never sees a value the host did supply.

### Changed

- `ValueContext.single()`'s javadoc says why it null-checks `value()`, which the same interface documents as
  never null: this method's whole promise is that it is total, so it must not be where a broken host
  implementation surfaces as a `NullPointerException` inside somebody's editor. `value()`'s rule is
  unchanged.

### Removed

- **`com.botmaker.plugin.api.authoring` is gone — 880 lines, 20% of this module.** `ProjectModel`,
  `ActivityModel`, `VariableModel`, `FlowModel`, `FlowNodeModel`, `FlowEdgeModel` and `PresetModel` are
  `com.botmaker.sdk.authoring` again, which is where they were until 2026-08-31. `ProjectModel`'s first line
  is *"This is `activities.json`, as a value"* — the SDK's file. A plugin that is not the SDK has no
  activities, no flow and no presets, and could never construct one meaningfully; they were plugin #1's
  concepts wearing the contract's name, which is what `Assets` and `SourceChoice` were deleted for on
  2026-08-27. The commit that brought them here also added `jackson-annotations` to this pom, the one
  dependency the module must not have; that was pulled back out within the day and the records stayed.
- **It also removes a trap in every bot ever built.** `ProjectData` runs inside a running bot and read four
  constants off `FlowModel` and `FlowEdgeModel` — and **this module is not on a bot's classpath**, because
  the SDK declares it `provided`. It did not crash only because all four are `public static final`
  `int`/`String` with literal initialisers, which javac copies into the calling class file (JLS §13.1);
  disassembly confirmed `ProjectData.class` held zero references to `plugin/api/authoring`. Changing any one
  of them to a computed value would have given every bot a `NoClassDefFoundError`. Five of the seven records
  name no contract type at all, so they link in a bot cleanly now.

- **`Region` is no longer in the contract.** It moved to `com.botmaker.plugin.toolkit.Region`, unchanged. It
  arrived here for `Capture`, a host capability deleted on 2026-08-31; with that producer gone **no contract
  signature takes or returns it**, and its only remaining producer is the toolkit's own `ScreenPicks`. A
  record that travels only between a plugin and the widget kit does not belong in the artifact both sides
  must agree on for ever. A plugin using it changes one import.
- **`Category`'s eight constants** — `VISION`, `INTERACTION`, `CAPTURE`, `LAUNCH`, `EMULATOR`, `GEOMETRY`,
  `BOT`, `UTIL`. Nothing could reference them: `@Palette(category = …)` takes a `String`, so a facade names
  its group as text. They were the default plugin's menu sitting in the contract. The record and both `of`
  overloads are untouched; a plugin joining an existing group writes the id, which is what the annotation
  makes it write anyway.

## [0.0.4] — 2026-09-04

### Fixed

- **The pin `0.0.3` added was itself unbuildable on JitPack**, which is why that tag is missing too. It
  named `maven-compiler-plugin` **3.13.0**, and the plugin raised its own Maven prerequisite from 3.2.5 to
  3.6.3 in 3.12.0 — JitPack's Maven is older than that, so the build stopped one line further down than
  before:

  ```
  [ERROR] The plugin org.apache.maven.plugins:maven-compiler-plugin:3.13.0 requires Maven version 3.6.3
  ```

  Pinned to **3.11.0**, the newest version that builds there and the one `botmaker-shared` — the module
  that never broke — has always used. Use `0.0.4`; `0.0.3` was never published.

## [0.0.3] — 2026-09-04

### Fixed

- **This module is resolvable from JitPack again.** `v0.0.2` was published and could not be built: the pom
  did not pin `maven-compiler-plugin`, and JitPack's Maven defaults it to **3.1** — a version that predates
  `maven.compiler.release` (added in 3.6), ignores it, and falls back to `source 5`:

  ```
  [ERROR] Source option 5 is no longer supported. Use 8 or later.
  ```

  It broke on 2026-09-02, the day the platform moved to Java 25, and nothing downstream of the contract
  could resolve it for two days — the toolkit, `botmaker-plugin-host`, `botmaker-cli` and the SDK all
  failed with `Could not find artifact com.github.LiQiyeDev:botmaker-studio-api`. Nothing on this side
  changed: the artifact this tag publishes is what `v0.0.2` was meant to be.

## [0.0.2] — 2026-09-02

### Changed

- **Compiled for Java 25 (LTS), against JavaFX 25.0.4.** A plugin compiling against this contract needs a
  JDK 25 or newer; a host loading one needs a 25 runtime. This is a floor rather than a feature — nothing in
  the contract changed shape — but it is the kind of floor that produces `UnsupportedClassVersionError` at
  load rather than a compile error, so it is worth reading before upgrading. The poms say
  `maven.compiler.release` now instead of `source`/`target`, which is what makes the platform API checked
  rather than merely the bytecode level.

## [0.0.1] — 2026-09-02

First release. **`0.x` on purpose**: the contract is still in development, there are no third-party plugins,
and the one implementor — the SDK — is rebuilt from the same reactor on every change. The never-grow-a-record
rule that protects already-compiled plugins is suspended until a second plugin exists, and a `1.0.0` would
claim a stability this does not yet have.

### The contract

- **`StudioPlugin`** — what a plugin implements. **Every method but `id()` is `default`**, which is the whole
  versioning strategy of the module: a plugin built against an older contract keeps loading, and a host
  meeting an older plugin gets the declined value rather than an `AbstractMethodError`.

  Eight surfaces: `id`, `displayName`, `catalog(pinnedVersion)`, `slotEditors`, `sourceSeeds`, `valueTypes`,
  `parameters(pinnedVersion)`, `toolbarItems`, and the lifecycle callback `projectClosing`.

- **`StudioServices`** — what the host offers back: `projectDir`, `resourcesDir`, `theme`, `dialogs`, and the
  three `default` ones — `runs()`, `sources()` and `status(String)`.

  **The rule for what may ever go here is that the host must be the *only possible* source of it.** Not that
  the host happened to write it first, and not that a real editor needed it — that was the earlier test, and
  it is what let the contract grow a vocabulary belonging to one plugin. An `Assets` service and a
  `Capture.SourceChoice`/`Frame`/`Sample` family were built during development and **deleted before this
  release**: they named one plugin's concepts (a *named picture*, a *capture source*) in the contract's own
  package, which no second plugin could have done. Everything a plugin might otherwise ask the host for it
  can do itself, because `botmaker-shared` is published and any plugin may depend on it — enumerating
  monitors, windows and emulator instances, grabbing their pixels, reading an installed game library. **The
  contract grows capabilities, never vocabularies.**

### The palette

- **`PaletteCatalog`, built by reflection** — `PaletteCatalog.of(Mouse.class, Keyboard.class, …)`. One class
  literal per facade, so the class list is javac-checked; the members are discovered from the type's own
  `getDeclaredMethods()`. `FacadeEntry`, `MemberEntry`, `MemberId`, `Category` and `SourceOrder` describe
  the result.
- **Curation is opt-out and its unit is the member *name*, not the overload** — every public declared method
  is offered, as one menu entry per name with a lead shape and a submenu. The exceptions travel with the
  member they annotate, in `com.botmaker.plugin.api.palette`: **`@Palette`** on the type (this type is
  catalogued), **`@Hidden`** on a type or member (not offered), **`@PaletteLabel`** and **`@PaletteDefault`**.
  All four are `RUNTIME`, because the plugin reflects on them itself.
- **Two bits, not three states.** `@Palette` says *catalogued* — the recognition set, which answers imports
  and *does `Point` mean this plugin's or `java.awt`'s*. `@Hidden` says *not offered*. A value type being
  recognised but never proposed is the ordinary case, and it needs no third state to express.
- **A malformed catalog is collected, never thrown.** `PaletteCatalog.problems()` reports a bad category, two
  leads on one name or a label on a hidden member. `ValueCatalog.merge` is the precedent and the rule behind
  both is the same: **no malformed catalog may be why a project will not open.**
- **Present means curated.** A type in a catalog offers exactly the members it lists; a type absent from it is
  not offered. An entry with an empty member list is a verdict, not an omission.

### The value vocabulary — open, and the contract's rather than any plugin's

- **`com.botmaker.plugin.api.value`**: `ValueType`, `ValueShape`, `ValueChoice`, `Visibility`, `Range`,
  `ValueCodec<T>` and `ValueCatalog`.
- **`ValueType` is not an enum.** Its identity is the persisted `id()`; it is built through `ValueType.of(id)`
  and registered in a `ValueCatalog`. A closed enum is right for one plugin and wrong for two — a plugin
  wanting a `Channel` variable would need a constant granted in somebody else's enum, which is the back door
  the platform exists to close. **Never compare a `ValueType` by object identity**: two plugin classloaders
  make that meaningless, and the id is what the file holds anyway.
- **`ValueType.unknown(id)` is what makes an open vocabulary safe.** An id nothing registered keeps its raw
  text, renders read-only and declines to emit. That state was unreachable while the set was closed; it is
  the ordinary state of a project opened without one of its plugins.
- **`ValueCodec<T>` is per *item*, not per value** — `parse(String)` / `store(T)` / `literal(T)`. Shape is
  composed above it by `ValueCatalog.initializer`, so one codec serves all four shapes without knowing they
  exist, and `T` never crosses to the host: only `literal(parse(wire))`, behind a wildcard capture. **The
  host never loads a plugin's value class.**
- **`ValueCatalog.types()` answers in registration order**, and a merge appends rather than reshuffling, so
  installing a second plugin does not reorder the first's types. (It held a `Map.copyOf` during development,
  whose iteration order is unspecified *and randomised per JVM run* — every "what type is this variable"
  dropdown came out different each time the host started.)
- **No JSON annotations, deliberately.** The contract declares the wire *form*; whoever owns the file supplies
  the parser. Adding a JSON library here would impose it on every plugin.

### Editing a value

- **`SlotEditor`** — `matches(TypeRef)` / `create(SlotContext)`, plus `preview(ValueContext)` (`default null`)
  for the one place the host *shows* a value without editing it: beside a declared choice, in the list an
  author picks from. Plain text is wrong there whenever the stored string is a reference rather than the
  value — a template name is not a picture.
- **`SlotContext`** carries `enclosingClass()`, `enclosingMethod()` and `argIndex()`, so an editor can be
  chosen by the **call** rather than by the type — the only way to tell a Steam app id from a window title
  when both are `String`.
- **`SlotContext.enclosingSource()` and `replaceEnclosingCall(String, String...)`** — an editor may rewrite the
  whole call its slot sits in, not only the slot, for when the choice is a **shape** rather than a value:
  *"wait somewhere between 800ms and 2s"* is not a different duration from *"2s"*, it is a two-argument call
  where there was a one-argument one. Source text in, source text out, exactly like `replaceWith`.
- **`SlotRun`, reached through `SlotContext.run()`** — several sibling slots edited as one, for a value the
  author writes as a *run* of arguments (three pictures to match any of). `elements()` are the run's Java
  expressions in order; `replace(List<String>, String...)` writes the whole run, because an editor confined
  to one argument can change an element and never add or remove one. `minimum()` and `allowed()` are the
  host's two narrowings — how few elements the surrounding code still compiles with, and the only element
  sources it will still accept — and **both are opaque Java source**: the host says these arguments are one
  list and what the code around them permits, and the plugin, which knows what the strings mean, decodes
  them itself. `null` for a slot that stands alone, which is nearly every slot.
- **`ValueContext`** is the same value seen from the Parameters window, where there is no call and no syntax
  tree. One editor serves both.

### Rewriting the user's Java

- **`Sources`, reached through `StudioServices.sources()`.** `find(List<String>)` answers where a needle
  occurs; `replace(Map, historyLabel, reviewNote)` rewrites it in the open buffer *and* on disk and says which
  files changed. `Sources.NONE` is the total no-op a host with no project answers with, so a plugin never
  null-checks and never asks whether rewriting is supported.
- **Needles are matched as tokens, never as text**: `Templates.ORE` matches `Templates . ORE` and does not
  match `Templates.OREX` or `MyTemplates.ORE`; `"images/ore.png"` matches that whole string literal and
  nothing inside a longer one. **Tokens rather than a regex on purpose** — a regex would hand every plugin the
  power to corrupt a user's source with a bad pattern, and would pin one flavour of regex semantics into a
  surface only a major release may break. **Text rather than an AST, equally on purpose**: the file that most
  needs a rename to reach it is the one the user has open and half-edited, which does not parse.
- It passes the host-only test on every part — the open buffers are editor state, the walk knows which files
  the bot owns, and the review mark and the history snapshot are the host's own undo model — while what to
  search for stays entirely with whoever owns the thing being renamed. **`Use` is host-constructed only**, so
  it may grow a component later without the `NoSuchMethodError` every already-compiled plugin would take from
  a changed canonical constructor; for the same reason replacements are a `Map<String,String>` rather than a
  record a plugin would call a constructor on.
- **`SourceSeed`** — the starting expression for a type the host is asked to write down and knows nothing
  about, so the host carries no arm per plugin type.

### The toolbar — the one surface where a plugin contributes data

- **`StudioPlugin.toolbarItems()`**, with `ToolbarItem`, `ToolbarGroup`, `EnabledWhen` and `ActionContext`.
  **The plugin contributes data and the host builds the node**, deliberately unlike `SlotEditor`, which hands
  back a `Node`. The difference is expressiveness, not consistency: a bespoke image picker cannot be described
  as data and a button can, and describing it as data is what lets the host keep what a *shared* bar has to
  own — grouping, ordering, packing, the overflow menu, the icon box and the theme. Two plugins returning
  nodes would produce a bar with two button heights.
- **The label and the icon are `Supplier`s**, because the host's own bar already had two buttons that relabel
  from project state and a third that resolves a game's real title and cover art on a background thread. A
  record of `String` would have described a toolbar nobody has. The obligation is stated where it will be
  read: cheap and pure, called during layout.
- **`ToolbarGroup` is a closed enum the host owns**, and `ToolbarGroup.STUDIO` is **refused** with the plugin
  named — quietly re-homing it would put a plugin's button where a user reads the application rather than
  their project. A plugin picks a group and an order within it; it cannot open a section, because a bar whose
  shape depends on install order is a bar nobody can predict.
- **`EnabledWhen` is a closed set, not a `BooleanSupplier`** — four states the host already broadcasts, so
  enablement is a switch rather than somebody else's code run once per item per plugin inside a layout pass.

### Parameters, the project model, and the lifecycle

- **`ParameterGroup`** — a plugin declares the sections of the Parameters window it owns fields of, and the
  **categories** those fields may be filed under. The host had been deriving its own rail headings by reading
  two files one plugin owns, which is the mirror image of the vocabulary leak: not the host spelling a
  plugin's types, but the host reading a plugin's data to decide its own UI.
- **`com.botmaker.plugin.api.authoring`** — `ProjectModel`, `ActivityModel`, `VariableModel`, `FlowModel`,
  `FlowNodeModel`, `FlowEdgeModel`, `PresetModel`: the shape of a project a plugin may be handed.
- **`StudioPlugin.projectClosing()`** — the open project is closing; release anything held on its behalf.
  Called once per bind, on the plugins that were serving the project being left and **before** their
  classloader is closed, so a plugin can still run its own code. It is not a contribution surface — it
  contributes nothing — and it is the one thing a plugin cannot find out for itself: that the project it
  opened a port, a nested display or a child process for is gone. Anything releasable by garbage collection
  needs no implementation. The instance is reused across projects, so this says *this project is over*, never
  *you are being discarded*.
- **`Runs`, reached through `StudioServices.runs()`** — the open project's bot as a running process: `start`,
  `stop`, `isRunning`, `pid`, and listeners for run state and for telemetry. Host-only for the plainest
  reason on the interface: the host compiled the project, holds its resolved classpath and owns the process.
  **Telemetry crosses as one encoded frame, not a decoded shape** — the format belongs to whichever runtime
  the bot is built on, and that runtime decodes its own wire, exactly as `SlotContext` passes Java source
  rather than a syntax tree. Bytes rather than text because the wire already is bytes and has one definition;
  a text rendering invented for the contract would be owned by neither end.

### Compatibility

- **`com.botmaker.plugin.api.meta.@ReplacedBy`** — the one redirect annotation, and it is the contract's
  rather than the SDK's because it says how *any* library keeps faith with the code that calls it. Its value
  is `fqn`, `fqn#member` or `fqn#<init>`, and **`{}` means "nothing takes my place"** — an explicit statement
  rather than an omission. It also carries `note()` (the author's own sentence, shown verbatim), a
  `behaviourChanged()` flag for the one break a host cannot see by construction, and a parallel `whens()` so a
  member that became **two** is expressible: which candidate a call meant is a property of *that call*.
- `@Retention(CLASS)`, read out of a jar the host never loads, and with no business in a running bot's
  reflection data. (The four palette annotations are `RUNTIME` for the opposite reason.)

### Deliberately absent

- **Any dependency but `javafx-controls`**, at a literal version, at `provided` scope, because a slot editor
  returns a `javafx.scene.Node`. **No BotMaker upstream at all** — which is the only shape in which this
  module can version more slowly than the SDK, and is why it needs neither a flatten nor a `.deps.env`. If a
  BotMaker dependency ever appears here, that is the thing to refuse.
- **No parsing library.** A plugin writes back Java source as text.
- **A panel or view surface.** Out of scope, and recorded as such in `docs/refactor/24-plugin-platform.md`.
- **A scaffold surface.** `com.botmaker.plugin.api.scaffold` existed during development and was deleted
  before this release: a plugin no longer contributes files to a user's project at all. **A project's
  structure belongs to the user, and a plugin contributes methods a user calls.** A file a plugin owns inside
  somebody's source tree is a file its user cannot freely edit, rename or delete, and the ledger, reconciler
  and rename engine that kept such a file owned were all cost paid to work around that.
- **A `SwitchHandler`, or anything else taking a JDT type.** Declined: the contract needs no JDT, and the
  host's AST never becomes plugin surface. A language construct the host would compose on a plugin's behalf
  is a vocabulary, not a capability.
