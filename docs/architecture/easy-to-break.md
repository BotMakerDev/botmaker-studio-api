# The rules that are easy to break

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
