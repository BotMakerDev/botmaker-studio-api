# ROADMAP — botmaker-studio-api

The running engineering log. `CHANGELOG.md` is the short, per-release answer; this is the detail and the
reasoning.

## Done

### 2026-09-23 — an empty `catalog()` means the host discovers the palette

A plugin's palette is the `@Palette` classes in its jar, found by the host (`botmaker-plugin-host`'s
`Palettes`), not listed by the plugin. **`PaletteCatalog.scan` stood here for one commit and is withdrawn**:
it put a class-file scanner in a module of interfaces and records, and made every plugin run the discovery
the host is placed to run once. Nothing in this module changed but javadoc and the meaning of the default.

### 2026-09-22 (last) — the parameter-data surface is deleted: `@Param` is the only source of a row

`ParameterGroup`, `ParameterEdit`, `StudioPlugin.parameters(String)`, `parameterRows(String)` and
`parameterEdited(ParameterEdit)` are **removed**. `ParameterRow` stays.

**The measurement that settled it.** A plugin declared a section and the host asked it for that section's
rows. Nothing ever declared one: the SDK's group was the only implementation in existence and it declared no
rows, and `botmaker-plugin-basics`' `ParameterStore.declare` — the call that would have written a row into a
plugin's file — had **no caller anywhere**. So `parameterRows` returned whatever sat in a pre-2026-09-17
project's JSON and empty for every project created since. A second reader of a format nothing writes is what
the umbrella `CLAUDE.md` forbids by name, and it is the same failure shape as `ValueCodec.wireOfLiteral`:
**the half nobody is forced to write is the half that rots.** Here the rotted half was the writing half, so
the reading half had nothing to read.

**What replaced it had already replaced it.** A user's parameter became a `@Param` static field in the bot's
own Java on 2026-09-17, read and written off the syntax tree. The case the surface was kept for — *a plugin's
own row, an activity's enable flag, a capture target* — has a better answer on the same terms: **the plugin
puts a `@Param` field in the file it ships**, and the host's ordinary walk of the bot's sources finds it.
One mechanism, one file format (Java), one editor, and the row a plugin owns is a row its user can read.

**`ParameterRow` survives on its own merits.** It is the shape one row crosses in — a name, a `ValueForm`,
the value as Java source, a `Visibility`, a declared option set, a `Range`, a free-text category — and the
window still draws its value with the slot editor the canvas uses. It no longer refers to a group, and its
category is now documented as the free text it always was.

**japicmp**: a removal, legitimate for the same reason as the two below — the baseline `v0.1.6` does not
exist yet and will *contain* it. **It ships in `--studio-api 0.1.6`.**

### 2026-09-21 — `PluginSource` is deleted: nobody hands the host a file

`StudioPlugin.pluginSources()` and `com.botmaker.plugin.api.source.PluginSource` are **removed**, one day
after they landed. Everything they were for survives: a bot still holds a plugin's values as `@Managed`
methods in its own source, and the host still rewrites nothing but the expression one returns. What went is
the host putting the first copy of that file into the project.

**Two things retired it.** A project gets its `plugins/<id>/` file from the template it was created from —
`botmaker-gamebot` carries one — so the only case the surface served was *adding a plugin to an existing
project*, for which a contract method, a copy on every `PluginHost.bind` and a `${package}` rewriter are a
great deal of machinery. And the skeleton being shipped shrank: it carried an `install()` that a bot's
`main` called by hand, one line per plugin, and `Bot.run(anchor, goHome, Sdk.class)` now installs every
`@Managed` value it is handed through `botmaker-plugin-basics`' new `ManagedValues`. Two `@Managed` methods
is not worth a surface.

**What it cost, accepted rather than hidden**: adding a plugin to a project with no file for it brings none.
The answer is the plugin's own window offering to write one at a click — one write, by the thing that wants
it — and it is not built yet.

**japicmp**: this is a removal, so it is legitimate only because the baseline `v0.1.6` does not exist yet and
will *contain* it. Same argument as the package move below. **It must ship in `--studio-api 0.1.6`.**

### 2026-09-21 — `33-plugin-java.md` is implemented, and the contract asked for nothing more

The plan finished on phases 5–7 without another contract change, which is the result worth recording: the
four members added on 2026-09-20 — `PluginSource`, `StudioPlugin.pluginSources()`, `PluginValues` off
`StudioServices`, and `ManagedValue` — carried a flow editor, a runtime swap and a whole template migration
with nothing added and nothing adjusted.

That is the *capabilities, never vocabularies* rule paying out. `PluginValues.open(id)` hands back a
`ValueContext`; what a `Flow` is, that an activity's body is a method reference, that a preset is a named
set of enable flags — none of it is spelled in `com.botmaker.plugin.api`. The SDK registers those shapes as
`ValueContainer`s in its own catalog and the host takes them apart generically. A contract that had learned
the word *activity* would have needed a release of its own for every one of those decisions.

**§2 of `25-compatibility.md` stays suspended** while there are no third-party plugins, and this plan used
that suspension: `ValueContext` lost `List<String> value()`/`set(List<String>)` for `form()`/`source()`/
`set(String, String...)` in phase 1. Reinstate it the day a second plugin exists.

### 2026-09-20 — a container may have arity zero, which is how a fixed shape is registered

Phase 4 of `../docs/refactor/33-plugin-java.md` needed one thing of the contract, and this is it.

**`ValueContainer.arity()` had to be at least one.** So a record with named components and no type
arguments — the SDK's `Flow(List<Activity>, List<Edge>, String, Limits)` — could only be registered as an
opaque leaf with a codec, which is a string, which is the encoding `ValueForm` exists to leave behind. A
zero-arity container answers a fixed list from `partForms` and ignores the (empty) arguments, and
**nothing else changed**, because every other method here already asked `partForms` rather than counting
arguments. That is the sign the arity check was guarding nothing: it rejected a shape the rest of the
design already handled.

Two total-ness repairs went with it: `ValueForm.Of.sourceName()` writes `Flow` rather than `Flow<>` when
there are no arguments, the rule `Declared` already followed, and `Of.last()` answers `null` instead of
indexing past the end of an empty list.

**It is not the same as `ValueForm.Declared`**, and the line is worth keeping straight. `Declared` is a
record the *bot* declares, which the host reads out of the project's own source and cannot register.
A zero-arity container is a record a *plugin* registers, with a factory it names and parts it takes apart.

### 2026-09-20 — `ManagedValue`: what a plugin owns is an annotation, not a type match

Phase 2 of `../docs/refactor/33-plugin-java.md`, and it deletes `ManagedField` and
`StudioPlugin.managedFields()` — added two days earlier, on 2026-09-18, and see the entry further down.

**A `ManagedField` named a Java type and inferred the rest.** Every `static final` field whose declared type
resolved to it was the plugin's, wherever it was written; and a class of nothing but such fields was refused
*whole*, which is how a file of picture constants got locked without anybody saying it should be. Two
inferences from shape, and both are wrong in an ordinary case: a bot that keeps one picture beside its own
code was locked out of that code, and two classes of the same type cannot be told apart at all — which
matters the moment a plugin has two values of one type, as the SDK's two `CaptureSource`s will.

**`ManagedValue(String id, String reason)` matches an annotation the author can read.** The plugin declares
the ids it answers to; the bot's Java carries `@Managed("id")` on a method or a class, where the annotation
is `com.botmaker.plugin.basics.managed.Managed` — a bot's own classpath, never the contract's, for the reason
`@Param` lives there. Neither side names the other's types; the id is the whole pairing.

**Two targets, one marker, and the difference is who names the parts.** On a **method** it is a fixed value
the plugin shipped a declaration for, and only that method's body is refused. On a **class** it is an open
set the user grows — one constant per captured picture — and the whole class is refused, because the rest of
the bot writes `Pictures.COLLECT` at its use sites and the window is what keeps that name and every use of it
in step. A flow's activities are named nowhere else, which is why a flow is a method.

### 2026-09-20 — `PluginValues`: the plugin ships the file, the host rewrites one expression

Phase 1 of the rewritten `../docs/refactor/33-plugin-java.md`, and it **withdraws the entry below**, which
shipped as `2bc1e2f` hours earlier and was never implemented by a host.

**The file is an input, not an output.** `StudioPlugin.pluginSources()` hands over a `PluginSource` — a class
name and its whole text, with `${package}` where the package line goes — and the host copies it into
`src/main/java/<bot package>/plugins/<last id segment>/` the first time the plugin is added. From then on it
is the user's file: the host never adds a method, never deletes one and never reformats it. What it rewrites
is the expression a `@Managed("id")` method returns, one `ReturnStatement` at a time.

**Five designs, and the first four shared one mistake.** A record handed over reflectively; the grammar
host-side; an annotation processor; a sequence of `ModelCall` statements written into a generated class. Every
one made the host the **author of a compilation unit**, which forces it to own the package, the class name,
the imports, the ordering and the whole round trip. The question that dissolved it was the user's: *why does
the host write a class at all, when the plugin could hand one over?* Handing it over answers all five at once
— the plugin's author already knows them — and shrinks the reader's input domain from *a class* to *one
expression*, which is precisely the domain `ValueCatalog.valueOf` has covered since `32` shipped.

**`PluginValues` is two methods**, `ids()` and `open(String id)`, and `open` hands back a `ValueContext`. That
is the load-bearing part: a slot on the canvas, a row of the Parameters window and a `@Managed` value are all
edited through one interface, so a plugin's own window needs no second way to edit a value. A body the user
has hand-edited into something that is not a single `return <expression>;` answers empty and is shown
read-only with the reason — the rule a computed `@Param` initializer already gets.

**`ValueContext` stopped speaking the wire form in the same phase**, which is what makes the above possible.
`List<String> value()` and `set(List<String>)` are replaced by `ValueForm form()`, `String source()` and
`set(String javaExpression, String... importsNeeded)`. The wire could say a leaf and a flat list of leaves
and nothing else, so an editor could never be handed `Map<String, List<Point>>` even though a bot may declare
such a field; and it was a *second encoding of a value beside the Java one*, kept in step by hand, which is
exactly the shape `ValueCodec.literal`/`wireOfLiteral` had already rotted into. `form()` is what lets an
editor claim a composite rather than a leaf.

**Every deprecated member in this module was deleted rather than carried.** `SlotContext.currentSource()`,
`replaceWith`, `enclosingClass()`, `enclosingMethod()`, `enclosingSource()`, `asSlot()`, `run()`;
`SlotRun.allowed()`; `ActionContext.projectName()`; `Dialogs.owner()`; `ValueContext.asSlot()`. Each was a
nullable member kept beside the `Optional` sibling that replaced it, and in an API no third party has
compiled against yet a deprecation is a second spelling nobody has written and everybody must read. The first
two were not merely deprecated but **redundant**: since a value is Java everywhere, `currentSource()` is
`source()` and `replaceWith` is `set`.

### 2026-09-20 — `Models`: a plugin's model as compiled Java *(withdrawn the same day — see above)*

Phase K of `../docs/refactor/33-plugin-java.md`, contract half. One capability off `StudioServices`, beside
`sources()` and by the same test: the host is the only possible source of it.

**A model is a sequence of calls.** A plugin declares the calls its model may be written as
(`StudioPlugin.modelCalls()` → `ModelCall`: an owner, a method, and one `Argument` per parameter) and hands
over `ModelStatement`s — *call `Activities.declare` with these six things*. The host writes the Java into
`<bot package>.plugins.<segment>` and reads it back. Nothing crosses as text in either direction, which is
the rule `33` states and which the rejected alternatives both break: a plugin that emits text makes every
plugin a code generator the host cannot check, and a plugin that supplies a *write* callback supplies one
direction and leaves the other to discipline.

**That second failure is measured, not hypothetical.** `ValueCodec.literal` had seventeen implementations and
`wireOfLiteral` — a `default` returning empty — had nine, because the reader had exactly one caller. The half
nobody is forced to write is the half that rots. One declaration that both halves are derived from makes it
impossible not to have both.

**`ModelCall` is `ValueContainer` one level up, and that is the whole argument for this shape.**
`factoryOwner`/`owner`, `factory`/`method`, `partForms`/`arguments`: `ValueCatalog.initializer` already
writes `java.util.List.of(a, b, c)` and `valueOf` already reads it back by matching the prefix and splitting
at depth zero. **A statement is that with a semicolon**, so the writer and the reader are machinery that
exists and is tested rather than machinery this had to invent.

**A record handed over reflectively was built first and withdrawn the same day.** It worked. What it had to
answer, and a sequence of statements never asks: naming a constant from an activity name, stably and
uniquely; choosing between a seven-component canonical constructor and a six-argument convenience one; a
compact constructor normalising `id` into a duplicate of `name` inside the user's repository; keeping map
order deterministic; cross-file constant references; and Jackson's `"empty": false`, a derived value
persisted beside what it was derived from. The observation that replaced it is that **the bot already stores
the hard half as calls** — `Activities.define("Collect", ctx -> {…})` — so the rest should be stored the same
way, and then the file reads like the file beside it.

**`MethodRef` is the one argument that is not a value.** `Collect::body` has no type of its own — its type is
whatever functional interface the parameter declares — so no codec could honestly claim it is a literal of
something, and forcing it through `ValueType` would have meant a `sourceName` that lies. It crosses as two
names, written and read exactly as an enum constant is. What it buys: renaming or deleting a body in the
bot's Java is a **compile error** in the generated file, rather than a model pointing at nothing discovered
three screens into a run.

**Three methods, all total.** `write(pluginId, simpleName, statements)` answers the file or empty;
`read(pluginId, simpleName)` answers the statements in order, or an empty list (a project that never had one
is the ordinary case, not an exception); `problem(ModelCall)` answers *which argument* cannot be written, so
a plugin asks once at startup rather than discovering it when a user saves. The set the host accepts is made
equal to the set it can write, which is how *what Studio writes always compiles* becomes a property rather
than a hope.

**The plugin names its own id.** The sketch in `33` took two arguments; one `StudioServices` instance serves
every plugin, so the segment has to arrive with the call. The alternative was a per-plugin services object
threaded through every place an editor is built. A plugin already names its own id to address its own
storage (`PluginData`), so the trust model is unchanged, and the last segment maps to a package by the same
normalisation — with one forced difference, that a dash becomes `_` because a folder may be called
`my-plugin` and a package may not.

**A class, a method name and a `ValueForm` are facts about Java**, so *capabilities, never vocabularies*
holds. `Models.NONE` is the total default, as `Sources.NONE` and `Runs.NONE` are. `ModelCall` is reached
through `of(…)` and never a constructor, the same rule as `ParameterRow` and for the same reason: a plugin's
compiled classes cannot be rewritten by anybody, so a record it constructs may not grow a component.

### 2026-09-20 — `ValueChoice` and `ValueShape` are deleted

Phase G of `../docs/refactor/32-generic-values.md`. `ValueForm` had been carrying every caller since phase D;
what is gone now is the pair it replaced, its bridge (`ValueForm.asChoice`, `ValueChoice.form()`) and
`ParameterRow.type()`.

**A shape answered two unrelated questions at once**, and that is the part worth keeping in mind rather than
the deletion. *How many* is the type's business and is a container now. *Out of what set* never was: it is
what the author wrote down beside the declaration, which the row has always carried as `options()`, and
encoding it in the type meant the same fact lived in two places and had to be kept in step — the whole
purpose of `VariableModel.listShapeOf`, which read a sibling field to decide which of two identical shapes a
stored `ANY_OF` had meant. That function is gone too, and so is the question.

What survived is one method. `ValueForm.leaf()` answers the single type a form's values are typed as — the
form itself, or a container's last argument, and `null` for a tree with more than one. Every caller that read
`ValueChoice.type()` asks it, and the two questions it exists for are the declared set and the declared
`Range`, both of which are meaningless without one leaf to be values of.

**The flat pair is renamed, not deleted.** `initializerOfWires(ValueForm, List<String>)` and
`wiresOfInitializer(ValueForm, String)` are the old `initializer`/`valueOfInitializer`, keyed on a form and
named for what they carry. They exist for a caller holding one string per item — a plugin's own JSON file —
delegate to the live-value grammar and compose nothing, and go when those files do (phase L). Naming them for
the wire is deliberate: nothing should reach for them without noticing.

### 2026-09-19 — a plugin can say which constants are its window's

`StudioPlugin.managedFields()` and `ManagedField(typeName, reason)`, `default` and empty. A plugin names a
field **type**; the host matches a `static final` field of that type, draws it with the plugin's
`SlotEditor.preview` and refuses a canvas edit with the plugin's sentence, and refuses a class holding
nothing else whole.

It is a **capability, not a vocabulary**, which is the test this surface has to pass: the host learns that
some constants are maintained elsewhere, and nothing about what a picture is. The alternative considered and
rejected was a path rule in Studio naming `Pictures.java` — the maintainer's objection is the rule itself, that
an `ImageTemplate` constant is the SDK plugin's concept and the host may not know it. Data crosses as text (a
type name, a sentence); no plugin code runs while a file is drawn.

### 2026-09-10 (later) — `projectOpened`, the half the surface could not work without

`StudioPlugin.projectOpened(StudioServices)`, `default` and no-op, called once per bind before the incoming
plugins are asked for anything. Phase 6 of `settings-becomes-a-plugin.md` found the hole the moment the SDK
plugin tried to answer `parameterRows`: **it had no way to know which project it was serving.**
`StudioServices` reaches a plugin only through a `SlotContext` or an `ActionContext` — that is, only when a
user is already editing something — and a row list is asked for before any of that. `SdkPlugin` had the
symptom written down already, in the javadoc of its toolbar items: *"constructed with no `StudioServices`,
so a label supplier here has no project to read one out of"*.

**Two shapes were possible and the choice is the interesting part.** Threading the services through each
call (`parameterRows(services, groupId)`) is local and needs no lifecycle — and it puts the host in the
signature of every data surface that follows, forever, for a fact that changes once per project rather than
once per call. The lifecycle pair is the other answer, and it is the one the contract already reaches for:
`projectClosing()` exists precisely because a plugin cannot learn that its project is gone, and *which
project it now has* is the same fact from the other end. So it is the mirror method, with the mirror rules —
total (a plugin that throws does not stop the next one or the project), reused instance (replace what is
held, never accumulate), and **nothing expensive**, because this runs on the path a user is waiting on.

`PluginHost.bind` grew a `StudioServices` argument and calls it from `swap`, after the outgoing set has been
told the old project is closing — one plugin may serve both, and being handed the new project while it still
believes it holds the old is how a release runs against the wrong paths. Nobody is told on `unbind`: there is
no project to name, and `projectClosing()` has already said it is over.

### 2026-09-10 — parameter data: the seventh contribution surface

`StudioPlugin.parameterRows(String groupId)` and `StudioPlugin.parameterEdited(ParameterEdit)`, plus
`ParameterRow` and `ParameterEdit` beside `ParameterGroup`. Phase 4 of
`~/.claude/plans/settings-becomes-a-plugin.md`, which the plan itself calls the riskiest phase and the one to
stop on rather than force. **No host is wired to it yet** — that is phase 5, the ~94-file retype of Studio
onto this module's vocabulary; what landed here is the surface, its javadoc and its tests.

**The problem it answers.** `ParameterGroup` (2026-08-29, categories 2026-09-02) already said the right
thing: a plugin declares the *section* of the Parameters window and the generated class its values become
fields of, while the user declares the values themselves. But the values were read out of `activities.json`
by Studio's own `ActivitiesConfig`, a file and a parser that predate the plugin system by a year. Three
consequences, all of them the same shape as the leaks this module exists to close: the host knew what a
`tag`, a `visibility` and a stored duration were; a *second* plugin could not have had parameters at all;
and the check the previous plan's phase 7 proposed — `Settings.load("name", T.class)` verified against the
declared variables — would have put one plugin's API (a class name, a method name, the meaning of a class
literal) inside the host. That phase was cancelled and this surface replaces its foundation.

**The cut is the same one `StudioServices.sources()` made on 2026-09-01.** The host keeps what only the host
has — one window rather than one per plugin, the ordering, the rendering, the undo, the theme. The owner
keeps the file, the meaning of the text and the decision of when to write. Neither half is a vocabulary.

**Every component of a row was already here**, which is the test this surface had to pass and the reason it
needed no exemption: a name, a `ValueChoice`, the stored `List<String>`, a description, a category out of
`ParameterGroup.categories()`, a `Visibility`, the declared options, a `Range`. The value crosses as
**text**, exactly as it does through `ValueCodec`, so rule 3 (*no syntax tree*) and rule 2 (*no `Class<?>`
the host loads*) hold by construction rather than by care.

**`ParameterRow` is a final class with a builder and `ParameterEdit` is a record, and the asymmetry is the
point.** Compatibility trap #2 bans growing a record a **plugin** constructs — the canonical constructor is
part of the binary signature, so a component added later throws `NoSuchMethodError` in every plugin already
compiled. A plugin builds rows, so rows get a builder, exactly as `ValueType` did for the same reason. The
*host* builds an edit and a plugin only reads it, so an edit can be a record and can grow: the same call
`Sources.Use` makes, written down here as a pair so the rule reads as a direction rather than as a blanket
ban on records. `ParameterRow` implements `equals`/`hashCode` over every component, because the host
compares what a plugin answered against what it sent and object identity is meaningless across two
classloaders — the same reasoning as `ValueType` comparing by `id()`.

**`parameterEdited` answers the row as stored.** One return type carries a clamp to a declared `Range`, a
list pruned to the options still on offer, a duration spelled back canonically, and a flat refusal — the
window renders what comes back, and the host models none of it. `Optional.empty()` means *not mine*, for a
row or a group this plugin does not own, and leaves the screen alone.

**Two things were deliberately not added, recorded so they are not re-derived.** There is **no listener**: a
push is a capability with a lifecycle (a registration, a thread, an unsubscribe) and the only thing it buys
is a plugin's own dialog editing a value behind the window's back, which no plugin has. And there is **no
`kind` enum** over adding, deleting, renaming or retyping a row. Those are different questions — they need
the declaration rather than the value, and retyping needs the coercion rules that belong to the editor,
where a user can watch them happen. They arrive as their own `default` methods when the window that performs
them does, and because `ParameterEdit` is host-constructed, waiting costs nothing.

`ParameterDataTest` holds what a host and a plugin both rely on before either exists: that a plugin
implementing only `id()` contributes no rows and is answered `Optional.empty()` for an edit (the versioning
rule, as one assertion), that every default is the reading that keeps a project open, that a row with no
type at all holds an *unknown* one rather than a `null`, that a value list is copied rather than shared, and
that a row survives `withValue`/`toBuilder` with equality intact.

### 2026-09-02 — JDK 25 LTS and JavaFX 25 across all eleven repositories

The whole constellation moves from Java 21 to **Java 25 LTS**, and from JavaFX 21 to **25.0.4** — the newest
patch of the matching LTS line, rather than the bare GA the old pin used. `botmaker-pilot` stays on 17: that
is the Android Gradle toolchain and has nothing to do with this.

**The risk was JitPack, and it was settled before anything was touched.** Eight modules publish there and a
bot resolves the SDK from it, so an image without a 25 JDK would have stopped the migration dead — the
documented workaround being an SDKMAN install in `jitpack.yml`'s `before_install`, which is a per-module
thing to maintain forever. It was proved on a throwaway **branch** build of this module (a branch build
resolves as `<branch>-SNAPSHOT`, so no junk tag had to be cut and later lived with): JitPack reported
`Setting java 25.0.2-open as default`, the build succeeded, and the published jar's class-file major version
was **69** — Java 25, where 21 would have been 65. It genuinely compiled at 25 rather than silently at 21.
No `before_install` is needed anywhere.

**`maven.compiler.release` replaces `source`/`target` in all ten poms**, including the one the archetype
generates. The difference is not cosmetic: `source`/`target` compile against whatever JDK is *running*, so on
a maintainer's JDK 26 box a call into a method that exists in 26 and not in 25 compiles clean and throws
`NoSuchMethodError` on a runner pinned to 25. `release` checks against 25's own platform API. It was safe to
switch because nothing here compiles against a JDK-internal API — every `com.sun.*` import in the
constellation is `com.sun.jna`, an ordinary library — and the only `--add-exports`/`--add-opens` are in
Studio's *surefire* `argLine`, which `release` does not police.

What moved, exhaustively: `jitpack.yml` × 8 → `openjdk25`; `maven.compiler.release` → 25 in ten poms;
`javafx.version` → 25.0.4 in five, plus the archetype's literal `javafx-controls` version; `java-version:
'25'` in fifteen workflow steps across eleven repositories, the plugin registry's `validate.yml` among them
(it resolves and runs `botmaker-cli`, so it must not be older than the CLI's bytecode); and the two
`testing/` Docker images, where `java-25-openjdk-devel` and `openjdk-25-jdk` both turned out to exist in
Fedora 43 and Ubuntu 24.04 respectively — so the images keep their bases and needed no backports line or
third-party JDK repository. The Fedora image's `FROM fedora:43` pin is kept but its *reason* is rewritten: it
existed because F44 had dropped `java-21-openjdk`, and that constraint is simply gone now.

**One thing is deliberately not proven yet: japicmp against a 25-compiled baseline.** The plugin loads and
runs on 25, but both baselines (`v0.0.1` here, `v1.2.0` in the SDK) are currently unresolvable — one is the
broken tag, one is unreleased — so `ignoreMissingOldVersion` makes it report and pass without ever parsing a
major-69 class file. Whether japicmp 0.23.1's javassist can read one is answered by the first release with a
working baseline, and the SDK's pom already records the neighbouring fact that its bundled Groovy cannot read
Java 26 class files. Watch that on the next release rather than assuming it.

**Studio's test suite fails 78 tests on this tree and failed exactly the same 78 before it** — verified by
stashing the pom change and re-running: 1001 tests, 78 failures, 6 errors, both times. They are the
fallout of Studio dropping its SDK dependency on 2026-09-02 (`SdkUpgradeServiceTest`, `SplitPointerTest`, the
toolbar's missing Pilot item) and are not this change's. Every other module's suite passes on 25.

### 2026-09-02 — the gate that published nothing, and the baseline that gated nothing

v0.0.1 — this module's first tag ever — produced **no artifact on JitPack at all**, and took
`botmaker-sdk` v1.1.1 down behind it (`Could not find artifact
com.github.LiQiyeDev:botmaker-studio-api:jar:v0.0.1`). Two separate faults, both in the japicmp block.

- **The plugin would not load on JitPack's Maven.** `japicmp-maven-plugin:0.23.1` declares a Maven 3.6.3
  prerequisite and JitPack's builder runs an older one, so the build died with `The plugin ... requires
  Maven version 3.6.3` — and it died at plugin **load**, before any configuration was read, which is why
  the `<skip>` flag the pom already had could never have avoided it. japicmp now lives in an **`api-gate`
  profile** activated by the *absence* of `botmaker.japicmp.skip`, and `jitpack.yml` passes that property.
  `mvn verify` here and in CI is unchanged.
  - The activation is property-negation rather than `<activeByDefault>` deliberately: Maven cancels every
    `activeByDefault` profile as soon as any profile is named on the command line, so a build that named
    an unrelated profile would silently lose the gate. (`botmaker-sdk` has a `pilot` profile and would
    have hit exactly that.)
  - Skipping it on JitPack costs nothing real, and the reasoning is the one `-DskipTests` in that file has
    always used: JitPack builds an artifact **from a tag that already exists**, so a gate there can only
    break the publish — it cannot prevent the change. The refusal that matters is `mvn verify` in this
    repository's CI, before the tag.
- **The baseline named a tag that has never existed.** `botmaker.japicmp.baseline` was `v1.0.0`, a
  placeholder from before the module had a release, and the instruction in `CLAUDE.md` to *"set it to the
  previous tag in every release commit from the first one onward"* was never once carried out. With
  `ignoreMissingOldVersion` — which exists so a first release is not refused for a reason its author cannot
  act on — that is not a strict gate but a no-op reporting success. Worse, `v1.0.0` sorts **above** every
  tag this module will have for a long time. It is `v0.0.1` now, and the umbrella's `release.sh` bumps it
  in each release commit (`bump_japicmp_baseline`), never backwards.

### 2026-08-31 — a run of slots, and a picture for a declared choice

Two additions, both `default`, both forced by the same editor: the image-template **group** picker, which
the port of the template editors could not carry because its chip row is only partly a slot editor. Its
three callers are the `ImageTemplateGroup.of(…)` slot, an image **varargs** run
(`Matches.hasAny(a, b, c)`) and the `Matches` switch, which narrows the offered set to what the enclosing
find call can produce. Only the first is one argument of one call.

- **`SlotRun`, reached through `SlotContext.run()`** (`null` for a slot that stands alone, which is nearly
  all of them). `elements()` are the run's Java expressions in order; `replace(List<String>, String...)`
  writes the whole run, because an editor that can only write inside its own argument can change an element
  and never add or remove one. `minimum()` and `allowed()` are the host's two narrowings — how few elements
  the surrounding code still compiles with, and the only element sources it will still accept.
- **`allowed()` is element *source*, not decoded values**, and that is the part worth remembering. Studio's
  own narrowing decoded the enclosing group into template paths, which is the host reading a plugin's
  vocabulary out of a plugin's expressions. Listing the arguments of a call is syntactic and needs no such
  knowledge; the plugin decodes them itself. The general rule: when the host has to describe a plugin's
  values, it describes them as the text they are written as.
- **`SlotEditor.preview(ValueContext)`**, `default null` — a small, non-interactive picture of one value,
  for the one place the host *shows* a value without editing it: beside a declared choice. Not a sixth
  contribution surface — it reuses `matches()`, and its default is exactly what a plugin-registered type
  gets today, so nothing is lost by not implementing it. The second `SlotEditor.of` overload takes it.

The host side and the editors that consume both land with the template-editor port; `TestContexts` gained
`withRun` in the same pass, so a plugin author can exercise a run editor — including that a `replace` below
`minimum()` is refused — without a running Studio. Toolkit tests 39 → 45.

### 2026-08-30 — a second plugin interface was built and withdrawn the same day

`CompanionPlugin` — `id`, `displayName`, `toolbarItems`, `projectClosing` — plus `ThemeTokens` and
`StudioServices.themeTokens()`, on the argument that `StudioPlugin` is two interfaces wearing one name: one
for plugins that shape the user's **code**, one for plugins that do something **beside** it. The Remote
Pilot was the case that drew the line, and out of the split came `botmaker-plugin-protocol` (JSON-RPC over
stdio, so a companion could be written in any language) and `ProcessPlugin` in `botmaker-plugin-host`.

**All of it is reverted.** The maintainer's judgement, and it is the right one to record rather than argue:
**the machinery was heavier than the problem it was solving, and the problem was not clearly stated.** One
plugin — the pilot — wanted to be out of process, and answering that took a new module, a new dependency
(LSP4J plus a pinned Gson), a process supervisor, a restart policy, a descriptor format, a wire-record
parallel of four contract records, and a mapping layer to keep them in step by hand. The pilot stays an
**exception attached to the SDK plugin** instead: `SdkPlugin` owns its toolbar item and releases it in
`projectClosing()`, exactly as before.

**What is worth keeping from the exercise, for whoever proposes it again:**

- **Seven of `StudioPlugin`'s eight surfaces already reduce to JSON.** `id`, `displayName`, `catalog`,
  `valueTypes` (a `ValueCodec` is `String`→`String`), `parameters`, `toolbarItems`, `projectClosing`. The
  one that does not is `slotEditors()`, which returns a JavaFX `Node` — and it is exactly the set that
  belongs to shaping code. That audit is still true and is the honest starting point for a second attempt.
- **The line, if it is ever drawn again:** *if it decides what the user's code says it is a `StudioPlugin`;
  otherwise it is a companion; a surface that seems to want both is two surfaces.*
- **Javac states the two-classes rule for you.** Two unrelated interfaces declaring the same `default`
  cannot both be inherited, so a class implementing both must write all of them out.
- **The trigger to watch for is a second plugin, not a better protocol.** The whole cost above was carried
  by one plugin that is already in the SDK's jar and already works. When a plugin exists that *cannot* be
  Java — not merely one that happens to be written in TypeScript — the ledger changes.

The work is in the history if it is wanted: contract `eee3891`, protocol module umbrella `21706a9`, host
`4653064`, Studio `ee33fe5`, SDK `b934aa4`.

### 2026-08-29 — the scaffold surface is deleted: a plugin does not write files

`com.botmaker.plugin.api.scaffold` (`@Scaffold`, `@ClassName`, `@EnumValues`, `@Editable`, `Seeding`),
`catalog.ScaffoldCatalog`/`ScaffoldEntry`/`ScaffoldPlan` and `StudioPlugin.scaffold`/`seedings`, gone whole —
days after they landed, and the reversal is worth more than the surface was.

- **What it did.** A *seed* was a real compiling class in the plugin's own build, marked with what a host may
  substitute, written into a user's project once and thereafter maintained at the marks. Every step of it was
  an improvement on the `SourceEmitter` it replaced: javac checked the seed, the class list was class
  literals, `ScaffoldPlan` validated without a parser, and `Seeding.key` told a rename from a
  delete-plus-create.
- **What was wrong is one level up.** Replacing one code generator with a generalised one made the wrong
  thing a *surface*: any plugin could own files inside somebody's source tree. A file a plugin owns is a file
  its user cannot freely edit, rename or delete — and the ledger, the reconciler and the reference-rewriting
  rename engine the host grew were all cost paid to work around that.
- **The rule now**: a project's structure belongs to the user; a plugin contributes methods a user calls. It
  is the same shape as the `Assets`/`Capture.SourceChoice` reversal two days earlier — *the host owned the
  policy only because it was written first* — and it is stated in `CLAUDE.md` beside that one.
- **What the surface was actually for still has an answer.** An activity's behaviour becomes
  `Activities.define("Mining", ctx -> …)`, written wherever the user likes. What followed as a whole from
  project data was data all along, which was the seed surface's own stated rule and the one its own files
  failed. The compile check a per-activity `Outcome` enum bought is replaced by a host picker on the
  argument — a contribution surface this module already has.
- **Kept from it**: `javax.lang.model.SourceVersion` over a hand-rolled keyword list, and the reminder that
  reflection promises no member order. Both are recorded in `CLAUDE.md`.

### 2026-08-29 — `types()` answers in registration order again

`ValueCatalog` held its registrations in `Map.copyOf`. That produces an immutable map whose iteration order is
unspecified **and randomised per JVM run** — so `types()`, whose own javadoc promises registration order and
which backs every "what type is this variable" dropdown, answered a different order every time Studio started.

- **The symptom is not bug-shaped.** Nobody files "the dropdown is in a different order today"; they conclude
  the application is unreliable. And no single run of anything could show it — it surfaced only from diffing a
  generated `Parameters` file across two builds of the *same* source and getting two different files.
- **It had been true since the vocabulary opened**, i.e. since `ValueType` stopped being an enum. An enum has
  a declaration order for free; a registry has one only if it keeps one.
- An unmodifiable `LinkedHashMap`, with the reason on the field so it is not "simplified" back.
  `typesComeBackInRegistrationOrder` pins three things: one catalog's order, two identically built catalogs
  agreeing (in one run and across runs), and a merge **appending** — installing a second plugin must not
  reorder the first's types.

### 2026-08-29 — the toolbar surface, and the one member that could not be a String

Plugin platform, phase 13a. `ToolbarItem`, `ToolbarGroup`, `EnabledWhen`, `ActionContext` and
`StudioPlugin.toolbarItems()`. The fifth contribution surface, and the first one where the plugin hands over
**data** rather than a `Node`.

- **Why data here and a `Node` for `SlotEditor`.** A bespoke image picker cannot be described as a record; a
  button can. What describing it buys is everything a *shared* bar has to own centrally — the group, the
  order, the separators, the packing, the overflow menu, the icon box, the theme. A surface returning nodes
  would hand all of that to each plugin, and two plugins would produce a bar with two button heights.
- **The label is a `Supplier<String>`, and so is the icon.** This is the one place the sketch was wrong and
  the correction is the useful part: the host's own bar already had a Capture Target button that relabels
  from project state, a Launch Target button that does the same *and* resolves a game's real title and cover
  art on a background thread. A record of `String` would have described a toolbar nobody has. The cost is an
  obligation on the plugin — cheap and pure, called during layout on the FX thread — stated in the javadoc
  where somebody will actually read it.
- **A fourth contributable group, `TOOLS`, was added while porting the host's own bar.** With
  `PROJECT`/`AUTHORING`/`RUN` alone, today's hand-arranged reading order could not be reproduced: the
  instruments (Input, Templates, Overlay, Record, Resources) sit *after* the run cluster. That the four
  groups reproduce the existing sequence exactly was the acceptance test for the port, and it is also the
  argument that the groups are real rather than invented.
- **`ToolbarGroup.STUDIO` is refused, not re-homed.** The host's own section, for what would still make sense
  with every plugin uninstalled. A plugin quietly moved out of it would sit where a user reads the
  application rather than their project, so the drop is loud and names the plugin.
- **`EnabledWhen` is a closed set rather than a predicate**, and the reason is cost rather than taste: a
  `BooleanSupplier` is somebody else's code called once per item per plugin on every state change, inside a
  layout pass, with no way for the host to know what it reads. Four states the host already broadcasts make
  it a switch. An item wanting more says why in a dialog when pressed — better than a greyed button with no
  explanation.
- **No toggle kind and no read-out kind**, though the host has one of each. Adding a contract member for the
  host's only instance of a thing is exactly what the `Assets`/`SourceChoice` reversal recorded, so Studio's
  debug toggle and resolution label stay hand-built nodes placed beside the described ones.
- **`ActionContext` is three members**, on `StudioServices`' rule: the project's name (a name, not a path —
  a path invites writing into a project directory behind the host's back), the plugin's own pinned version,
  and the services. Nothing a plugin could answer for itself.

**What this surface does not yet carry, and it is the whole of phase 13:** the SDK contributes no items
through it. Its eleven buttons are still Studio's, because their actions are Studio dialogs on Studio's
project services, and moving the *declaration* without the *behaviour* would need an intent registry — which
was offered and declined. The dialogs move instead, once phase 14 gives them a project surface to stand on.

### 2026-08-28 — the GitHub Release is published from here, by JReleaser

- **`jreleaser.yml` and a `release` job in `ci.yml`.** A `v*` tag now publishes this module's GitHub
  Release from its own CI, with the `## [x.y.z]` section of `CHANGELOG.md` as the body. It used to be a
  `gh release create` inside the umbrella's `release.sh`, which keeps everything JReleaser cannot express —
  which modules are being cut, at what versions, in what order, and the tag itself. JReleaser's unit of
  work is one repository; the umbrella's is eight with a dependency order between them.
- **`tools/changelog-section.sh`** — the extractor, moved out of `release.sh` into this repository so that
  the two readers which must not disagree can both reach it: the umbrella's `check_changelog` gate calls it
  before anything is tagged, and the workflow calls it for the notes. A release whose body is extracted by
  a different rule than the one that gated it can pass the gate and then publish something else.
- **Two findings worth keeping, because each reads as a configuration mistake until you hit it.** JReleaser
  **cannot open a submodule**: in the umbrella working copy `.git` is a `gitdir:` FILE and its JGit reports
  *repository not found*, while `--git-root-search` gets past that only by resolving the **umbrella**
  repository — which would attach a module's release to the wrong repo. Hence CI, where a checkout is
  standalone. And `jreleaser-maven-plugin` is not a way round it: it ignores `jreleaser.yml` entirely (its
  model comes from an XML block in the pom) and takes the version from `<version>`, which here is the
  cosmetic `0.0.0-SNAPSHOT` JitPack overrides. The version arrives as `JRELEASER_PROJECT_VERSION`, read off
  the tag.
- **The build is untouched** — no Maven plugin, no lifecycle binding, no pom edit.
- Also found, and now refused in `release.sh`'s decide pass rather than halfway through a run: **this
  repository has no `origin` remote.** It was created inside the umbrella and has never been pushed. Create
  `LiQiyeDev/botmaker-studio-api` before any `--studio-api` release.

### 2026-08-27 — reflection replaces the processor, japicmp replaces the back edge

The annotation set narrows to five and the catalog stops being generated.

- **`@Facade` → `@Palette`** (the `role` element deleted) and **`meta.@Internal` → `palette.@Hidden`**,
  narrowed back to what it actually did. `FacadeRole`, `CatalogBuilder`, `MemberRef` and `M0`–`M5` deleted;
  `FacadeEntry.role` is a `boolean offered` and `isFacade()`/`inMenus()` are gone. **Two bits, not three:**
  every consumer read one, and `VALUE` only ever existed to work around `@Internal` welding *not-surface* to
  *not-offered*. All four palette annotations are `RUNTIME` now, because the plugin reflects on its own.
- **`PaletteCatalog.of(Class<?>...)`** is the entry point. Members are discovered, so the property the
  method-reference builder was defended on — *a catalog naming a renamed member does not compile* — answers
  a problem that no longer exists; the class list stays javac-checked as class literals. It also removed a
  cost only an outside plugin author would have paid: a pom omitting `<annotationProcessorPaths>` got no
  catalog and no diagnostic.
- **`SourceOrder`** (package-private, ~150 lines) parses the class file's constant pool and `methods` table
  to recover declaration order — the one processor capability reflection lacks, and what let the switch
  reproduce the SDK's generated menus exactly (52 facades, same order, every `.order(…)` prefix). Every
  failure path returns an empty list and the caller sorts alphabetically.
- **Load-time validation degrades rather than throws**, collected into `PaletteCatalog.problems()`: two
  `@PaletteDefault`s on one name, a `@PaletteLabel` on a `@Hidden` member, two facades disagreeing about a
  category label, a class with no `@Palette`, and a facade whose members cannot be read at all. That last one
  was found by hitting it — `getDeclaredMethods()` throws `NoClassDefFoundError` when a signature names a
  class the classloader lacks, and it is all-or-nothing, so the facade is reported and offered with no
  members. Precedent: `ValueCatalog.merge`.
- **Constructors are deliberately not catalogued** — reflecting them added an `<init>` entry to seven offered
  static facades. `MemberId` keeps its constructor support.
- **`@Replaces` and `@Since` deleted**; `@ReplacedBy` stays and stays `CLASS`-retention, since Studio reads it
  from a jar it never loads. **japicmp on `verify`**, unconditional, no ignore list: the record-component trap
  from `25-compatibility.md` is checked by something now. The August objection to japicmp — *CI cannot tell an
  intended break from an accident because it cannot see the version* — is about a conditional rule; both new
  uses are unconditional.

### 2026-08-27 — the host services grow to fit the editors (plugin platform, phase 12a)

Phase 12 moves the SDK's thirteen slot editors out of Studio. Surveying them first found that **four cannot
move without this module growing**, and that is the honest way to read every addition below: they are not a
guess at what a plugin might want, they are what an editor already written could not do without.

- **`Assets`**, on `StudioServices.assets()` — the project's named pictures. The `ImageTemplate` and
  `ImageTemplateGroup` editors reached `ImageTemplateLibrary`, `ProjectConfig`, `ProjectSettingsService` and
  `CoreApplicationEvents` for it. It is a service and not a directory listing because **naming one is policy
  the host owns**: reserved names, sanitising, collisions, tags, and who is told when the set changes. Two
  plugins walking `resourcesDir()` themselves would answer all of that differently about one folder.
- **`Capture.pickPoint`** — one pixel under a magnifier. Deliberately not `selectRegion` with the size thrown
  away: at 1:1 the cursor covers the pixel it is choosing.
- **`Capture.Frame` + `grabTargetFrame`** — the AWT pixels and the label of the target they came from.
  `grabFrame` hands over a JavaFX `Image`, which is right for *showing* a frame and useless for *searching*
  one, and a frame with no label leaves the user guessing which monitor was read. Never a silent desktop
  fallback: a frame of the wrong thing answers a question the editor did not ask.
- **`Capture.Sample` + `sampleFromTarget`** — the eyedropper, carrying `spread`. That number can only be
  measured at the moment of sampling (it is how much the chosen pixel's neighbourhood varies) and it is the
  honest suggested tolerance — the justification a ΔE slider has never had.
- **`Capture.SourceChoice` + `chooseSource` + `defaultSource`** — and this one is the design decision worth
  keeping. **A capture source crosses as data, never as an expression.** The host enumerates monitors, windows
  and emulator instances and paints the thumbnails; the plugin decides what a choice is *written down as*. The
  alternative — the host handing back `CaptureSource.window("…")` — would bake one plugin's vocabulary into the
  contract and make a second capture-source editor impossible.

Two shape decisions inside that:

- **`SourceChoice.Kind` is an enum component of a record, not a sealed hierarchy.** A new permitted subtype
  breaks a plugin's `switch` at recompile; a new enum constant with a documented `default` arm does not. The
  Javadoc says to read it with one.
- **`assets()` is `default`; the new `Capture` methods are not.** The rule that every addition must be
  `default` protects plugins from an older *host*, and `StudioServices` is what a plugin holds — so it gets the
  default. `Capture` is host-implemented only; an abstract method there breaks a host at compile time, which is
  a build failure in this repository rather than a user's plugin dying at runtime.

### 2026-08-27 — parameters become a plugin surface (plugin platform, phase 11)

`ValueContext` is new, and it is the AST-free half of what an editor needs: `TypeRef type()`,
`List<String> value()`, `void set(List<String>)`, `StudioServices services()`. `SlotContext` **extends** it
with the source-text half and its `slotType()` is gone — the inherited `type()` was the same question asked
twice. `SlotEditor` widened from `SlotContext` to `ValueContext` in all three of `matches`, `create` and
`of`, and that widening is the whole point: **one editor now serves both the code editor's argument slot and
the Parameters window**, because both are "a value of a Java type", and a plugin writes the predicate once.
`asSlot()` is how an editor that genuinely needs the call site asks for it — null when there is none.

`ParameterGroup(id, title, className)` is new, and `StudioPlugin` gains
`default List<ParameterGroup> parameters(String pinnedVersion)` — empty by default, so the contract's rule
that every method but `id()` is `default` is intact. A group is **one plugin's section of the Parameters
window and one generated file**, which is the whole-file-ownership rule applied to parameters: two plugins
may both offer a `timeout` because they are fields of two classes. `DEFAULT_ID` is `""`, so a project
written before groups existed reads back as the default plugin's — no migration, and the discriminator is
absent rather than wrong.

Nothing here carries a JSON annotation, and that is deliberate: the contract's one dependency is
`javafx-controls` at `provided`, and a serialiser added here would be imposed on every plugin ever written.
The contract declares the wire *form*; whoever owns the file supplies the parser (the SDK's `ValueJson`).

### 2026-08-27 — a value type says where a picker files it (plugin platform, phase 10b)

`ValueType.group()` — a free `String`, `""` for the top level — and `Builder.group(…)`. Studio's picker used
to read the grouping off `BotType.Group`, an enum in Studio, which is exactly the constant a second plugin
could never be granted: a plugin's own types would have arrived in a menu with nowhere to sit. A free string
for the same reason the class is not an enum.

The ordering rule is worth stating because it is the one a second plugin could otherwise use to disturb the
first: types carrying the same group are shown together in **registration** order, and the **first**
registration of a group decides where that group sits. A plugin cannot reorder another's menu by naming its
heading.

### 2026-08-27 — the compatibility vocabulary arrives (plugin platform, phase 8c.4)

`@ReplacedBy`, `@Replaces` and `@Since` are `com.botmaker.plugin.api.meta` now, beside `@Internal`. They were
`com.botmaker.sdk.api.meta`, which made them the SDK's rather than every plugin's — compatibility trap #8:
a second plugin renaming its own public types had no equivalent and its users' bots broke with a bare compile
error. `botmaker-plugin-processor` already checked them by FQN string, so the move is what makes that
checking usable by anybody.

- **The SDK's three survive one minor** as `@Deprecated(forRemoval = true)` shims with `@ReplacedBy` at the
  new FQNs, so the pointer pair's first use is its own move. Nothing about the grammar changed.
- **A pointer may name a target in another module** — which the move is the first instance of. The
  `@version` on such a `@Replaces` entry is the **old** module's: the last release *that* module shipped the
  old spelling in, because the entry is a statement about a spelling being retired.
- **A scan that folds meta-annotations must filter to direct ones.** These three annotate each other now, so
  ClassGraph reports `@Since`'s own `@Replaces` on every element that merely uses `@Since`. `javax.lang.model`
  has no such folding, so the processor is unaffected; a host reading pointers out of a jar is not.

Two earlier arrivals in this module are logged in `../botmaker-sdk/ROADMAP.md` rather than here, because the
work that moved them was the SDK's: the palette annotations and `@Internal` (phases 8 and 8c.2), and the
value vocabulary under `com.botmaker.plugin.api.value` (phase 10a).

### 2026-08-26 — the module exists (plugin platform, phase 5)

Created as the seventh BotMaker repository, first in the umbrella reactor. It holds the contract that lets
Studio become a **plugin host** and `botmaker-sdk` become plugin #1 — a privileged default plugin, but a
plugin, with no back door into the host.

**Why a separate repository rather than a package in the SDK or in Studio.** The contract must be allowed to
version *slower* than either. A bot's source can be rewritten when the SDK changes, because Studio holds an
AST of it; a plugin's compiled `.class` files cannot be rewritten by anybody. Putting the contract in the
SDK would tie its cadence to the SDK's, which changes every week, and a third-party plugin would then depend
on the whole SDK to implement two interfaces.

**What landed:**

- `com.botmaker.plugin.api` — `StudioPlugin` (every method but `id()` a `default`), `SlotEditor`,
  `SlotContext`, `TypeRef`, `StudioServices`, `Theme`, `Capture`, `Dialogs`, `Region`.
- `com.botmaker.plugin.api.catalog` — `PaletteCatalog`, `CatalogBuilder`, `Category`, `FacadeEntry`,
  `MemberEntry`, `MemberId`, `MemberRef`, `M0`–`M5`.
- `CatalogBuilderTest`, 13 tests, all on the one claim the design rests on: that a method reference carries
  enough identity to name an *overload*.

**The decisions worth not re-litigating**, each taken against a named alternative:

- **A slot editor returns a `javafx.scene.Node`**, so the platform is pinned to JavaFX permanently. The
  alternative was a UI-factory abstraction; the editors worth writing (image template, capture source,
  launch target) are bespoke, and a factory able to express them would be larger than JavaFX.
- **A catalog entry is a method reference**, read through `SerializedLambda`. The alternatives were strings
  (nothing checks them) and class literals plus strings (the maintainer's objection: *"I don't like
  literals"*, and the same non-checking on the member half). The reference is checked by javac.
- **One void-returning shape per arity, `M0`–`M5`.** A value-returning shape beside `M1` would make `add`
  ambiguous for most real members, since a reference to a value-returning method is compatible with a
  void-returning interface. Ambiguity within one arity is the caller's, via a type witness.
- **No shaded plugin toolkit** — with the contract at interfaces and records, there is nothing to shade
  against.
- **Panels are not a surface.** Plugins contribute to the editor; they do not contribute editors.
- **No flatten-maven-plugin and no `.deps.env`**, unlike session and the SDK: this module pins no BotMaker
  upstream, so there is nothing to inject and nothing to bake.

## Deferred / next

- **The SDK's per-version catalog** (phase 6) — one frozen class per released `SdkVersion`, built as the
  previous one plus deltas, plus a `release.sh` gate: editing an already-released catalog is only possible
  when a member it names is deleted, so the edit *is* the removal signal.
- **Studio reads the catalog** (phase 7) — `SdkSurfaceService` curation switches over and `palette/SdkType`
  retires.
- **The slot editors move into the SDK** (phase 8) — thirteen of them, plus the Steam/Epic scanners moving
  to `botmaker-shared`.
- **Dynamic plugin loading** — `ServiceLoader` over a `URLClassLoader` built from the resolved artifact.
  A loader, not a redesign, and worth writing once a second plugin exists.
- **A dockable side-panel surface** — one interface (a title and a `Node`), the cheapest honest answer if a
  plugin ever needs to *show* something. Full editor views stay out.
- **Nothing here has a second implementor yet**, and one implementor proves little. The contract's real
  validation is a second plugin; until then the mitigation is that the SDK consumes it as an ordinary
  plugin.
