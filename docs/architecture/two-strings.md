# Two strings that stay

Both were examined on 2026-09-28 and kept; do not propose removing either without new facts.

- **`Ref.member(owner, name, params)`** names the one factory javac cannot reference: the SDK's
  `CaptureSource.region`, where a static `region(src, r)` and an instance `src.region(r)` share a name and an
  arity. Both are what a bot writes, and a new-named factory would still leave the chained one read through
  this. (The SDK's never-delete, the reason first given, was retired on 2026-10-01.)
- **The SDK's flow activity body is the text `Collect::body`** (`FlowTypes`). It is not a plugin writing
  Java: it goes through the host's own source-leaf path (Studio's `ValueWriter.ofClass`), which parses it into
  a tree, and renames follow bindings in the real `Sdk.java`. A contract `MethodName` type was considered and
  declined: one contract type, grammar changes and a flow-editor rewrite to save about forty lines.
