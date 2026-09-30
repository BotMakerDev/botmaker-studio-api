# CLAUDE.md

Guidance for working in **botmaker-studio-api**, the contract a BotMaker Studio plugin compiles against.

Read the umbrella `../CLAUDE.md` first, `../docs/refactor/24-plugin-platform.md` for why this module exists at
all and `../docs/refactor/25-compatibility.md` before changing any public member. This file states what is
true now. The mechanisms that were here and went — `Assets`, `Capture`, `Sources`, `PluginSource`, the
parameter-data surface, the scaffold/seed surface, `ValueType`/`ValueCodec`/`ValueCatalog`, the catalog's
method-reference builder, `@Replaces`/`@Since` — and why each went are in
`../docs/refactor/31-umbrella-history.md` (*contract*); the text this file carried until 2026-09-28 is
`git show ad93451:CLAUDE.md` in this repository. Search there before re-proposing one.

## What this module is

Interfaces, records, annotations, and the step classes that build a declaration. It has no implementation of
a host, references no Studio type, and depends on one artifact (`javafx-controls`, `provided`).

**One runtime class: `managed.ManagedValues`**, the bot-side half of `@Managed`
(`claim(ManagedValue<T>, Consumer<? super T>)`, `install(Class<?>...)`). It runs inside a bot, which already
has this jar through any plugin that puts `@Managed` there. Add nothing else of the kind: the test is *does
every plugin with a bot-side half need it, and does it name nothing but this module and the JDK*.

Studio is the host; `botmaker-sdk` is the first plugin — a *privileged default* plugin, but a plugin, with
no back door. If the SDK needs something this module does not expose, the contract is wrong, not the SDK.

### The declaration steps

Every surface a plugin fills is declared through small final step classes, each offering only the moves
valid next, so the compiler walks an author through a declaration and an incomplete one does not compile (the
maintainer's call, over a toolkit builder: a plugin needs no toolkit to declare itself):

- **The plugin**: `StudioPlugin.id(ID).named(NAME).types(() -> …).parts(…).editors(…).values(…).toolbar(…)
  .recorded(…)`, a `PluginDeclaration` handed to `DeclaredPlugin`'s constructor. Each surface is a supplier,
  asked when the host asks and cached nowhere: the lists are `static final` constants, and a list built in
  the constructor would load its classes — JavaFX among them — on a headless host.
- **A type**: `PluginType.value(X.class)` → `fresh(…)` | `firstConstant()` | `filledBy(Owner::method)` →
  `editor(() -> E::draw)` → optional `preview(…)` → `writtenAs(Owner::factory, X::part, …)` |
  `writtenAsEach(…)` | `writtenAsRecord()` | `writtenAsConstant()` | `writtenAsLiteral()` | `writtenAsParts()`
  (`TypeSteps`, `CallSteps`, results `DeclaredType`/`DeclaredCallType`). **A part**: `ComponentType.part(X.class)
  .writtenAs(…)` (`DeclaredCall`, with `.constants(VALUE)`, and `.components`/`.build` for the one hand-made
  part, the SDK's activity body).
- **Factories are method references, never names.** `Ref.Of0`–`Of10` are serializable functional interfaces;
  `Ref.resolve` reads the `SerializedLambda` into the `Method` or `Constructor` once, when the declaration is
  built, and refuses a lambda. One typed `writtenAs` per arity, so the accessors' types pick an overloaded
  factory (`LocalDate::of`, `Color::new`); past ten, a plugin declares its own interface extending `Ref` and
  uses `writtenAs(Ref, Function...)`. `build` is invoking the factory on the parts coerced to its parameters,
  `null` when they do not fit or it throws.
- **A managed value**: `ManagedValue.method(id).in(holder).holds(T.class, initial).because(reason)`,
  `.notCreated()` instead of `in` for one the host may open and never create, and
  `ManagedValue.openSet(id).in(holder).because(reason)`. It is typed and declared once: a plugin keeps one
  constant per value and uses it in `managedValues()`, in `ManagedValues.claim`, and through the toolkit's
  `ManagedHandle`. **A recorded value**: `RecordedValue.of(T.class).at(Finder::find)`.
- **A slot editor**: `SlotEditor.onParameter(Annotation.class)` | `forType(X.class)` | `when(predicate)` →
  `.draw(() -> E::draw[, () -> E::preview])` (`EditorSteps`). A value the type cannot tell apart is told apart
  by a `RUNTIME` annotation on the parameter it is passed to (`launchSteam(@SteamAppId String)`), matched by
  the annotation's binary name through `SlotContext.parameter()`; an editor reads its settings off it
  (`@Setting(label, min, max, …)`). A preview is a small, non-interactive picture of one value, for a value
  shown in a list of choices rather than edited.
- **A toolbar item**: `ToolbarItem.id(ID).label(…).tooltip(…).in(group, order)` → optional
  `.enabledWhen(…)`/`.icon(…)` → `.onPress(() -> MyWindow::open)` (`ToolbarSteps`). The press is a `Pressed`,
  a supplier of the handler, for `Drawn`'s reason: the item list is built headless.
  `DeclaredPlugin.toolbarItems()` is `final`.

Implementing the interfaces by hand still works and the host cannot tell the two apart; the steps are how
nobody has to know which methods to override, which may answer `null`, or how to name a factory without a
string.

### Two strings that stay

Both were examined on 2026-09-28 and kept; do not propose removing either without new facts.

- **`Ref.member(owner, name, params)`** names the one factory javac cannot reference: the SDK's
  `CaptureSource.region`, where a static `region(src, r)` and an instance `src.region(r)` share a name and an
  arity. The SDK's never-delete keeps both for ever, so a new-named factory would still leave this one.
- **The SDK's flow activity body is the text `Collect::body`** (`FlowTypes`). It is not a plugin writing
  Java: it goes through the host's own source-leaf path (Studio's `ValueWriter.ofClass`), which parses it into
  a tree, and renames follow bindings in the real `Sdk.java`. A contract `MethodName` type was considered and
  declined: one contract type, grammar changes and a flow-editor rewrite to save about forty lines.

### A value a plugin declares is built through steps or a static factory, never a public constructor

A record's canonical constructor is public and changes when a component is added, which throws
`NoSuchMethodError` in every compiled plugin that called it. So `ToolbarItem` and `ManagedValue` are final
classes whose constructors only their steps reach, and `SlotRun.Element`, which an editor hands back and has
no steps, is built with `Element.of(value, source)`. A record is still right for what the **host** builds and
a plugin only reads (`Bounds`, `ActionContext.Area`, `PluginValues.Use`, `RecordedValue.Spot`,
`Dialogs.Choice`). A new value a plugin constructs gets steps or a factory from its first commit.
`botmaker-plugin-host`'s `ContractLinks` is the load-time half: a plugin linking a contract member this build
lacks — or one made package-private — is refused with the member named (`25-compatibility.md` §1–2).

## The packages

- `com.botmaker.plugin.api` — `StudioPlugin`, `DeclaredPlugin`, `PluginDeclaration`, `StudioServices` and
  what it hands back (`Theme`, `Dialogs`, `Runs`), and **`StyleClasses`**: the style-class names the host's
  stylesheet defines, constants only. Only the host knows what its stylesheet names, and it is the one thing a
  plugin's widgets and Studio's own both need — which is why it is here rather than a reason for Studio to
  depend on the toolkit. `StyleClassesTest` in Studio holds the stylesheet to it.
- **One package per contribution surface**: `…api.slot` (`SlotEditor`, `EditorSteps`, `SlotContext`,
  `SlotRun`, `ValueContext`, `TypeRef`, `Bounds`), `…api.toolbar` (`ToolbarItem`, `ToolbarSteps`, `Pressed`,
  `ToolbarGroup`, `EnabledWhen`, `ActionContext`), `…api.source` (`ManagedValue`, `PluginValues`),
  `…api.record` (`@Records`, `Gesture`, `RecordedValue`), and `…api.params` (`@Param`) and `…api.managed`
  (`@Managed`, `ManagedValues`), the two annotations that sit on a **bot's** own declarations.
- `…api.value` — `PluginType<T>` (the class, `fresh()`, optional `freshCall`/`preview`), `EditableType<T>`
  (adds `editor(ValueContext)`, never `null` — a type its owner draws), `ComponentType<T>` (`componentTypes`,
  `components`, `build`, `factory`, an `Executable`), and the steps that declare them (`TypeSteps`,
  `CallSteps`, `CallShape`, `DeclaredType`, `DeclaredCall`, `DeclaredCallType`, `Ref`, `Drawn`).
- `…api.catalog` — `PaletteCatalog`, `Category`, `FacadeEntry`, `MemberEntry`, `MemberId`, and the
  package-private `SourceOrder`. `PaletteCatalog.of(Class<?>...)` builds it by reflection.
- `…api.palette` — **`@Palette`**, **`@Hidden`**, `@PaletteLabel`, `@PaletteDefault`: the marks a plugin puts
  on its own classes, `RUNTIME` because the catalog reflects on them. Their elements are plain `String`s on
  purpose — an annotation element's type must be visible from the module *declaring* the annotation, so a
  contract annotation can never take a plugin-defined enum constant. `@Palette` = **offered** (its own menu
  entry); the **catalogue** (the recognition set: imports, "does `Point` mean this plugin's or
  `java.awt`'s", a variable's member list) is the offered classes plus every public type of the same jar
  their non-`@Hidden` members reach, derived by `PaletteCatalog.of` (2026-09-30). `@Hidden` is member-only:
  that member is not offered and reaches nothing. Facades and categories are listed alphabetically by label;
  `@Palette.order` is deleted.
- `…api.meta` — **`@ReplacedBy`** (a rename's forward pointer; `CLASS` retention, because Studio reads it out
  of a jar it never loads and it stays out of every running bot's reflection data) and **`@Refactor(value,
  done)`**, the mark on a function where a refactor wrote a value the user never chose.

## What may go on `StudioServices` — the host-only rule

**A service belongs here only when the host is the *only* possible source of it.** Not when the host happens
to have written it first, and not when a real editor needed it — that was the old test, and it is what let
the contract grow a vocabulary belonging to one plugin (`Assets`, `Capture`'s source choice, `Sources`'
needles: see the history).

What passes: **which project is open** (`projectDir`/`resourcesDir`), **the theme the user chose**, **the
window a dialog is owned by** (`dialogs()`), **the bot as a process** (`runs()`: start, stop, `isRunning`,
`pid`, run-state and telemetry listeners — the host compiled the project and owns the process it launched),
**the status line** (`status(String)`), and **a plugin's own values as compiled Java in the bot's source**
(`pluginValues()`, below). Nothing else does. A plugin can enumerate monitors, windows and emulator
instances, grab pixels from any of them and read a launcher's installed-game library, because
**`botmaker-shared` is published and any plugin may depend on it** — so nothing shared can do is a
privilege. The files under `resourcesDir()` are ordinary files.

When a plugin's editor needs something the host has and the contract does not expose, ask *could any plugin
have built this on shared plus its own files?* If yes, it builds it. If no, and only then, the contract grows
— and it grows a **capability**, never a vocabulary: nothing here may name a concept that belongs to some
plugin's API.

**When a shape wants to cross, pass what already has one definition.** `Runs.onTelemetry` hands over one
encoded telemetry frame, and this module has no idea what is in it: the wire already is bytes with exactly
one definition, and a decoded record would have put one runtime's vocabulary in the contract. `runs()` and
`status` are `default` (`Runs.NONE`, nothing), so a host that runs no bots implements neither.

**`StudioPlugin.projectClosing()` is not a surface.** It is the one fact a plugin cannot establish for
itself: that the project it opened an operating-system resource for (the Remote Pilot's bound port and nested
display) is gone. **Only a resource the OS counts justifies a lifecycle.** The instance is reused across
projects, so it means *this project is over*, never *you are being discarded*.

**`pluginValues()` rewrites one expression inside a file the host did not write**
(`../docs/refactor/33-plugin-java.md`). A bot holds a plugin's values as `@Managed("id")` methods in its own
`src/main/java/<bot package>/plugins/<last id segment>/`, and the host never touches the class around them.
`open(id)` hands back a **`ValueContext`**, so a slot, a Parameters row and a `@Managed` value are all edited
through one interface and no second way to edit a value exists; a body that is not a single
`return <expression>;` answers empty and is shown read-only with the reason. An open set (`@Managed` on a
type, the SDK's `Pictures`) changes **by binding**: `members`, `add`, `uses`, `rename`, `repoint`, `remove`,
each total and refused when the bot would stop compiling. The host rewrites; the plugin says which constant.
`PluginValues.Use` is host-constructed only.

## The rules that are easy to break

**1. Every method but `StudioPlugin.id()` is `default`, and stays that way.** A bot's source can be
rewritten when the SDK changes — Studio holds an AST of it. A plugin's compiled `.class` files cannot be
rewritten by anyone. So a new member here that an already-built plugin cannot survive is a **major** change,
and only a Studio major release may make one. This is the whole reason the module is separate from the SDK:
it must be allowed to move slower.

**2. Nothing from a plugin may cross as a `Class<?>` the host is expected to load.** The host resolves types
out of the *bot's* classpath, not its own, so a type may be a different version of itself or absent
entirely. `TypeRef` answers `is(Class)`/`isSubtypeOf(Class)` by binary name and hands over no name at all;
that is the comparison that is actually true across two classloaders. A slot's call is
`SlotContext.enclosingExecutable()`, which the host loads on the **plugin's** loader — so it is a class the
plugin could have named — and `SlotEditor.onParameter` compares the parameter's annotations by name.

**3. No syntax tree and no Java text, in either direction.** An editor reads a value
(`ValueContext.value(Class)`) and writes one (`set(Object)`); the host owns every character of syntax. The
one string left is `ValueContext.source()`, to *show* what the grammar could not read, never to parse. A
fresh value the bot re-evaluates is `PluginType.freshCall()`, a `Method` the host writes with its import.
**`SlotContext.siblingRun()`** hands several sibling arguments over as one list of values
(`SlotRun.Element`s, `null` value when unreadable, kept exactly as written when handed back), with what only
the host knows: `minimum()` (how few elements still compile) and `allowed()`.

## The value vocabulary — one declaration per type

A plugin declares a type **once**, as a `PluginType<T>` — an `EditableType<T>` when it draws the type itself
(a plain one is drawn by another plugin's `forType` or the host's fallback, and `plugin validate` refuses a
type nobody draws), plus a `ComponentType<T>` when its Java is a call.

- **A fresh value is a `T`, not a string.** `fresh()` returns `new Point(0, 0)`, so a renamed class or a
  removed constructor fails the plugin's own compile, not a user's file.
- **Nothing parses.** The host owns every character of syntax in both directions, which is what lets one
  grammar (Studio's `plugin/grammar/`) serve every plugin.
- **The identity is the Java class, not an id.** The host indexes declarations by the class's canonical name,
  **read, never loaded**. Two plugins may not *own* one type; offering an editor for somebody else's is
  `SlotEditor.forType`, and the host asks the user which to use.
- **A type no loaded plugin declares is a supported state, not an error path.** The value keeps the
  expression its author wrote, renders read-only and is never rewritten.
- **`build(components(v))` equals `v` is the law**, and `botmaker plugin validate` checks it against
  `fresh()`. The half nobody is forced to write is the half that rots.
- **`ComponentType.factory()` is an `Executable`**: a constructor, a public static method, or an instance
  method on part 0 that the host reads and never writes (`Precision.TIGHT.minArea(400)`)
  (`../docs/refactor/35-typed-value-reader.md`).
- **No Jackson here, and none is coming.** A serialisation library here would be every plugin's, at its
  compatibility rate.

## The catalog, and why it is reflection

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

## japicmp, and why it is legitimate here

`mvn verify` compares this build against `botmaker.japicmp.baseline` and **fails on any binary- or
source-incompatible change**, with no ignore list and no exemption annotation. It catches the trap
`../docs/refactor/25-compatibility.md` lists: adding a component to a public record changes its canonical
constructor descriptor and throws `NoSuchMethodError` in every already-compiled plugin.

The SDK's August japicmp gate was deleted because CI cannot tell an intended break from an accident. Here
the rule is unconditional — only a Studio major release may break a plugin, and that release edits this
block — so there is nothing to distinguish. The baseline is set to the previous tag in every release commit
(`Japicmp.bump`, never backwards).

**The baseline is pinned to `v0.4.0`, ahead of the tag** (2026-09-30). `v0.3.0` is released; what has broken
since (`@Palette.order` and the type-level `@Hidden` removed) is the next release, which must therefore be
**`--studio-api 0.4.0`**. An absent baseline tag reports and passes. **When a baseline "passes", look at the first line of `target/japicmp/japicmp.diff`**:
`against` followed by nothing means nothing was compared.

## Style

`../docs/refactor/00-conventions.md` applies. Two things specific to this module:

- **Javadoc is the deliverable.** A plugin author has this module's Javadoc and nothing else — no source of
  the host, no examples inside the repository. Every public member says what it is *for*, and every design
  decision that constrains an implementor (the callback that is not invoked on cancel, the label that falls
  back to the member name, the empty catalog meaning "declined to curate") is written down where they will
  read it.
- **No utility methods that are not about the contract.** A general-purpose helper that merely happens to be
  useful goes in a plugin or the toolkit.

## Building

```bash
mvn test        # the step tests, PaletteCatalogTest, SlotEditorTest, TypeRefTest, ManagedValuesTest,
                # StudioPluginDefaultsTest, the context defaults tests — the module's only behaviour
mvn verify      # the above plus japicmp against botmaker.japicmp.baseline (see above)
mvn install     # com.github.LiQiyeDev:botmaker-studio-api:0.0.0-SNAPSHOT
```

Published through JitPack, which serves each git tag under `com.github.LiQiyeDev` regardless of this pom's
`groupId`/`version` (so the version is cosmetic). **The maintainer owns the publish** — releases are cut
from the umbrella with `../release.sh --studio-api <version>`.

Unlike `botmaker-session` and `botmaker-sdk` this module runs **no `flatten-maven-plugin` and has no
`.deps.env`**: flatten exists to bake a `-D`-injected `${botmaker.*.version}` into the published pom, and
this module pins no BotMaker upstream to inject. Its `jitpack.yml` is a plain `mvn install`.
