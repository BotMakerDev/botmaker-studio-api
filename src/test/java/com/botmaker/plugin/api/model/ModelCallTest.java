package com.botmaker.plugin.api.model;

import com.botmaker.plugin.api.value.ValueForm;
import com.botmaker.plugin.api.value.ValueType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The vocabulary a plugin describes its model's calls with.
 *
 * <p>No host and no project: what is asserted here is that a call can be named, written down and taken
 * apart, which is everything the contract's half of {@code 33-plugin-java.md} is.
 */
class ModelCallTest {

    private static final ValueType TEXT = ValueType.of("TEXT").label("Text").source("String").build();

    /** The stand-in for a plugin's own bot-facing class. Read for its name, never loaded. */
    public static final class Activities {
    }

    private static ModelCall declare() {
        return ModelCall.of(Activities.class, "declare",
                ModelCall.Argument.body("body"),
                ModelCall.Argument.value("name", ValueForm.of(TEXT)),
                ModelCall.Argument.value("outcomes", ValueForm.listOf(ValueForm.of(TEXT))));
    }

    /**
     * A call already has a name in Java, so its identity is that name. Declaring a second one would be a
     * second thing to keep in step — the same rule {@code ValueContainer.factorySource()} follows.
     */
    @Test
    void aCallsIdentityIsHowItIsWrittenBeforeTheBracket() {
        assertEquals(Activities.class.getCanonicalName() + ".declare", declare().id());
        assertEquals("Activities", declare().simpleOwner());
    }

    @Test
    void theArgumentsAreKeptInTheOrderTheyAreTakenIn() {
        List<ModelCall.Argument> arguments = declare().arguments();
        assertEquals(3, arguments.size());
        assertInstanceOf(ModelCall.Argument.Body.class, arguments.get(0));
        assertInstanceOf(ModelCall.Argument.Value.class, arguments.get(1));
        assertEquals("outcomes", arguments.get(2).name());
    }

    /**
     * A call that cannot be written down is a mistake in the plugin, and there is a moment at startup to
     * notice it in. Everything a <em>user</em> can get wrong answers empty instead; this cannot come from a
     * user at all.
     */
    @Test
    void aMethodNameThatIsNotOneIsRefusedAtOnce() {
        assertThrows(IllegalArgumentException.class, () -> ModelCall.of(Activities.class, "de clare"));
        assertThrows(IllegalArgumentException.class, () -> ModelCall.of(Activities.class, ""));
        assertThrows(IllegalArgumentException.class, () -> ModelCall.of(Activities.class, "2fast"));
        assertThrows(NullPointerException.class, () -> ModelCall.of(null, "declare"));
    }

    @Test
    void aMethodReferenceIsTwoNamesAndNothingElse() {
        MethodRef body = new MethodRef("com.example.bot.Collect", "body");
        assertEquals("Collect", body.simpleOwner());
        assertTrue(body.isPresent());
        assertFalse(new MethodRef("com.example.bot.Collect", "").isPresent());
        assertFalse(new MethodRef(null, null).isPresent());
    }

    @Test
    void aStatementNamesItsCallAndCarriesItsArgumentsInOrder() {
        ModelStatement statement = ModelStatement.of(declare(),
                new MethodRef("com.example.bot.Collect", "body"), "Collect", List.of("NOTHING_LEFT"));

        assertEquals(declare().id(), statement.call());
        assertEquals(3, statement.arguments().size());
        assertEquals("Collect", statement.arguments().get(1));
    }

    /**
     * A null argument is not writable, but a {@code NullPointerException} thrown out of a record constructor
     * is the wrong place to say so: the writer knows which argument it was and can name it.
     */
    @Test
    void aNullArgumentSurvivesAsFarAsTheWriter() {
        ModelStatement statement = new ModelStatement("x.y", Arrays.asList("a", null));
        assertEquals(2, statement.arguments().size());
        assertEquals(null, statement.arguments().get(1));
        assertThrows(UnsupportedOperationException.class, () -> statement.arguments().add("b"));
    }
}
