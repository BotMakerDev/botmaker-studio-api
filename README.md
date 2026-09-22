# botmaker-studio-api

The contract a **BotMaker Studio plugin** compiles against. Interfaces and records only — no implementation,
no Studio types, no parser.

```xml
<dependency>
    <groupId>com.github.LiQiyeDev</groupId>
    <artifactId>botmaker-studio-api</artifactId>
    <version><!-- a tag --></version>
    <scope>provided</scope>
</dependency>
```

## What a plugin contributes

```java
public final class MyPlugin implements StudioPlugin {

    @Override
    public String id() {
        return "my-plugin";
    }

    @Override
    public PaletteCatalog catalog() {
        return PaletteCatalog.of(Sound.class);          // members are read off @Palette / @Hidden
    }

    @Override
    public List<PluginType<?>> types() {
        return List.of(new VolumeType());               // one declaration per type this plugin owns
    }

    @Override
    public List<SlotEditor> slotEditors() {             // only where the *call* decides the editor
        return List.of(SlotEditor.forCall(Sound.class, 0, ctx -> new ChannelPicker(ctx), "play"));
    }
}

final class VolumeType implements PluginType<Volume>, ComponentType<Volume> {
    public Class<Volume> type()                  { return Volume.class; }
    public Volume fresh()                        { return new Volume(50); }
    public Node editor(ValueContext ctx)         { return VolumeSlider.of(ctx); }

    public List<Class<?>> componentTypes()       { return List.of(int.class); }
    public List<Object> components(Volume v)     { return List.of(v.percent()); }
    public Volume build(List<Object> parts)      { return new Volume((int) parts.get(0)); }
}
```

| surface | what it is |
|---|---|
| **palette** | which types and members are worth proposing, in which groups and in which order |
| **types** | a type this plugin owns: what a new one is, how a user edits one, and — when its Java is a call — the parts the host writes it as |
| **slot editors** | an editor chosen by the call around a value, or one offered for a type another plugin owns |
| **managed values, toolbar items** | a value the plugin keeps in the bot's own Java, and a button in the host's toolbar |

**Panels are deliberately not a surface.** A plugin contributes to the editor; it does not contribute
editors.

## Two things worth knowing before you write one

**A catalog is a list of classes.** `PaletteCatalog.of(Mouse.class, Keyboard.class)`: class literals javac
checks, and members discovered by reflection. Every public method of a `@Palette` class is offered unless
it is `@Hidden`, so a member you rename is renamed in the palette with nothing to keep in step.

**No plugin reads or writes Java.** An editor is handed the value (`ctx.value(Volume.class)`) and hands one
back (`ctx.set(new Volume(80))`); the host writes `new Volume(80)` and reads it back through the plugin's
own `components` and `build`. A value nobody can decode — a variable, a call — is still readable as
`ctx.source()` and is shown read-only. No syntax tree crosses the boundary in either direction, which is why
this module depends on nothing but JavaFX.

## Compatibility

Stricter than anything else in the BotMaker repositories, for one reason: a bot's *source* can be rewritten
when the SDK changes, but a plugin's *bytecode* cannot be rewritten by anybody. So this module changes far
more slowly than the plugins implementing it — new members arrive as `default` methods, and a plugin built
against an older release keeps working until a Studio **major** release explicitly refuses it.

## Building

```bash
mvn test        # the catalog, PluginType/ComponentType, SlotEditor and the context defaults
mvn install     # lands at com.github.LiQiyeDev:botmaker-studio-api:0.0.0-SNAPSHOT
```

Published via JitPack, which serves each git tag as `com.github.LiQiyeDev:botmaker-studio-api:<tag>`. The
pom's own `<version>` is cosmetic. Releases are cut from the umbrella repository with
`./release.sh --studio-api <version>`.
