# The declaration steps

Every surface a plugin fills is declared through small final step classes, each offering only the moves
valid next, so the compiler walks an author through a declaration and an incomplete one does not compile (the
maintainer's call, over a toolkit builder: a plugin needs no toolkit to declare itself):

- **The plugin**: `StudioPlugin.declare().types(MyTypes.class).values(MyValues.class).editors(MyEditors.class)
  .toolbar(MyToolbar.class)`, a `PluginDeclaration` handed to `DeclaredPlugin`'s constructor. **Every surface
  is named by holder classes, and that is the one way** (2026-10-28): each step takes its own holder interface,
  whose one method says what it gives — `TypeHolder.types()` (and the default `components()` for parts),
  `ValueHolder.values()`, `EditorHolder.editors()`, `ToolbarHolder.toolbar()`, `RunOverlayHolder.runOverlay()`,
  `AssistantHolder.tools()`, and for the two single surfaces `OverlayHolder.overlay()` and
  `CatalogHolder.catalog()`, each refusing a second holder at the step. So `.toolbar(MyTypes.class)` does not
  compile, and the interface tells the author what to write; the method names differ so one class may give
  several. A holder is a concrete class with a constructor taking no arguments (package-private is fine);
  several holders of one list give their values in the order named, a value given twice once. The host builds
  a holder only when it first asks for that surface and keeps what it gets for the bound project: naming a
  class initialises nothing, so a toolbar holder that links JavaFX costs a headless host nothing. Two shapes
  went with this: holders read by field reflection (2026-10-19, members came from `public static final`
  fields), and `() -> X.ALL` suppliers beside them, with `parts(…)` — four ways to say one thing, and a
  member-less marker interface that told the author nothing (2026-10-28, the user's call).
- **The lifecycle**: `.onProjectOpened(Owner::open)` (a `Consumer<StudioServices>`) and
  `.onProjectClosing(Owner::close)`; `DeclaredPlugin.projectOpened`/`projectClosing` are `final` and call them,
  so a plugin class is its declaration and nothing else (`studio-services.md` says when a plugin needs either).
  `.onBotRun(Owner::started)` (a `Consumer<RunChannel>`) is the run's side: inside a bot's process, before its
  `main`, the host hands the plugin's runtime the channel it writes lines, details, frames and questions on
  (2026-11-03, `DeclaredPlugin.botRun`).
- **A type**: `PluginType.value(X.class)` → `fresh(…)` | `firstConstant()` →
  `editor(() -> E::draw)` → optional `preview(…)` → `writtenAs(Owner::factory, X::part, …)` |
  `writtenAsEach(…)` | `writtenAsRecord()` | `writtenAsConstant()` | `writtenAsLiteral()` | `writtenAsParts()`
  (`TypeSteps`, `CallSteps`, results `DeclaredType`/`DeclaredCallType`). **A part**: `ComponentType.part(X.class)
  .writtenAs(…)` (`DeclaredCall`, with `.constants(VALUE)`, and `.components`/`.build` for the one hand-made
  part, the SDK's activity body until flows were deleted on 2026-11-09). A plugin declares nothing about a type it
  retires: the bot's author writes the `@Migrate` rule that carries its values over (2026-10-16); the plugin
  never converts them. An element it removes ships, in the release that deprecates it, a `@Migrate` rule beside it
  or `@Migrate.Dropped("why")` on it when nothing replaces it (2026-11-01); `plugin validate` warns on neither.
- **Factories are method references, never names.** `Ref.Of0`–`Of10` are serializable functional interfaces;
  `Ref.resolve` reads the `SerializedLambda` into the `Method` or `Constructor` once, when the declaration is
  built, and refuses a lambda. One typed `writtenAs` per arity, so the accessors' types pick an overloaded
  factory (`LocalDate::of`, `Color::new`); past ten, a plugin declares its own interface extending `Ref` and
  uses `writtenAs(Ref, Function...)`. `build` is invoking the factory on the parts coerced to its parameters,
  `null` when they do not fit or it throws.
- **A plugin's own type names nothing twice** (2026-11-01). `writtenAs(Tree::of)` with no accessors reads
  each parameter `p` of a static factory or constructor by `p()`, or `isP()` for a `boolean`, by name and type
  on the compiled class (`-parameters`, on in `botmaker-plugin-parent`). Every method marked `@Link`, on the
  type or on one it overrides (`Input`'s), is chained after the factory: `x(A)` read by `x()`, the flag `x()`
  by `boolean isX()`, in the readers' record-component order, then by name. A mismatch is refused when the
  declaration is built, so the plugin's own tests (`PluginTests.assertDeclared`) catch it, never a bot; an IDE
  rename of one side alone is caught there, not by javac. `.with`/`.flag` stay for a JDK type, a reader under
  another name, or a `Held` part, and an explicit one replaces the marked link in its place.
- **Several factories, no predicate.** The host writes a value with `ComponentType.writer(declarations, v)`:
  the first that `writes` it and `rebuilds` it (`build(components(v))` equals `v`), else the first that writes
  it — so a value holding a lambda, equal only to itself, still has a writer. `.when` is for what equality
  cannot decide: a value written as its constant (`PictureTag`), or one holding a lambda.
- **A managed value**: `ManagedValue.method(id).in(holder).holds(T.class, initial).because(reason)`,
  `.openedOnly().holds(T.class)` instead of `in` for one the host may open and never create, and
  `ManagedValue.openSet(id).of(E.class).in(holder).because(reason)`, `E` being the class of each constant;
  `.ofEnum(E.class, Owner::byName)` instead of `of` makes the holder an enum implementing the interface `E`,
  whose constants the host reads as `byName(name)`. `id` is a constant of the enum nested in the plugin's
  `@ManagedMarker` annotation (`SdkValue.Id.SETTINGS`). It
  is typed and declared once: a plugin keeps one constant per value and lists it in its `ValueHolder`, uses it
  in `ManagedValues.claim`, and through the toolkit's `ManagedHandle` or, for an open set, `ManagedSet`.
  A holder enum that implements `HostHolder` (2026-11-12) makes the value this PC's (`onHost()`): declared by the
  same steps, under a marker annotation of its own, served by `StudioServices.hostValues()`; `param` refuses it.
- **A slot editor**: `SlotEditor.onParameter(Annotation.class)` | `forType(X.class)` | `when(predicate)` →
  `.draw(() -> E::draw[, () -> E::preview])` (`EditorSteps`). A value the type cannot tell apart is told apart
  by a `RUNTIME` annotation on the parameter it is passed to (`outcome(@OutcomeName String)`), matched by
  the annotation's binary name through `SlotContext.parameter()`; an editor reads its settings off it, or
  off a table the plugin keys by `Parameter.getDeclaringExecutable()` (the SDK's `SettingHints`). A preview is a small, non-interactive picture of one value, for a value
  shown in a list of choices rather than edited.
- **A toolbar item**: `ToolbarItem.id(ID).label(…).tooltip(…).onBar(order)` (or `.inOverlay(order)`: the
  overlay editor's Actions tab) → optional
  `.enabledWhen(…)`/`.icon(…)` → `.onPress(() -> MyWindow::open)` (`ToolbarSteps`). The press is a `Pressed`,
  a supplier of the handler, for `Drawn`'s reason: the item list is built headless.
  Listed by a `ToolbarHolder`.
- **An overlay part**: `OverlayPart.of()` → any of `.targets(Body.class, "Group")` (a functional interface;
  the host finds the methods passed as one), `.watched(Owner::watched)`,
  `.changeWatched(() -> Owner::change)`, `.tool(OverlayTool.id(ID).named(LABEL).pane(Owner::pane))`,
  `.probe(Call::ref, Param.class…, Owner::probe)` (the parameter classes pick an overloaded call, which a bare
  reference cannot; `probeMember` for `Ref.member`). One per
  plugin, through `.overlay(MyOverlay.class)` (an `OverlayHolder`); a part declaring nothing is no part.
- **An assistant tool**: `AssistantTool.named("snake_name").describedAs(…)` → `.takes(Params.class)` (a
  record; its components are the schema, checked here) | `.takesNothing()` → `.handledBy(Owner::handle)`.
  Listed by an `AssistantHolder`, through `.assistant(MyTools.class)`.
- **A run beside the bot's** (2026-11-03): `RunEntry.of("Explore map", ExploreMap::main)`, a title and a public
  static `main(String[])` by method reference, checked in `of`. Listed by a `RunEntryHolder`, through
  `.runs(MyRuns.class)`. The host runs it as it runs the bot (same JVM options, classpath, log and end-of-run
  dialog) with that class as the main class, its `args` the bot's value-holder binary names, as ▶ Try's are; two
  entries with one title are refused when the host lists them. **The trial entry**: `.trial(Bot::trial)`, a `TrialEntry`. It is
  resolved and checked to be a public static `(Runnable, Class<?>...)` in the step itself, so a bad one fails
  the plugin's own construction.

`StudioPlugin` is sealed to `DeclaredPlugin` and every surface method of `DeclaredPlugin` is `final`
(2026-10-27, 2026-10-28): there is no second way to be a plugin, so nobody has to know which methods to
override, which may answer `null`, or how to name a factory without a string. `PluginTests.assertDeclared`
(toolkit) runs the host's load check in the plugin's own `mvn verify`: it builds every holder and asks it.
