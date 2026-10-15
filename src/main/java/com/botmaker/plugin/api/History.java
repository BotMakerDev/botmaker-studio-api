package com.botmaker.plugin.api;

/**
 * The open project's undo history — the steps the host's own ↶ and ↷ take back and do again.
 *
 * <p>On {@link StudioServices} because the host is its only source: the history is the editor's, and it holds
 * every change to the bot's Java, the user's typing and a plugin's
 * {@link com.botmaker.plugin.api.source.PluginValues} writes alike. A plugin window with an undo of its own would take back only its own writes, and an edit the user made
 * in between would be lost or kept by chance; through this one, the window's ↶ is the editor's, in the same
 * order. Nothing here names a plugin's concept: a step is a label and the files it wrote.
 *
 * <p>Each {@code PluginValues} write is one step. {@link #batch} makes several one — a drag that moved ten
 * cards is one ↶, under the plugin's own name for it.
 *
 * <p>{@link #NONE} is what a host with no editor answers: nothing to undo, nothing ever fires, and a batch
 * just runs its writes.
 */
public interface History {

    /** Whether there is a step to take back. */
    boolean canUndo();

    /** Whether there is a step to do again. */
    boolean canRedo();

    /** What {@link #undo()} would take back, for a button's tooltip to name; blank when there is nothing. */
    String undoLabel();

    /** What {@link #redo()} would do again; blank when there is nothing. */
    String redoLabel();

    /**
     * Takes the last step back, as the host's ↶ does — whoever made it. The files are as they were when this
     * returns; a plugin reads its values again to show them. Does nothing when there is nothing to undo.
     */
    void undo();

    /** Does the last undone step again, as the host's ↷ does. Does nothing when there is nothing to redo. */
    void redo();

    /**
     * Registers {@code listener} for every change to the history — a step recorded, undone or redone, or the
     * history cleared — and returns the way to unregister it. A window's ↶/↷ re-read their state there.
     *
     * <p>Close the handle when the window closes, or in {@link StudioPlugin#projectClosing()}. The thread is the
     * host's and is not promised: a listener that touches UI hops to the JavaFX application thread itself.
     */
    AutoCloseable onChanged(Runnable listener);

    /**
     * Runs {@code writes} and records every {@code PluginValues} write they make as one step named
     * {@code label}: <em>Move 3 cards</em>. A batch inside a batch belongs to the outer one; writes that change
     * nothing record nothing. Each write still answers its own refusal; the ones that passed are the step.
     *
     * <p>{@code writes} runs exactly once, on the calling thread, before this returns — on every host, {@link #NONE}
     * too — so a plugin reads what its writes answered right after. Only writes made on that thread join the step.
     */
    void batch(String label, Runnable writes);

    /** A host with no editor: nothing to undo, nothing fires, and a batch runs its writes. */
    History NONE = new History() {

        @Override
        public boolean canUndo() {
            return false;
        }

        @Override
        public boolean canRedo() {
            return false;
        }

        @Override
        public String undoLabel() {
            return "";
        }

        @Override
        public String redoLabel() {
            return "";
        }

        @Override
        public void undo() {
        }

        @Override
        public void redo() {
        }

        @Override
        public AutoCloseable onChanged(Runnable listener) {
            return () -> {
            };
        }

        @Override
        public void batch(String label, Runnable writes) {
            if (writes != null) writes.run();
        }
    };
}
