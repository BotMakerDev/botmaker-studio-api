# CLAUDE.md

Guidance for working in **botmaker-studio-api**, the contract a BotMaker Studio plugin compiles against.

Read the umbrella `../CLAUDE.md` first, `../docs/refactor/24-plugin-platform.md` for why this module exists at
all and `../docs/refactor/25-compatibility.md` before changing any public member. This file states what is
true now. The mechanisms that were here and went — `Assets`, `Capture`, `Sources`, `PluginSource`, the
parameter-data surface, the scaffold/seed surface, `ValueType`/`ValueCodec`/`ValueCatalog`, the catalog's
method-reference builder, `@Replaces`/`@Since` — and why each went are in
`../docs/refactor/31-umbrella-history.md` (*contract*); the text this file carried until 2026-09-28 is
`git show ad93451:CLAUDE.md` in this repository. Search there before re-proposing one.

The design and the reasons behind it are in `docs/architecture/`, one file per section (moved there unchanged
on 2026-10-05).

## Read before touching

| Touching | Read (`docs/architecture/`) |
|---|---|
| any public member | `easy-to-break.md`, then `../docs/refactor/25-compatibility.md` |
| `StudioPlugin.id(…)`, `PluginType`, `ComponentType`, `ManagedValue`, `SlotEditor`, `ToolbarItem` steps, `Ref` | `declaration-steps.md` |
| `Ref.member`, the SDK's activity-body text | `two-strings.md` |
| a new value a plugin constructs | `no-public-constructor.md` |
| which package a type goes in, `StyleClasses`, the palette annotations, `@ReplacedBy`, `@Refactor` | `packages.md` |
| `StudioServices`, `Runs`, `projectClosing`, `pluginValues()` | `studio-services.md` |
| `PluginType`/`EditableType`/`ComponentType` semantics | `value-vocabulary.md` |
| `PaletteCatalog`, `SourceOrder`, `MemberId` | `catalog.md` |
| `botmaker.japicmp.baseline`, a japicmp failure | `japicmp.md` |

## What this module is

Interfaces, records, annotations, and the step classes that build a declaration. It has no implementation of
a host, references no Studio type, and depends on one artifact (`javafx-controls`, `provided`).

**One runtime class: `managed.ManagedValues`**, the bot-side half of `@Managed`
(`claim(ManagedValue<T>, Consumer<? super T>)`, `install(Class<?>...)`). It runs inside a bot, which already
has this jar through any plugin that puts `@Managed` there. Add nothing else of the kind: the test is *does
every plugin with a bot-side half need it, and does it name nothing but this module and the JDK*.

Studio is the host; `botmaker-sdk` is the first plugin — a *privileged default* plugin, but a plugin, with
no back door. If the SDK needs something this module does not expose, the contract is wrong, not the SDK.

## Rules

- **Every method but `StudioPlugin.id()` is `default`, and stays that way**; a member an already-built plugin
  cannot survive is a Studio major (`easy-to-break.md`).
- **Nothing from a plugin crosses as a `Class<?>` the host must load**; compare by binary name (`TypeRef`).
- **No syntax tree and no Java text, in either direction**: an editor reads a value and writes one.
- **A service goes on `StudioServices` only when the host is the sole possible source of it** — a
  capability, never a vocabulary (`studio-services.md`).
- **Every surface is declared by steps; factories are method references**, with the two strings in
  `two-strings.md` kept. **No public constructor on a value a plugin builds** (`no-public-constructor.md`).
- **The catalog degrades, never throws** (`catalog.md`). **No Jackson here.**
- **japicmp fails on any incompatible change, with no ignore list**; the baseline is pinned to `v0.4.0`, so
  the next release is `--studio-api 0.4.0` (`japicmp.md`).

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
mvn verify      # the above plus japicmp against botmaker.japicmp.baseline (docs/architecture/japicmp.md)
mvn install     # com.github.BotMakerDev:botmaker-studio-api:0.0.0-SNAPSHOT
```

Published through JitPack, which serves each git tag under `com.github.BotMakerDev` regardless of this pom's
`groupId`/`version` (so the version is cosmetic). **The maintainer owns the publish** — releases are cut
from the umbrella with `../release.sh --studio-api <version>`.

Unlike `botmaker-session` and `botmaker-sdk` this module runs **no `flatten-maven-plugin` and has no
`.deps.env`**: flatten exists to bake a `-D`-injected `${botmaker.*.version}` into the published pom, and
this module pins no BotMaker upstream to inject. Its `jitpack.yml` is a plain `mvn install`.
