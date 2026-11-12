package com.botmaker.plugin.api.managed;

import com.botmaker.plugin.api.Dialogs;
import com.botmaker.plugin.api.StudioServices;
import com.botmaker.plugin.api.Theme;
import com.botmaker.plugin.api.source.ManagedMethod;
import com.botmaker.plugin.api.source.ManagedOpenSet;
import com.botmaker.plugin.api.source.ManagedValue;
import com.botmaker.plugin.api.source.PluginValues;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A value of this PC: declared as a bot's is, told apart by its holder enum. */
class HostHolderTest {

    @ManagedMarker
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface PcValue {

        Id value();

        enum Id implements ManagedId<Holder> { SETTINGS, SPEED }

        enum Holder { SDK }
    }

    /** This PC's marker: a marker's {@code value()} is one enum, so host values have their own. */
    @ManagedMarker
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface PcHostValue {

        Id value();

        enum Id implements ManagedId<Holder> { EMULATORS, INSTANCES, SPEED }

        enum Holder implements HostHolder { EMULATORS, INSTANCES }
    }

    @Test
    void aHostHoldersValuesAreOnHostAndABotsAreNot() {
        ManagedMethod<String> emulators = ManagedValue.method(PcHostValue.Id.EMULATORS)
                .in(PcHostValue.Holder.EMULATORS)
                .holds(String.class, "").because("This PC's emulators. Change them in Emulators.");
        ManagedOpenSet.OfConstants<String> instances = ManagedValue.openSet(PcHostValue.Id.INSTANCES)
                .of(String.class).in(PcHostValue.Holder.INSTANCES).because("This PC's instances.");
        ManagedMethod<String> settings = ManagedValue.method(PcValue.Id.SETTINGS).in(PcValue.Holder.SDK)
                .holds(String.class, "").because("The bot's settings.");

        assertTrue(emulators.onHost());
        assertTrue(instances.onHost());
        assertFalse(settings.onHost());
        assertEquals("Emulators", emulators.holder());
        assertEquals("Instances", instances.holder());
    }

    @Test
    void aParameterOnAHostHolderIsRefused() {
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> ManagedValue.param(PcHostValue.Id.SPEED));
        assertTrue(refused.getMessage().contains("HostHolder"), refused.getMessage());

        ManagedValue.param(PcValue.Id.SPEED);
    }

    /** Ids that reach {@code ManagedId} through an interface of the plugin's own. */
    interface HostIds extends ManagedId<PcHostValue.Holder> {
    }

    /** Ids whose holder is left a type variable: the host cannot read it, so {@code in} refuses a host holder. */
    interface AnyIds<H extends Enum<H>> extends ManagedId<H> {
    }

    @ManagedMarker
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface Indirect {

        Id value();

        enum Id implements HostIds { ACCOUNTS }
    }

    @ManagedMarker
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface Generic {

        Id value();

        enum Id implements AnyIds<PcHostValue.Holder> { ACCOUNTS }
    }

    @Test
    void anIdReachingManagedIdThroughAnInterfaceIsStillOnHost() {
        assertTrue(ManagedValue.method(Indirect.Id.ACCOUNTS).in(PcHostValue.Holder.EMULATORS)
                .holds(String.class, "").because("Mine.").onHost());
        assertThrows(IllegalArgumentException.class, () -> ManagedValue.param(Indirect.Id.ACCOUNTS));
    }

    @Test
    void aHolderTheIdEnumHidesIsRefusedRatherThanMisfiled() {
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> ManagedValue.method(Generic.Id.ACCOUNTS).in(PcHostValue.Holder.EMULATORS));
        assertTrue(refused.getMessage().contains("ManagedId<Holder>"), refused.getMessage());
    }

    @Test
    void aHostValueIsNeverOpenedOnlyNorClaimedByARun() {
        assertThrows(IllegalArgumentException.class, () -> ManagedValue.method(PcHostValue.Id.EMULATORS).openedOnly());

        ManagedMethod<String> emulators = ManagedValue.method(PcHostValue.Id.EMULATORS)
                .in(PcHostValue.Holder.EMULATORS).holds(String.class, "").because("Mine.");
        ManagedValues.claim(emulators, value -> {
            throw new AssertionError("no run reads a value of this PC");
        });
    }

    @Test
    void aHostWithNoFolderAnswersNone() {
        StudioServices services = new StudioServices() {
            @Override
            public Path projectDir() {
                return Path.of(".");
            }

            @Override
            public Path resourcesDir() {
                return Path.of(".");
            }

            @Override
            public Theme theme() {
                return Theme.PLAIN;
            }

            @Override
            public Dialogs dialogs() {
                return Dialogs.NONE;
            }
        };

        assertSame(PluginValues.NONE, services.hostValues());
    }
}
