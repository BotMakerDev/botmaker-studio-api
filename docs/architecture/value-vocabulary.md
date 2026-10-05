# The value vocabulary — one declaration per type

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
