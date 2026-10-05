# japicmp, and why it is legitimate here

`mvn verify` compares this build against `botmaker.japicmp.baseline` and **fails on any binary- or
source-incompatible change**, with no ignore list and no exemption annotation. It catches the trap
`../docs/refactor/25-compatibility.md` lists: adding a component to a public record changes its canonical
constructor descriptor and throws `NoSuchMethodError` in every already-compiled plugin.

The SDK's August japicmp gate was deleted because CI cannot tell an intended break from an accident. Here
the rule is unconditional — only a Studio major release may break a plugin, and that release edits this
block — so there is nothing to distinguish. The baseline is set to the previous tag in every release commit
(`Japicmp.bump`, never backwards).

**The baseline is pinned to `v0.4.0`, ahead of the tag** (2026-09-30). `v0.3.0` is released; what has broken
since (`@Palette.order` and the type-level `@Hidden` removed) is the next release, which must therefore be
**`--studio-api 0.4.0`**. An absent baseline tag reports and passes. **When a baseline "passes", look at the first line of `target/japicmp/japicmp.diff`**:
`against` followed by nothing means nothing was compared.
