// SPDX-FileCopyrightText: 2026 LiQiyeDev
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package com.botmaker.plugin.api.catalog;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The order a class declares its methods in, read out of the compiled class file.
 *
 * <h2>Why this exists</h2>
 *
 * <p>Member order is what the menu shows, and it is editorial: {@code Mouse} reads
 * <i>click, move, down, up, rightClick…</i> because its author wrote it that way, not because those words
 * sort. {@link Class#getDeclaredMethods()} explicitly promises no order at all, so a catalog built by
 * reflection alone can only sort alphabetically, which is a different menu.
 *
 * <p>The order is not lost, though — it is simply not in the reflective API. javac writes the {@code methods}
 * table of a class file in source order, and that table is four fields into a format that has been stable
 * since Java 1.0. So the answer comes from the same {@code .class} bytes the loader already read, with no
 * dependency, no annotation restating what the source says, and no build step.
 *
 * <p>The predecessor got this from an annotation processor, which had the source itself in hand and emitted
 * {@code .order("click", "move", …)} into a generated catalog. This is that one capability kept, at the price
 * of ~90 lines rather than a module.
 *
 * <h2>What it promises</h2>
 *
 * <p>Nothing, by design. The JVM specification does not require the {@code methods} table to be in source
 * order — javac happens to write it that way, and every compiler in practice does. So every failure here is
 * an empty result and the caller falls back to sorting, which is what the reflective API would have given
 * anyway. A wrong menu order is a cosmetic regression; an exception thrown while a plugin loads is a project
 * that will not open.
 */
final class SourceOrder {

    private SourceOrder() {
    }

    /**
     * The distinct method names this class declares, in class-file order — source order in practice.
     *
     * <p>Names rather than descriptors, because the palette's unit of curation is the name: overloads of one
     * name are one entry, and their internal order is decided by
     * {@link com.botmaker.plugin.api.palette.PaletteDefault} and parameter count, not by the source.
     *
     * @return the names, or an empty list if the class file cannot be found or read
     */
    static List<String> methodNames(Class<?> type) {
        String resource = type.getName();
        int lastDot = resource.lastIndexOf('.');
        resource = (lastDot < 0 ? resource : resource.substring(lastDot + 1)) + ".class";
        try (InputStream in = type.getResourceAsStream(resource)) {
            if (in == null) {
                return List.of();
            }
            return read(new DataInputStream(in));
        } catch (IOException | RuntimeException e) {
            return List.of();
        }
    }

    private static List<String> read(DataInputStream in) throws IOException {
        if (in.readInt() != 0xCAFEBABE) {
            return List.of();
        }
        in.readUnsignedShort();                                   // minor version
        in.readUnsignedShort();                                   // major version
        String[] utf8 = constantPool(in);
        in.readUnsignedShort();                                   // access flags
        in.readUnsignedShort();                                   // this class
        in.readUnsignedShort();                                   // super class
        skipShorts(in, in.readUnsignedShort());                   // interfaces
        skipMembers(in);                                          // fields
        int methods = in.readUnsignedShort();
        Set<String> names = new LinkedHashSet<>();
        for (int i = 0; i < methods; i++) {
            in.readUnsignedShort();                               // access flags
            String name = utf8[in.readUnsignedShort()];
            in.readUnsignedShort();                               // descriptor
            skipAttributes(in);
            if (name != null) {
                names.add(name);
            }
        }
        return List.copyOf(names);
    }

    /**
     * Reads the constant pool, keeping only the {@code CONSTANT_Utf8} entries — the only kind a member name
     * can be. Everything else is skipped by its fixed width.
     *
     * <p>An unrecognised tag aborts by throwing, which the caller turns into "no order known". That is the
     * right failure: a future constant kind of unknown width makes every following offset meaningless, and
     * guessing would produce confident nonsense.
     */
    private static String[] constantPool(DataInputStream in) throws IOException {
        int count = in.readUnsignedShort();
        String[] utf8 = new String[count];
        for (int i = 1; i < count; i++) {
            int tag = in.readUnsignedByte();
            switch (tag) {
                case 1 -> utf8[i] = in.readUTF();                                       // Utf8
                case 7, 8, 16, 19, 20 -> in.skipNBytes(2);                              // Class, String, …
                case 15 -> in.skipNBytes(3);                                            // MethodHandle
                case 3, 4, 9, 10, 11, 12, 17, 18 -> in.skipNBytes(4);                   // Integer, Float, …
                case 5, 6 -> {                                                          // Long, Double
                    in.skipNBytes(8);
                    i++;                                        // eight-byte constants take two pool slots
                }
                default -> throw new IOException("unknown constant pool tag " + tag);
            }
        }
        return utf8;
    }

    private static void skipMembers(DataInputStream in) throws IOException {
        int count = in.readUnsignedShort();
        for (int i = 0; i < count; i++) {
            skipShorts(in, 3);                                    // access flags, name, descriptor
            skipAttributes(in);
        }
    }

    private static void skipAttributes(DataInputStream in) throws IOException {
        int count = in.readUnsignedShort();
        for (int i = 0; i < count; i++) {
            in.readUnsignedShort();                               // name index
            in.skipNBytes(Integer.toUnsignedLong(in.readInt()));
        }
    }

    private static void skipShorts(DataInputStream in, int count) throws IOException {
        in.skipNBytes(2L * count);
    }

    /** The names in the order given, then everything else alphabetically — the fallback folded in. */
    static List<String> arrange(Set<String> present, List<String> declared) {
        List<String> ordered = new ArrayList<>(present.size());
        for (String name : declared) {
            if (present.contains(name)) {
                ordered.add(name);
            }
        }
        List<String> rest = new ArrayList<>(present);
        rest.removeAll(ordered);
        rest.sort(String::compareTo);
        ordered.addAll(rest);
        return ordered;
    }
}
