package com.botmaker.plugin.api;

import javafx.stage.Window;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Native file choosers, and the window a plugin's own dialog should be owned by.
 *
 * <p>Native rather than JavaFX's own on purpose: choosing an executable is a thing the user's desktop
 * already knows how to do well, and its dialog knows about places JavaFX's does not (recent items, mounted
 * volumes, the application folder). Where a platform has no native dialog to show, the host falls back to
 * JavaFX's — hence {@link Choice#nativeDialogShown()}, which an editor reads only to decide whether to
 * apologise for how the chooser looked, never to change what it does with the path.
 */
public interface Dialogs {

    /**
     * What a chooser returned.
     *
     * @param nativeDialogShown whether the platform's own dialog was used, rather than the JavaFX fallback
     * @param path              the chosen path, or empty if the user cancelled
     */
    record Choice(boolean nativeDialogShown, Optional<Path> path) {

        public static Choice cancelled(boolean nativeDialogShown) {
            return new Choice(nativeDialogShown, Optional.empty());
        }
    }

    /**
     * The window a plugin's dialog should be owned by — the editor's own window, so a modal dialog blocks
     * the right thing and appears over it rather than behind it.
     *
     * <p>Empty while the editor is not yet attached to a scene, which is the ordinary state during
     * construction: an editor that builds its dialog eagerly asks too early. A dialog with no owner still
     * shows, so {@code ownerWindow().orElse(null)} is a correct thing to pass on.
     */
    Optional<Window> ownerWindow();

    /** Lets the user choose an executable, starting in {@code initialDir} when it exists. */
    Choice chooseProgram(Path initialDir);

    /** Lets the user choose any file, filtered to the given extensions ({@code "png"}, {@code "jpg"}). */
    Choice chooseFile(String title, Path initialDir, String... extensions);

    /** Lets the user choose a directory. */
    Choice chooseDirectory(String title, Path initialDir);
}
