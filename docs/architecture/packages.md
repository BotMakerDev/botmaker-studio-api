# The packages

- `com.botmaker.plugin.api` — `StudioPlugin`, `DeclaredPlugin`, `PluginDeclaration`, `StudioServices` and
  what it hands back (`Theme`, `Dialogs`, `Runs`, `History`), `TraceLine`, and **`StyleClasses`**: the
  style-class names the host's stylesheet defines, constants only. Only the host knows what its stylesheet
  names, and it is the one thing a plugin's widgets and Studio's own both need — which is why it is here rather
  than a reason for Studio to depend on the toolkit. `StyleClassesTest` in Studio holds the stylesheet to it.
- **One package per contribution surface**: `…api.slot` (`SlotEditor`, `EditorSteps`, `SlotContext`,
  `SlotRun`, `ValueContext`, `TypeRef`, `Bounds`), `…api.toolbar` (`ToolbarItem`, `ToolbarSteps`, `Pressed`,
  `EnabledWhen`, `ActionContext`; no groups since 2026-10-28 — an item is placed `onBar` or `inOverlay`), `…api.overlay` (`OverlayPart`, `OverlayTool`,
  `OverlayContext`, `FrameContext`, `OverlayToolContext`, `OverlayFrame`, `Probe`, `ProbeContext`, `ProbeResult`,
  `Marks`, `Pixel`, `Area`, `Watched`), `…api.run` (`RunOverlayPart`, `RunOverlayContext`, `RunProperty`), `…api.assist`
  (`AssistantTool`, `ToolSchema`, `@Describe`, `@Key`, `AgentContext`, `AgentReply`, `TrialEntry`, and the
  package-private `ToolSchemas` and `ToolArguments` behind it). `…api.record` (`@Records`, `Gesture`,
  `RecordedValue`) went on 2026-10-22 with the macro recorder it served.
- `…api.source` — a plugin's values in the bot: `ManagedValue`, sealed by shape into `ManagedParam`,
  `ManagedMethod` and `ManagedOpenSet` (its kinds `OfConstants`, `OneEnum`, `EnumGroups`, and
  `ManagedClasses`), `ParamScope`, and `PluginValues`, the host's reads and writes of them.
- `…api.params` (`@Param`) and `…api.managed` (`@ManagedMarker`, `ManagedValues`): `@Param` and a plugin's
  own `@ManagedMarker` annotation are the two that sit on a **bot's** own declarations.
- `…api.value` — `PluginType<T>` (the class, `fresh()`, optional `freshCall`/`preview`), `EditableType<T>`
  (adds `editor(ValueContext)`, never `null` — a type its owner draws), `ComponentType<T>` (`componentTypes`,
  `components`, `build`, `factory`, an `Executable`), `Held`, `Wither`, and the steps that declare
  them (`TypeSteps`, `CallSteps`, the package-private `CallShape`, `DeclaredType`, `DeclaredCall`,
  `DeclaredCallType` and their package-private base `CallDeclaration`, `Ref`, `Drawn`).
- `…api.catalog` — `PaletteCatalog`, `Category`, `FacadeEntry`, `MemberEntry`, `MemberId`, and the
  package-private `CatalogReader` and `SourceOrder`. `PaletteCatalog.of(Class<?>...)` builds it by reflection.
- `…api.palette` — **`@Palette`**, **`@Hidden`**, `@PaletteLabel`, `@PaletteDefault`, `@EntryPoint`,
  `@Untraced`, and `@Fills` on a method: the marks a plugin puts on its own classes, `RUNTIME` because the
  catalog and the host reflect on them.
  Their elements are plain `String`s on purpose — an annotation element's type must be visible from the module
  *declaring* the annotation, so a contract annotation can never take a plugin-defined enum constant.
  `@Palette` = **offered** (its own menu entry); the **catalogue** (the recognition set: imports, "does `Point`
  mean this plugin's or `java.awt`'s", a variable's member list) is the offered classes plus every public type
  of the same jar their non-`@Hidden` members reach, derived by `PaletteCatalog.of`. `@Hidden` is member-only:
  that member is not offered and reaches nothing. Facades are listed alphabetically by label.
- `…api.meta` — **`@Refactor(value, done)`**, the mark on a function where a refactor wrote a value the user
  never chose; **`@Migrate`** (a bot's rule saying what one element of an old API becomes — a method,
  constructor, field or whole type, named by the rule's own signature — never run) and **`@Migrated`** (a managed
  value such a rule wrote and nobody edited since), all three `SOURCE`. A bot's author writes a rule in the bot; a
  plugin may ship one beside an element it marks `@Deprecated(forRemoval = true)`, which Studio reads from the
  plugin's sources jar, and removes both in its next release (2026-10-28). The author's rule wins.
