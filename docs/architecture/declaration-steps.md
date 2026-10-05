# The declaration steps

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
  `.openedOnly().holds(T.class)` instead of `in` for one the host may open and never create, and
  `ManagedValue.openSet(id).of(E.class).in(holder).because(reason)`, `E` being the class of each constant. It
  is typed and declared once: a plugin keeps one constant per value and uses it in `managedValues()`, in
  `ManagedValues.claim`, and through the toolkit's `ManagedHandle` or, for an open set, `ManagedSet`. **A recorded value**: `RecordedValue.of(T.class).at(Finder::find)`.
- **A slot editor**: `SlotEditor.onParameter(Annotation.class)` | `forType(X.class)` | `when(predicate)` →
  `.draw(() -> E::draw[, () -> E::preview])` (`EditorSteps`). A value the type cannot tell apart is told apart
  by a `RUNTIME` annotation on the parameter it is passed to (`outcome(@OutcomeName String)`), matched by
  the annotation's binary name through `SlotContext.parameter()`; an editor reads its settings off it, or
  off a table the plugin keys by `Parameter.getDeclaringExecutable()` (the SDK's `SettingHints`). A preview is a small, non-interactive picture of one value, for a value
  shown in a list of choices rather than edited.
- **A toolbar item**: `ToolbarItem.id(ID).label(…).tooltip(…).in(group, order)` → optional
  `.enabledWhen(…)`/`.icon(…)` → `.onPress(() -> MyWindow::open)` (`ToolbarSteps`). The press is a `Pressed`,
  a supplier of the handler, for `Drawn`'s reason: the item list is built headless.
  `DeclaredPlugin.toolbarItems()` is `final`.
- **An overlay part**: `OverlayPart.of()` → any of `.targets(Owner::targets)`, `.watched(Owner::watched)`,
  `.changeWatched(() -> Owner::change)`, `.tool(OverlayTool.id(ID).named(LABEL).pane(Owner::pane))`,
  `.probe(Call::ref, Param.class…, Owner::probe)` (the parameter classes pick an overloaded call, which a bare
  reference cannot; `probeMember` for `Ref.member`). One per
  plugin, through `.overlay(() -> PART)`; a part declaring nothing is no part.

Implementing the interfaces by hand still works and the host cannot tell the two apart; the steps are how
nobody has to know which methods to override, which may answer `null`, or how to name a factory without a
string.
