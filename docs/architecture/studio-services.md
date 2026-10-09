# What may go on `StudioServices` — the host-only rule

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
(`../docs/refactor/33-plugin-java.md`). A bot holds a plugin's values as methods marked with the plugin's own
annotation (`@SdkValue(SdkValue.Id.FLOW)`, meta-annotated `@ManagedMarker`) in its own
`src/main/java/<bot package>/plugins/<last id segment>/`, and the host never touches the class around them.
`open(id)` hands back a **`ValueContext`**, so a slot, a Parameters row and a managed value are all edited
through one interface and no second way to edit a value exists; a body that is not a single
`return <expression>;` answers empty and is shown read-only with the reason. An open set (the mark on a
type: the SDK's `Pictures` class, or its `Outcomes` enum when the set is declared `ofEnum`) changes **by
binding**: `members`, `add`, `uses`, `rename`, `repoint`, `remove`,
each total and refused when the bot would stop compiling. The host rewrites; the plugin says which constant.
`PluginValues.Use` is host-constructed only.
