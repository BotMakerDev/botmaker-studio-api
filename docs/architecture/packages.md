# The packages

- `com.botmaker.plugin.api` — `StudioPlugin`, `DeclaredPlugin`, `PluginDeclaration`, `StudioServices` and
  what it hands back (`Theme`, `Dialogs`, `Runs`), and **`StyleClasses`**: the style-class names the host's
  stylesheet defines, constants only. Only the host knows what its stylesheet names, and it is the one thing a
  plugin's widgets and Studio's own both need — which is why it is here rather than a reason for Studio to
  depend on the toolkit. `StyleClassesTest` in Studio holds the stylesheet to it.
- **One package per contribution surface**: `…api.slot` (`SlotEditor`, `EditorSteps`, `SlotContext`,
  `SlotRun`, `ValueContext`, `TypeRef`, `Bounds`), `…api.toolbar` (`ToolbarItem`, `ToolbarSteps`, `Pressed`,
  `ToolbarGroup`, `EnabledWhen`, `ActionContext`), `…api.source` (`ManagedValue`, `PluginValues`),
  `…api.record` (`@Records`, `Gesture`, `RecordedValue`), and `…api.params` (`@Param`) and `…api.managed`
  (`@ManagedMarker`, `ManagedValues`): `@Param` and a plugin's own `@ManagedMarker` annotation are the two
  that sit on a **bot's** own declarations.
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
