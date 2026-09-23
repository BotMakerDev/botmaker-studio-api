package com.botmaker.plugin.api.catalog;

import com.botmaker.plugin.api.palette.Palette;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Finds the {@link Palette} classes in the jar or class directory one class was loaded from — the reading
 * behind {@link PaletteCatalog#scan(Class)}.
 *
 * <p>Each class file is searched for the annotation's descriptor before anything is loaded, so a plugin's
 * JavaFX half is never linked by a scan: only a class whose bytes name {@code @Palette} is loaded, without
 * being initialised, and it is kept only if the annotation is really on the type.
 */
final class PaletteScan {

    private static final byte[] MARKER =
            ("L" + Palette.class.getName().replace('.', '/') + ";").getBytes(StandardCharsets.US_ASCII);

    private PaletteScan() {
    }

    /** The annotated classes beside {@code anchor}, sorted by name; what cannot be read goes to {@code problems}. */
    static List<Class<?>> classes(Class<?> anchor, List<String> problems) {
        Path root = location(anchor, problems);
        if (root == null) return List.of();
        List<String> names = new ArrayList<>();
        try {
            if (Files.isDirectory(root)) {
                try (Stream<Path> files = Files.walk(root)) {
                    for (Path file : (Iterable<Path>) files.filter(PaletteScan::isClassFile)::iterator) {
                        if (annotated(Files.readAllBytes(file))) {
                            names.add(className(root.relativize(file).toString().replace('\\', '/')));
                        }
                    }
                }
            } else {
                try (JarFile jar = new JarFile(root.toFile())) {
                    for (JarEntry entry : Collections.list(jar.entries())) {
                        if (entry.isDirectory() || !isClassFile(entry.getName())) continue;
                        try (InputStream in = jar.getInputStream(entry)) {
                            if (annotated(in.readAllBytes())) names.add(className(entry.getName()));
                        }
                    }
                }
            }
        } catch (IOException | UncheckedIOException e) {
            problems.add("cannot scan " + root + " for @Palette classes (" + e + ")");
            return List.of();
        }
        Collections.sort(names);

        List<Class<?>> classes = new ArrayList<>();
        for (String name : names) {
            try {
                Class<?> type = Class.forName(name, false, anchor.getClassLoader());
                if (type.isAnnotationPresent(Palette.class)) classes.add(type);
            } catch (ClassNotFoundException | LinkageError e) {
                problems.add(name + ": cannot be loaded (" + e + ")");
            }
        }
        return classes;
    }

    private static Path location(Class<?> anchor, List<String> problems) {
        try {
            CodeSource source = anchor.getProtectionDomain().getCodeSource();
            if (source != null && source.getLocation() != null) return Path.of(source.getLocation().toURI());
        } catch (URISyntaxException | IllegalArgumentException | SecurityException e) {
            problems.add(anchor.getName() + ": cannot locate its jar (" + e + ")");
            return null;
        }
        problems.add(anchor.getName() + " has no jar or class directory to scan");
        return null;
    }

    private static boolean isClassFile(Path file) {
        return isClassFile(file.getFileName().toString());
    }

    /** A class file a class loader can name: not {@code module-info}, {@code package-info} or a versioned copy. */
    private static boolean isClassFile(String path) {
        return path.endsWith(".class") && !path.startsWith("META-INF/")
                && !path.endsWith("module-info.class") && !path.endsWith("package-info.class");
    }

    private static String className(String path) {
        return path.substring(0, path.length() - ".class".length()).replace('/', '.');
    }

    private static boolean annotated(byte[] bytes) {
        outer:
        for (int i = 0; i <= bytes.length - MARKER.length; i++) {
            for (int j = 0; j < MARKER.length; j++) {
                if (bytes[i + j] != MARKER[j]) continue outer;
            }
            return true;
        }
        return false;
    }
}
