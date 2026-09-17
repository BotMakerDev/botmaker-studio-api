# CLAUDE.md

Guidance for working in **botmaker-studio-api**, the contract a BotMaker Studio plugin compiles against.

Read the umbrella `../CLAUDE.md` first for how the six modules fit together, and
`../docs/refactor/24-plugin-platform.md` for why this module exists at all.

## What this module is

Interfaces and records. Nothing else. It has no implementation, references no Studio type, and depends on
one artifact (`javafx-controls`, `provided`).

- `com.botmaker.plugin.api` — `StudioPlugin`, `SlotEditor`, `SlotContext`, `ValueContext`, `TypeRef`,
  `StudioServices` and the three services it exposes (`Theme`, `Capture`, `Dialogs`) plus `Region`.
- `com.botmaker.plugin.api.catalog` — `PaletteCatalog`, `Category`, `FacadeEntry`, `MemberEntry`,
  `MemberId`, and the package-private `SourceOrder`. The *result* type:
  `PaletteCatalog.of(Class<?>...)` builds it by reflection. `CatalogBuilder`, `MemberRef` and the arity
  shapes `M0`–`M5` were deleted on 2026-08-27 — see *The catalog* below.
- `com.botmaker.plugin.api.value` — the **value vocabulary**: `ValueType`, `ValueShape`, `ValueChoice`,
  `Visibility`, `Range`, `ValueCodec` and `ValueCatalog`. What a bot's *variable* can be, which is a question
  the contract answers so a plugin can own a type without the SDK granting it one.
- `com.botmaker.plugin.api.palette` — **`@Palette`**, **`@Hidden`**, `@PaletteLabel`, `@PaletteDefault`: the
  marks a plugin puts on its own classes, read **at runtime by `PaletteCatalog.of`**. All four are
  `RUNTIME` since 2026-08-27, because the plugin itself reflects on them. Their elements are plain `String`s
  on purpose — an annotation element's type must be visible from the module *declaring* the annotation, so a
  contract annotation can never take a plugin-defined enum constant.

  **Two bits, not three.** `@Palette` = **catalogued** (the recognition set: imports, "does `Point` mean this
  plugin's or `java.awt`'s"). `@Hidden` on the type = **not offered** in an insert menu; on a member = that
  member is not offered. `FacadeRole{MENU,HIDDEN,VALUE}` is deleted: nothing ever distinguished its second
  state from its third, and `VALUE` existed only to work around `@Internal` welding *not-surface* to
  *not-offered*. `@Facade` → `@Palette` (no `role`), `meta.@Internal` → `palette.@Hidden`.
- `com.botmaker.plugin.api.meta` — **`@ReplacedBy` alone**, which arrived from the SDK's `api.meta` in 1.2.0
  because a plugin renaming its own types wants exactly the machinery the SDK already had. A pointer may name
  a target in **another module** — the SDK's shims point here. It is the one annotation still `CLASS`
  retention, and for a reason: **Studio** reads it out of a jar it never loads, and `CLASS` keeps it out of
  every running bot's reflection data.

  **`@Replaces` and `@Since` were deleted on 2026-08-27.** The back edge existed because Studio holds only
  two jars at upgrade, so a bot skipping a release could not see a pointer added on an element later deleted.
  japicmp now enforces **never-delete** on `com.botmaker.sdk.api.**`, so the deprecated element and its
  forward pointer are both still in the target jar and `@ReplacedBy` alone answers every upgrade, chains
  included. `@Since` went by this repo's standing test for a gate: *the question must not already be answered
  by bytecode*.

**These are annotations, not implementation**, so they do not breach the *interfaces and records* rule above.

Studio is the host; `botmaker-sdk` is the first plugin — a *privileged default* plugin, but a plugin, with
no back door. That is what makes the contract honest: if the SDK needs something this module does not
expose, the contract is wrong, not the SDK.

## What may go on `StudioServices` — the host-only rule

**A service belongs here only when the host is the *only* possible source of it.** Not when the host happens
to have written it first, and not when a real editor needed it — that was the old test, and it is what let
the contract grow a vocabulary belonging to one plugin.

Four things pass: **which project is open** (`projectDir`/`resourcesDir`), **the theme the user chose**,
**the window a dialog is owned by**, and **the screen overlay** — an overlay goes over every window on the
screen including the host's own, hides the editor that opened it, and comes back on the right thread. Nothing
else does. A plugin can enumerate monitors, windows and emulator instances, grab pixels from any of them and
read a launcher's installed-game library, because **`botmaker-shared` is published on JitPack and any plugin
may depend on it** — so nothing shared can do is a privilege, and the SDK gets no advantage from being built
in this repository. The files under `resourcesDir()` are ordinary files.

**Six members were deleted on 2026-08-27 for failing this test**, all added weeks earlier because the SDK's
editors wanted them: the whole `Assets` interface and `StudioServices.assets()` (the project's *named
pictures* — that is `ImageTemplate`'s concept), and `Capture`'s `SourceChoice`, `Frame`, `Sample`,
`chooseSource`, `defaultSource`, `grabTargetFrame` and `sampleFromTarget` (a *capture source*, and a sampled
colour with its tolerance — `CaptureSource`'s and the vision API's). A third set never landed: `Launcher`,
`GameChoice`, `EmulatorChoice` and two `Dialogs` methods over them, written and reverted the same day. Each
was justified at the time as "the host owns the policy, the plugin owns what it is written down as" — the
`SourceChoice` split — and the flaw in that reasoning is that **the host only owned the policy because
Studio was written first**. A second plugin could not have added its own `Assets`, so the SDK was reaching
through the contract for its own API. That is the back door this module exists to close.

When a plugin's editor needs something the host has and the contract does not expose, the question to ask is
*could any plugin have built this on shared plus its own files?* If yes, it builds it. If no, and only then,
the contract grows — and it grows a **capability**, never a vocabulary: nothing here may name a concept that
belongs to some plugin's API.

**Two more passed it on 2026-08-30, both for the Remote Pilot becoming an SDK feature.** `StudioServices`
gained **`runs()`** (a `Runs`: start, stop, `isRunning`, `pid`, and listeners for run state and telemetry)
and **`status(String)`** (one line in the host's own status area). They pass on the plainest reading of the
rule in this section: the host compiled the project, holds its resolved classpath, owns the process it
launched, and draws the status bar. A plugin cannot start a bot it did not compile, learn that one exited,
or discover a pid it never spawned.

**The interesting half is telemetry, and it is the pattern to copy.** Studio holds a decoded
`com.botmaker.shared.ipc.TelemetryEvent`, and passing that record would have been shorter and would have put
one runtime's vocabulary in the contract. So `onTelemetry` hands over **one encoded frame** and this module
has no idea what is in it — the same trick `SlotContext.currentSource()` plays with Java source. *Bytes*
rather than text because the wire already is bytes and has exactly one definition; a text rendering invented
for the contract would be owned by neither end and would drift from both. **When a shape wants to cross, ask
what already has one definition and pass that.**

Both are `default`, answering `Runs.NONE` and doing nothing, so a host that runs no bots implements neither
— and a plugin never asks whether running is supported.

**A third thing passed it, and it is not on `StudioServices` at all** —
`StudioPlugin.projectClosing()`. It contributes nothing, which is why it is not a sixth surface: it is the
one fact a plugin cannot establish for itself, that the project it opened an operating-system resource for
is gone. A plugin polling for it would be guessing at a moment the host knows precisely. It exists because
the Remote Pilot is becoming an SDK feature and holds a bound port and a nested `:N` display; the rule it
sets for anything similar is that **only a resource the OS counts justifies a lifecycle** — anything
garbage collection can reclaim needs no implementation. The instance is reused across projects, so it means
*this project is over*, never *you are being discarded*.

**One member has passed that test on `StudioServices` itself, and it is the worked example of what a
capability looks like there.**
Phase 12c (2026-08-28) added `SlotContext.enclosingSource()` and `replaceEnclosingCall(String, String...)`,
because a duration editor cannot otherwise offer "wait a random amount": the SDK's humanized wait is
`Wait.between(min, max)` where the fixed one is `Wait.time(x)`, so ticking that box is a change to the
**call**, not to the value in the slot. It passes on both halves of the rule — the bot's syntax tree is
something only the host has, and the signature takes and returns Java source text exactly as `replaceWith`
does, naming no type of anyone's. Compare it with `Capture.SourceChoice`, which also arrived because a real
editor needed it and which named `CaptureSource`'s concept: *a real editor needed it* is not the test, and
never was.

**Two more passed it on 2026-08-31, and both were forced by one editor the port could not carry** — the
image-template *group* picker, whose chip row turned out not to be a slot editor at all.

**`SlotContext.run()` and `SlotRun`** — several sibling slots edited as one. A `SlotContext` is one argument
of one call, which is right for almost everything and wrong for a value the author writes as a **run** of
arguments: three pictures to match any of, four keys to try in order. An editor confined to a single
argument can change one element and can never add or remove one, so it has to hand back the whole run —
`replace(List<String>, String...)`. It passes on both halves. **Everything in it is opaque Java source**:
`elements()` are the expressions as they stand, and even the narrowing is expressed that way. And what the
host contributes is what only the host has — that these arguments *are* one list, plus two facts about the
code around them: `minimum()`, how few elements the surrounding source still compiles with (a guarded branch
needs one), and `allowed()`, the only element sources it will still accept.

**`allowed()` returning source rather than decoded values is the load-bearing detail.** Studio's own version
of this narrowing decoded the enclosing group into template *paths* — which meant the host reading a
plugin's vocabulary out of a plugin's expressions. Element sources are a purely syntactic answer: the host
lists the arguments of the list a branch is narrowing against without knowing what any of them mean, and the
plugin, which does know, compares or parses them itself. **When the host must describe a plugin's values,
describe them as the text they are written as.**

**`SlotEditor.preview(ValueContext)`** — a small, non-interactive picture of one value, `default null`. The
host shows a value in one more place than it edits one: beside a **declared choice**, in the list an author
picks from. `create` is wrong there (a live control in a list of options) and plain text is wrong too
wherever the stored string is a *reference* rather than the value — a template name is not a picture,
`#3A7F2B` is not a colour — because offering a gallery to pick a choice from and then listing the result as
raw text puts the decoding back on the person the choices exist for. It is not a sixth contribution surface:
it reuses the matcher `matches()` already provides, and its default is exactly today's behaviour for every
type the host does not answer itself, so a type costs nothing by not implementing it.

**And one passed it on 2026-09-01, which is the clearest statement of the rule so far because the thing it
splits was one class in the editor for a year.** **`StudioServices.sources()`**, returning a `Sources`:
`find(List<String> needles)` and `replace(Map<String,String>, historyLabel, reviewNote)` over the bot's own
Java.

Studio's `TemplateReferences` renamed an image template and carried every block that named it. Half of that is
host work no plugin can do — the open buffers are editor state, the walk knows which files the bot owns, and
`@NeedsReview` plus the Project History snapshot are the host's own undo model. The other half is that
`ore.png` is spelled `Templates.ORE`, which is `ImageTemplate`'s concept and **nobody else's** — so the host
was carrying one plugin's vocabulary, and a second plugin renaming a concept of its own had no way to ask for
the same service. Cutting it at this interface leaves each side with what is actually theirs. The picture
library moved to the SDK plugin the same day, and it was the last thing in Studio that knew what a picture is
called.

**The needle is the part to copy.** It is a **sequence of Java tokens**, matched as tokens and never as text:
`Templates.ORE` matches `Templates . ORE` and does not match `Templates.OREX` or `MyTemplates.ORE`. That is a
fact about Java, which is why it names nobody's concept — compare `Assets`, which had to say the word
*picture*. Not a regex, deliberately: a regex hands every plugin the power to corrupt a user's source with a
bad pattern, and pins one flavour of regex semantics into a surface only a major release may break. And text
rather than an AST, which is rule 3 above arriving from the other direction — the file a rename most needs to
reach is the one the user has open and half-edited, and it does not parse.

`Use` is **host-constructed only**, and the record says so in its own javadoc: a plugin that had called its
canonical constructor would take a `NoSuchMethodError` the day a component is added. That is why replacements
are a `Map<String,String>` and not a `Replacement` record — see `docs/refactor/25-compatibility.md` §2.

## Parameter data (2026-09-10) — the window is the host's, the file behind it is not

`StudioPlugin.parameterRows(String groupId)` and `parameterEdited(ParameterEdit)`, with `ParameterRow` and
`ParameterEdit` beside `ParameterGroup`. The seventh surface, and the first one where a plugin hands over
**project data** rather than something it decided at build time.

It exists because the Parameters window read one plugin's storage format itself. `ParameterGroup` had always
said the right thing — a plugin declares the *section*, a user declares the values in it — and then the host
parsed the values out of `activities.json`, a file that predates the plugin system. A second plugin could not
have had parameters at all, and the host knew what a `tag`, a `visibility` and a stored duration were. So
this is the same cut as `StudioServices.sources()`: the host keeps the window, the undo, the rendering and
the ordering; the plugin keeps the file, the meaning of the text and when it is written.

**Every component of a row is already vocabulary this module owns** — a name, a `ValueChoice`, a stored
`List<String>`, a `Visibility`, a declared option set, a `Range`, a category out of
`ParameterGroup.categories()`. Nothing plugin-specific crosses, no `Class<?>` crosses, and the value is
**text**, exactly as it is through `ValueCodec`. That is rules 2 and 3 above, satisfied by not needing an
exemption.

**The pair is shaped as a class and a record on purpose, and it is the clearest example of compatibility
trap #2 in the module.** A plugin *builds* a row, so `ParameterRow` is a final class with a builder — a
record's canonical constructor is part of its binary signature and a component added later throws
`NoSuchMethodError` in every plugin already compiled. The host *builds* an edit and a plugin only reads it,
so `ParameterEdit` is a record and may grow a component safely. The trap is a ban on records a **plugin**
constructs, not a ban on records; `Use` (host-constructed, above) is the same call made the same way.

**`parameterEdited` answers the row as stored**, `Optional.empty()` for a row this plugin does not own. That
one return type is what carries a clamp, a normalisation and a refusal without the host modelling any of
them: the window renders what comes back, so a value pulled to its `Range` or a duration spelled canonically
shows up by itself, and a plugin that will not accept the edit answers the row it still holds.

**`projectOpened(StudioServices)` arrived with it (2026-09-10) and is not a surface** — it is
`projectClosing()`'s mirror, and the same kind of fact: a plugin is constructed once by `ServiceLoader` and
then serves whatever the host binds, so *which* project it has is something only the host can tell it. It is
here because a data surface takes a group id and nothing else. That was a choice: rows are asked for every
time a window is drawn, and threading the host through each call would put it in every future data surface
too. So the host names the project once per bind and the plugin reads its own file when asked. **Nothing
expensive may happen in it** — a project open must not pay for a window nobody has looked at — which is the
rule `AbstractStudioPlugin`'s four lazy builders already follow.

**Asked, never pushed.** There is no listener, because a listener is a capability with a lifecycle — a
registration, a thread, an unsubscribe — and the only thing it buys is a plugin's own dialog changing a value
behind the window's back, which no plugin has yet.

**There is no declaration half any more (2026-09-17).** `parameterDeclared(ParameterDeclaration)` and its
record are deleted: a *user* parameter is a `@Param` static field in the bot's own Java, and the host adds,
renames, retypes, refiles and removes one by editing the syntax tree. A plugin's rows are the plugin's own —
an activity's enable flag, a capture target — and a plugin declares those in its own code, where a wire form
for *here is the row I want* buys nothing. What is left of the surface is `parameterRows(String)` and
`parameterEdited(ParameterEdit)`: read the rows, change a value.

**The rule the deleted method was an instance of still stands**: state the desired end value and let the
owner reconcile it, rather than adding a verb. A verb would have made the contract learn what retyping means,
which is a rule about a plugin's own value types; an enum of verbs would have frozen today's list into a
surface only a major release may extend. Reach for it whenever a surface looks like it needs a verb —
`parameterEdited` is the one that remains, and its answer is the row *as stored*, which is how a clamp, a
canonical spelling or a pruned value reports itself.

## The three rules that are easy to break

**1. Every method but `StudioPlugin.id()` is `default`, and stays that way.** A bot's source can be
rewritten when the SDK changes — Studio holds an AST of it. A plugin's compiled `.class` files cannot be
rewritten by anyone. So a new member here that an already-built plugin cannot survive is a **major** change,
and only a Studio major release may make one. This is the whole reason the module is separate from the SDK:
it must be allowed to move slower.

**2. Nothing from a plugin may cross as a `Class<?>` the host is expected to load.** The host resolves types
out of the *bot's* classpath, not its own, so a type may be a different version of itself or absent
entirely. `TypeRef` crosses as names and compares by fully-qualified name; that is the comparison that is
actually true across two classloaders. (Inside a catalog a `Class<?>` *is* used — but the plugin holds it,
and it is the plugin's own class.)

**3. No syntax tree, in either direction.** `SlotContext.currentSource()` is a `String` and
`replaceWith(String, String...)` takes one. This was not a simplification imposed on the host — it is what
the host already did, since fifteen of its nineteen built-in editors handed back source text and let it
re-parse. Keeping it that way is what stops the host's parser from becoming plugin surface.

## The value vocabulary, and why `ValueType` is not an enum

`ValueType` is a **final class whose identity is its persisted `id()`**, reached through `ValueType.of(id)`
and registered in a `ValueCatalog`. An enum would have been shorter and is wrong for the same reason the whole
module exists: a plugin wanting a `Channel` variable would need a constant added to somebody else's enum.

- **Compare by `id()`, never by object identity.** Two plugin classloaders each hold their own copy of a
  class; the id is what a project file holds and the only comparison that is true across them.
- **`ValueType.unknown(id)` is a supported state, not an error path.** A type nothing registered keeps its raw
  `List<String>`, renders read-only and declines to emit. That is what a project opened without one of its
  plugins looks like, and refusing the file or coercing the value would destroy a user's data because a jar is
  missing. An **absent** id is different and reads as text — a field older than the vocabulary.
- **`ValueCodec<T>` is per item.** Shape (`ONE`/`ONE_OF`/`ANY_OF`/`OPEN_LIST`) is composed above it by
  `ValueCatalog.initializer`, so a codec is written once and serves all four. `T` never crosses to the host —
  only `literal(parse(wire))` behind a wildcard capture — which is what keeps rule 2 above true for values.
- **A type is asked for by the Java class it is, not by its id** (2026-09-09). `ValueCatalog.forJava(Class<?>)`
  indexes the registrations on `ValueType.javaName()` — the import when there is one, the source spelling
  otherwise — so a plugin author writes `Duration.class` and never writes `DURATION`. It does not weaken rule
  2 above: the class is **read, never loaded**, only its names are taken off the object the caller already
  holds, and nothing here compares `Class` objects. A wrapper resolves to its primitive, which is what makes
  a list of them askable the same way a single one is. **The index is a function only because the builder
  makes it one** — `add` throws when a second type claims a Java name, the same refusal one id registered
  twice already got. Across two plugins it is a report (`javaClashesWith`) and not a throw, because both ids
  are real, both projects are valid, and dropping the loser would retype a user's stored variable.
- **`ValueCatalog.merge` is left-biased and never throws.** Deliberately unlike a generation collision, which
  is a hard error: refusing to merge would break every project that has a plugin installed.
- **No Jackson here, and none is coming.** The vocabulary declares the wire *form* — an id out, a total
  factory back — and whoever owns the file supplies the parser (the SDK's `internal/authoring/ValueJson`).
  Adding a JSON library to this module would impose it on every plugin and tie the contract to its
  compatibility rate.
- **`ValueType` and `ValueCatalog.Entry` are classes with builders, not records**, for trap #2 of
  `../docs/refactor/25-compatibility.md`: adding a component to a public record changes its canonical
  constructor descriptor, which is `NoSuchMethodError` in every already-compiled plugin. `ValueChoice` and
  `Range` *are* records and are therefore **frozen** — their components may not grow.

## The catalog, and why it is reflection

`PaletteCatalog.of(Mouse.class, Keyboard.class, …)` — one class literal per facade, and **members are
discovered, never named**. Every public declared method of a `@Palette` class is offered unless something on
it says otherwise, grouped by name, lead shape chosen by `@PaletteDefault` or else fewest parameters, labels
from `@PaletteLabel`, whole name dropped if any overload is `@Hidden`.

**It used to be method references** — `Mouse::click` through a `MemberRef extends Serializable` and a
`SerializedLambda`, built by `CatalogBuilder` with one arity shape `M0`–`M5` per parameter count. All of that
was deleted on 2026-08-27 along with `botmaker-plugin-processor`, and the property it was defended on does
not need saving: *a catalog naming a renamed member does not compile* was answering a problem that only
exists when something names members. Nothing does now. What stays javac-checked is the **class list**,
because it is written as class literals.

The processor also cost something a plugin author outside this repository could not pay: a pom that omitted
`<annotationProcessorPaths>` got no catalog, and nothing said why. Reflection needs no build configuration.

**Three things about `of` that are decisions rather than details:**

- **It degrades, never throws.** Two `@PaletteDefault`s on one name, a `@PaletteLabel` on a `@Hidden` member,
  two facades disagreeing about one category's label, a class with no `@Palette`, a facade whose members
  cannot be read at all (`LinkageError` from an optional dependency the host did not resolve) — each is
  collected into **`problems()`** and the rest of the catalog is built. The precedent is `ValueCatalog.merge`
  and the rule behind both is the same: **no malformed catalog may be the reason a project will not open.**
- **Member order is the class file's, not `getDeclaredMethods()`'s.** javac writes the `methods` table in
  source order and reflection promises nothing, so `SourceOrder` parses the class file's constant pool and
  methods table to recover the author's ordering — the one processor capability reflection alone lacks, and
  the reason the switch reproduced the generated catalog's menus exactly. Every failure path returns an empty
  list and the caller sorts alphabetically: the worst case is a cosmetic menu order.
- **Constructors are not catalogued.** Reflecting them put an `<init>` entry under seven *offered* static
  facades whose public constructor exists only because nobody wrote a private one, and a palette entry
  inserts a call. `MemberId` keeps `of(Constructor)` and `CONSTRUCTOR` for a plugin that wants one.

**What a catalog does not answer: presence.** It describes the build it was reflected from, not the jar the
bot resolves — so whether a member exists in the jar a bot actually pins stays with the host's ClassGraph
scan of that jar, and the catalog answers only curation, order and labels. The two compose as an
intersection, which fails in the safe direction: an old pin may be offered slightly less than it truly had,
never more.

## A plugin does not write files — and this is where that was tried

Deleted on 2026-08-29, whole: `com.botmaker.plugin.api.scaffold` (`@Scaffold`, `@ClassName`, `@EnumValues`,
`@Editable`, `Seeding`), `catalog.ScaffoldCatalog`/`ScaffoldEntry`/`ScaffoldPlan`, and
`StudioPlugin.scaffold`/`seedings`. It is recorded rather than edited away, because the reasoning that
produced it is a good one and will be produced again.

A **seed** was a real class in the plugin's own build, marked with what a host may substitute, written into a
user's project once and thereafter *maintained* at the marks — the type name, and the constants of a
substituted enum. It replaced `botmaker-sdk`'s `SourceEmitter`, which built nine `.java` files as Java
strings that nothing checked until somebody ran the generator, and every step of it was an improvement on
what it replaced: javac checked the seed, the class list was class literals, `ScaffoldPlan` validated without
a parser, the key told a rename from a delete-plus-create.

**The flaw is one level up, and it is the same flaw as `Assets` above.** Replacing one code generator with a
generalised one made the wrong thing a surface: *any* plugin could now own files inside somebody's source
tree. A file a plugin owns is a file its user cannot freely edit, rename or delete, and everything the
mechanism grew — a key ledger persisted in the project, a reconciler, a rename engine rewriting the user's
own references — was cost paid to work around that one fact. The rule that replaces it:

> **A project's structure belongs to the user. A plugin contributes methods a user calls.**

Everything a seed was for has an answer on this side of the line. Behaviour is a static method: an activity's
body is `Activities.define("Mining", ctx -> …)` written wherever the user likes, not a generated subclass.
Anything that followed *as a whole* from project data was data all along and is read at runtime — which was
already the seed surface's own rule, stated in its javadoc, and is what the rest of the file always failed.
The one thing genuinely lost is the compile check a per-activity `Outcome` enum bought; it is replaced by a
host **picker** on the argument, which is a contribution surface this module already has.

Two smaller things learned there, kept because they generalise:

- **`javax.lang.model.SourceVersion`, never a hand-rolled keyword list.** It covers the three cases such a
  list always misses — `true`, `false` and `null` are literals rather than keywords, and none is a name. It
  lives in the `java.compiler` module, which is present: Studio's jpackage build bundles a **full** JDK
  runtime. There are already three hand-rolled lists in this project (Studio's `VariableNames`,
  `FunctionDraft`, and a partial one elsewhere); do not add a fourth.
- **Reflection promises no order**, and `getDeclaredClasses()`/`getDeclaredMethods()` order varying by JVM is
  how a plugin author's own test passes locally and fails in CI. Where order matters to a *human* — a
  palette's members — `SourceOrder` recovers the author's; where a thing is addressed by key, sort it.

## japicmp, and why it is legitimate here

`mvn verify` compares this build against `botmaker.japicmp.baseline` and **fails on any binary- or
source-incompatible change**, with no ignore list and no exemption annotation. It catches the trap
`../docs/refactor/25-compatibility.md` lists as checked by nothing: adding a component to a public record
changes its canonical constructor descriptor and throws `NoSuchMethodError` in every already-compiled
plugin — source-compatible, binary-incompatible, and until 2026-08-27 carried by a Javadoc sentence.

The SDK's August japicmp gate was deleted because **CI cannot tell an intended break from an accident: it
cannot see the version.** That is an objection to a *conditional* rule. Here the rule is unconditional —
only a Studio major release may break a plugin, and that release edits this block — so there is nothing to
distinguish and the objection does not apply. The module has never been released, so the baseline does not
resolve yet and `ignoreMissingOldVersion` reports instead of failing; set the baseline to the previous tag in
every release commit from the first one onward.

## Style

`../docs/refactor/00-conventions.md` applies. Two things specific to this module:

- **Javadoc is the deliverable.** A plugin author has this module's Javadoc and nothing else — no source of
  the host, no examples inside the repository. Every public member says what it is *for*, and every design
  decision that constrains an implementor (the callback that is not invoked on cancel, the label that falls
  back to the member name, the empty catalog meaning "declined to curate") is written down where they will
  read it.
- **No utility methods that are not about the contract.** `Capture.toFxImage` is here because capture is
  where the AWT image comes from; a general-purpose helper that merely happens to be useful is not.

## Building

```bash
mvn test        # PaletteCatalogTest (10) + ValueVocabularyTest (12) — the module's only behaviour
mvn verify      # the above plus japicmp against botmaker.japicmp.baseline (see above)
mvn install     # com.github.LiQiyeDev:botmaker-studio-api:0.0.0-SNAPSHOT
```

Published through JitPack, which serves each git tag under `com.github.LiQiyeDev` regardless of this pom's
`groupId`/`version` (so the version is cosmetic). **The maintainer owns the publish** — releases are cut
from the umbrella with `../release.sh --studio-api <version>`.

Unlike `botmaker-session` and `botmaker-sdk` this module runs **no `flatten-maven-plugin` and has no
`.deps.env`**: flatten exists to bake a `-D`-injected `${botmaker.*.version}` into the published pom, and
this module pins no BotMaker upstream to inject. Its `jitpack.yml` is a plain `mvn install`.
