# botmaker-studio-api

The contract a **BotMaker Studio plugin** compiles against. Interfaces, records and declaration steps only —
no implementation, no Studio types, no parser.

```xml
<dependency>
    <groupId>com.github.BotMakerDev</groupId>
    <artifactId>botmaker-studio-api</artifactId>
    <version><!-- latest tag: https://github.com/BotMakerDev/botmaker-studio-api/tags --></version>
    <scope>provided</scope>
</dependency>
```

`provided` unless the plugin's annotations sit on a bot's own code (the SDK's `@SdkValue`, `@Param`): then
`compile`.

## A plugin is one declaration

```java
public final class ExamplePlugin extends DeclaredPlugin {

    public static final String ID = "com.example.greetings";   // the identity; never changes

    public ExamplePlugin() {
        super(StudioPlugin.id(ID).named("Greetings")
                .types(() -> ExampleTypes.ALL)
                .editors(() -> ExampleEditors.ALL));
    }
}

public final class ExampleTypes {
    public static final DeclaredCallType<Greeting> GREETING = PluginType.value(Greeting.class)
            .fresh(() -> new Greeting("world", 1))     // what a new one is
            .editor(() -> GreetingEditor::of)          // how a person edits one
            .writtenAsRecord();                        // how the host writes its Java

    public static final List<PluginType<?>> ALL = List.of(GREETING);
}
```

Each step after `named` is optional, and each list sits behind a supplier, so a headless host (the
registry's CI, `botmaker plugin validate`) constructs the plugin without loading JavaFX. The steps lead the
author through what the host needs: the compiler refuses a type with no fresh value or no way to be
written. Factories are method references (`writtenAs(Owner::factory, Type::part, …)`), never strings.

| step | what it declares |
|---|---|
| **palette** | usually nothing: every `@Palette` class in the plugin's jar is offered, each public member unless `@Hidden`; `catalog(…)` builds one by hand |
| `types` | a type this plugin owns: what a new one is, how it is edited, how its Java is written |
| `parts` | calls inside a value that are not types of their own |
| `editors` | an editor chosen by the call or the parameter around a value (`SlotEditor.onParameter(…)`) |
| `values` | **managed values**: values a window of the plugin keeps in the bot's own Java |
| `toolbar`, `recorded`, `runOverlay`, `overlay`, `assistant` | a toolbar button, recorded input, what a run draws, the overlay editor's tools, assistant tools |
| `trial` | ▶ Try: a method reference (`.trial(Bot::trial)`), not a supplier |

**Panels are deliberately not a surface.** A plugin contributes to the editor; it does not contribute
editors.

## Two things worth knowing before you write one

**No plugin reads or writes Java.** An editor is handed the value (`ctx.value(Greeting.class)`) and hands one
back (`ctx.set(new Greeting("you", 2))`); the host writes the call and reads it back through the type's
declared parts. A value nobody can decode — a variable, a call — stays readable as `ctx.source()` and is
shown read-only. No syntax tree crosses the boundary in either direction.

**A managed value is Java in the bot, marked with the plugin's own annotation.** The annotation carries
`@ManagedMarker`, and its `value()` is a constant of an enum nested in it:

```java
// the plugin
@ManagedMarker @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.TYPE, ElementType.METHOD})
public @interface SdkValue { Id value(); enum Id { FLOW, CAPTURE, ACTIVITIES } }

// the bot
public final class Sdk {
    @SdkValue(SdkValue.Id.FLOW)
    public static Flow flow() { return Flow.of(…); }
}
```

Every marked method is `public static T id() { return <expr>; }`, and the host rewrites only that expression,
so comments and helpers around it survive. A plugin that changes a managed value's shape declares the
`Conversion` from the old one (by overriding `StudioPlugin.conversions()`; there is no declaration step); Studio runs it when a project upgrades the plugin, and the release refuses a
plugin that changed shape without one.

## Compatibility

A released Studio loads a plugin only when the contract it ships has every member the plugin's classes link
(`ContractLinks` in `botmaker-plugin-host`); otherwise it is not loaded, and the loader reports the missing
members and which side is behind — the plugin or Studio.
The contract itself may still break between releases while the platform is in active development.

## Building

```bash
mvn test        # the declaration steps, the catalog, the managed values and the context defaults
mvn install     # com.github.BotMakerDev:botmaker-studio-api at the pom's main -SNAPSHOT
```

Published through JitPack, which serves each git tag as `com.github.BotMakerDev:botmaker-studio-api:<tag>`.
Releases are cut from the umbrella only, with `--studio-api <version>` plus every downstream it forces
(toolkit, host, basics, cli, sdk, studio, dashboard); `/release-check` names them.
