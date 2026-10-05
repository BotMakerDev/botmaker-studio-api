# A value a plugin declares is built through steps or a static factory, never a public constructor

A record's canonical constructor is public and changes when a component is added, which throws
`NoSuchMethodError` in every compiled plugin that called it. So `ToolbarItem` and `ManagedValue` are final
classes whose constructors only their steps reach, and `SlotRun.Element`, which an editor hands back and has
no steps, is built with `Element.of(value, source)`. A record is still right for what the **host** builds and
a plugin only reads (`Bounds`, `ActionContext.Area`, `PluginValues.Use`, `RecordedValue.Spot`,
`Dialogs.Choice`). A new value a plugin constructs gets steps or a factory from its first commit.
`botmaker-plugin-host`'s `ContractLinks` is the load-time half: a plugin linking a contract member this build
lacks — or one made package-private — is refused with the member named (`25-compatibility.md` §1–2).
