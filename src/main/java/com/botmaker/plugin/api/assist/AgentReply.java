package com.botmaker.plugin.api.assist;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/**
 * What an {@link AssistantTool} answers: text, images, or a refusal, in order —
 * {@code AgentReply.text("Found it at 412,230").and(AgentReply.image(boxed))}.
 *
 * <p>A refusal is an answer, not an error: the assistant reads the sentence and tries something else. A
 * handler that throws is reported to it the same way, with the exception's message. Immutable.
 */
public final class AgentReply {

    /** What a part of a reply is. */
    public enum Kind {
        TEXT,
        /** A PNG. */
        IMAGE
    }

    /** One part of a reply: a line of text, or a PNG. */
    public static final class Part {

        private final Kind kind;
        private final String text;
        private final byte[] png;

        private Part(Kind kind, String text, byte[] png) {
            this.kind = kind;
            this.text = text;
            this.png = png;
        }

        public Kind kind() {
            return kind;
        }

        /** The text of a {@link Kind#TEXT} part, {@code ""} for an image. */
        public String text() {
            return text;
        }

        /** A copy of an {@link Kind#IMAGE} part's PNG bytes, empty for text. */
        public byte[] png() {
            return png.clone();
        }
    }

    private final List<Part> parts;
    private final boolean refused;

    private AgentReply(List<Part> parts, boolean refused) {
        this.parts = List.copyOf(parts);
        this.refused = refused;
    }

    /** A line the assistant reads. */
    public static AgentReply text(String text) {
        return new AgentReply(List.of(new Part(Kind.TEXT, text == null ? "" : text, new byte[0])), false);
    }

    /** A PNG the assistant looks at. */
    public static AgentReply image(byte[] png) {
        if (png == null || png.length == 0) throw new IllegalArgumentException("An image reply needs a PNG");
        return new AgentReply(List.of(new Part(Kind.IMAGE, "", png.clone())), false);
    }

    /** {@code image}, encoded as a PNG. */
    public static AgentReply image(BufferedImage image) {
        if (image == null) throw new IllegalArgumentException("An image reply needs an image");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            if (!ImageIO.write(image, "png", out)) throw new IllegalArgumentException("Cannot encode a PNG");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return image(out.toByteArray());
    }

    /** The tool would not do it, and {@code why} says what to do instead. */
    public static AgentReply refused(String why) {
        return new AgentReply(List.of(new Part(Kind.TEXT, why == null ? "" : why, new byte[0])), true);
    }

    /** This reply followed by {@code more}; refused if either is. */
    public AgentReply and(AgentReply more) {
        if (more == null) return this;
        List<Part> all = new ArrayList<>(parts);
        all.addAll(more.parts);
        return new AgentReply(all, refused || more.refused);
    }

    /** The parts, in order. */
    public List<Part> parts() {
        return parts;
    }

    /** Whether the tool refused. */
    public boolean isRefused() {
        return refused;
    }
}
