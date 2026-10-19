# japicmp, and why it is legitimate here

`mvn verify` compares this build against `botmaker.japicmp.baseline` and **fails on any binary- or
source-incompatible change**, with no ignore list and no exemption annotation. It catches the trap
`../docs/refactor/25-compatibility.md` lists: adding a component to a public record changes its canonical
constructor descriptor and throws `NoSuchMethodError` in every already-compiled plugin.

The SDK's August japicmp gate was deleted because CI cannot tell an intended break from an accident. Here
the rule is unconditional — only a Studio major release may break a plugin, and that release edits this
block — so there is nothing to distinguish. The baseline is set to the previous tag in every release commit
(`Japicmp.bump`, never backwards).

**The baseline is pinned to `v0.11.0`** (2026-10-19), ahead of the tag: `TypeSteps.Fresh.filledBy` and
`TypeSteps.Filled` went, a value the bot fills in being its method marked `@Fills`, so the next release is
**`--studio-api 0.11.0`**. A break is pinned ahead to the next minor's tag, as was done for `v0.2.0`, `v0.3.0`,
`v0.4.0`, `v0.6.0`, `v0.9.0` and `v0.10.0`. An absent
baseline tag reports and passes. **When a baseline "passes", look at the first line of `target/japicmp/japicmp.diff`**:
`against` followed by nothing means nothing was compared.
