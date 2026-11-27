// SPDX-FileCopyrightText: 2026 LiQiyeDev
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package com.botmaker.plugin.api.run;

import com.botmaker.plugin.api.DeclaredPlugin;
import com.botmaker.plugin.api.PoolHolder;
import com.botmaker.plugin.api.StudioPlugin;
import com.botmaker.plugin.api.StudioServices;
import com.botmaker.plugin.api.managed.ManagedId;
import com.botmaker.plugin.api.managed.ManagedMarker;
import com.botmaker.plugin.api.source.ManagedParam;
import com.botmaker.plugin.api.source.ManagedValue;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginPoolTest {

    @ManagedMarker
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface PcValue {

        Id value();

        enum Id implements ManagedId<Holder> { DEVICE, ACCOUNT }

        enum Holder { RUNNING }
    }

    public static String first() {
        return "pc-1";
    }

    static final ManagedParam<String> DEVICE = ManagedValue.param(PcValue.Id.DEVICE).in(PcValue.Holder.RUNNING)
            .startsAs(PluginPoolTest::first).shown("").because("Mine.");
    static final ManagedParam<String> ACCOUNT = ManagedValue.param(PcValue.Id.ACCOUNT).in(PcValue.Holder.RUNNING)
            .startsAs(PluginPoolTest::first).shown("").because("Mine.");

    static List<String> devices(StudioServices services) {
        return Arrays.asList("pc-1", "pc-2", null, "pc-1", "pc-3");
    }

    /** A device goes with the account of its own number only. */
    static List<String> linked(List<String> candidates, PluginPool.Picks picks, StudioServices services) {
        Optional<String> account = picks.of(ACCOUNT);
        if (account.isEmpty()) return null;
        List<String> left = new ArrayList<>(candidates);
        left.removeIf(device -> !device.endsWith(account.get().substring(account.get().length() - 1)));
        left.add("pc-9");
        return left;
    }

    static final List<String> RAN = new ArrayList<>();

    static List<PoolOffer> offers(PluginPool.Shortfall shortfall, StudioServices services) {
        return Arrays.asList(PoolOffer.of(" Create " + shortfall.missing() + " devices ",
                given -> RAN.add("created " + shortfall.missing())), null);
    }

    static PluginPool.Shortfall shortfall(int asked, int missing) {
        return new PluginPool.Shortfall() {
            @Override
            public int asked() {
                return asked;
            }

            @Override
            public int missing() {
                return missing;
            }
        };
    }

    static PluginPool.Picks account(String name) {
        return new PluginPool.Picks() {
            @Override
            public <V> Optional<V> of(ManagedParam<V> parameter) {
                return parameter == ACCOUNT ? Optional.ofNullable(parameter.cast(name)) : Optional.empty();
            }
        };
    }

    @Test
    void candidatesAreReadOnceEachAndNeverNull() {
        PluginPool<String> pool = PluginPool.fills(DEVICE).from(PluginPoolTest::devices);
        assertEquals(DEVICE, pool.parameter());
        assertEquals(List.of("pc-1", "pc-2", "pc-3"), pool.candidates(null));
        assertEquals(List.of(), PluginPool.fills(DEVICE).from(services -> null).candidates(null));
        @SuppressWarnings({"unchecked", "rawtypes"})
        PluginPool<String> mistyped = PluginPool.fills(DEVICE).from(services -> (List) List.of(7, "pc-1"));
        assertEquals(List.of("pc-1"), mistyped.candidates(null), "a value that is not the parameter's is dropped");
    }

    @Test
    void aPoolThatDoesNotNarrowLeavesEveryCandidateAndOffersNothing() {
        PluginPool<String> pool = PluginPool.fills(DEVICE).from(PluginPoolTest::devices);
        assertEquals(List.of("pc-2", "pc-3"), pool.left(List.of("pc-2", "pc-3"), account("acc-2"), null));
        assertEquals(List.of(), pool.offers(shortfall(3, 1), null));
        assertEquals(List.of(), pool.left(null, PluginPool.Picks.NONE, null), "no candidates read as none");
        assertEquals(List.of("pc-2"), pool.left(Arrays.asList("pc-2", null), PluginPool.Picks.NONE, null));
    }

    @Test
    void narrowingLeavesOnlyCandidatesItWasGiven() {
        PluginPool<String> pool = PluginPool.fills(DEVICE).from(PluginPoolTest::devices)
                .narrowedBy(PluginPoolTest::linked);
        assertEquals(List.of("pc-2"), pool.left(List.of("pc-1", "pc-2", "pc-3"), account("acc-2"), null),
                "pc-9 was no candidate");
        assertEquals(List.of(), pool.left(List.of("pc-1"), PluginPool.Picks.NONE, null), "null reads as none");
        assertEquals(List.of(), pool.left(List.of("pc-1"), null, null), "no picks reads as a row with none");
    }

    @Test
    void anOfferRunsOnlyWhenItIsRun() {
        PluginPool<String> pool = PluginPool.fills(DEVICE).from(PluginPoolTest::devices)
                .offering(PluginPoolTest::offers);
        RAN.clear();
        List<PoolOffer> offers = pool.offers(shortfall(5, 2), null);
        assertEquals(List.of("Create 2 devices"), offers.stream().map(PoolOffer::title).toList());
        assertTrue(RAN.isEmpty(), "asking runs nothing");
        offers.getFirst().run(null);
        assertEquals(List.of("created 2"), RAN);
        assertThrows(IllegalArgumentException.class, () -> PoolOffer.of(" ", services -> { }));
        assertThrows(NullPointerException.class, () -> PoolOffer.of("Create", null));
    }

    @Test
    void eachStepReturnsACopyAndRefusesNothingGiven() {
        PluginPool<String> plain = PluginPool.fills(DEVICE).from(PluginPoolTest::devices);
        plain.narrowedBy(PluginPoolTest::linked).offering(PluginPoolTest::offers);
        assertEquals(List.of("pc-1"), plain.left(List.of("pc-1"), PluginPool.Picks.NONE, null));
        assertThrows(NullPointerException.class, () -> PluginPool.fills(null));
        assertThrows(NullPointerException.class, () -> PluginPool.fills(DEVICE).from(null));
        assertThrows(NullPointerException.class, () -> plain.narrowedBy(null));
        assertThrows(NullPointerException.class, () -> plain.offering(null));
    }

    static final class Pools implements PoolHolder {
        static final PluginPool<String> DEVICES = PluginPool.fills(DEVICE).from(PluginPoolTest::devices);
        static final PluginPool<String> ACCOUNTS = PluginPool.fills(ACCOUNT).from(services -> List.of("acc-1"));

        @Override
        public List<PluginPool<?>> pools() {
            return List.of(ACCOUNTS, DEVICES);
        }
    }

    static final class SameParameter implements PoolHolder {
        @Override
        public List<PluginPool<?>> pools() {
            return List.of(PluginPool.fills(DEVICE).from(services -> List.of("pc-4")));
        }
    }

    @Test
    void aCandidateGoesToOneRunUnlessThePoolSharesIt() {
        PluginPool<String> once = PluginPool.fills(DEVICE).from(PluginPoolTest::devices);
        PluginPool<String> shared = once.sharing(device -> switch (device) {
            case "pc-1" -> PluginPool.Sharing.ANY;
            case "pc-2" -> 0;
            case "pc-3" -> throw new IllegalStateException("unread");
            default -> 3;
        });

        assertEquals(1, once.sharedBy("pc-1"));
        assertEquals(PluginPool.Sharing.ANY, shared.sharedBy("pc-1"));
        assertEquals(1, shared.sharedBy("pc-2"), "under one reads as one");
        assertEquals(1, shared.sharedBy("pc-3"), "a sharing that throws shares nothing");
        assertEquals(3, shared.sharedBy("pc-4"));
        assertEquals(3, shared.narrowedBy(PluginPoolTest::linked).offering((s, services) -> List.of())
                .sharedBy("pc-4"), "the later steps keep it");
        assertThrows(NullPointerException.class, () -> once.sharing(null));
    }

    @Test
    void aCandidateHoldsTheKeysItsPoolClaimsWithIt() {
        PluginPool<String> once = PluginPool.fills(DEVICE).from(PluginPoolTest::devices);
        PluginPool<String> claiming = once.sharing(device -> PluginPool.Sharing.ANY)
                .claiming((device, services) -> device.equals("pc-1")
                        ? Arrays.asList("pointer:pc-1", null, " ", "pointer:pc-1") : null);

        assertEquals(List.of(), once.claimedWith("pc-1", null));
        assertEquals(List.of("pointer:pc-1"), claiming.claimedWith("pc-1", null), "each once, none blank");
        assertEquals(List.of(), claiming.claimedWith("pc-2", null), "null reads as none");
        assertEquals(List.of("pointer:pc-1"), claiming.narrowedBy(PluginPoolTest::linked)
                .offering((s, services) -> List.of()).sharing(device -> 1).claimedWith("pc-1", null),
                "the later steps keep it");
        assertEquals(PluginPool.Sharing.ANY, claiming.sharedBy("pc-1"), "it keeps the sharing");
        assertThrows(NullPointerException.class, () -> once.claiming(null));
    }

    @Test
    void aRowIsNotedOnlyWhenThePoolNotesIt() {
        PluginPool<String> once = PluginPool.fills(DEVICE).from(PluginPoolTest::devices);
        PluginPool<String> noting = once.noting((device, picks, services) -> switch (device) {
            case "pc-1" -> "  pc-1 last read other@example.com ";
            case "pc-2" -> " ";
            case "pc-5" -> "last read a@x.com\n  main signs in with b@x.com";
            case "pc-3" -> throw new IllegalStateException("unread");
            default -> picks.of(ACCOUNT).map(account -> "plays " + account).orElse(null);
        });

        assertEquals(Optional.empty(), once.noteOn("pc-1", null, null), "no noting, no note");
        assertEquals(Optional.of("pc-1 last read other@example.com"), noting.noteOn("pc-1", null, null));
        assertEquals(Optional.empty(), noting.noteOn("pc-2", null, null), "blank reads as none");
        assertEquals(Optional.of("last read a@x.com main signs in with b@x.com"), noting.noteOn("pc-5", null, null),
                "one line");
        assertEquals(Optional.empty(), noting.noteOn("pc-4", null, null), "null picks read as none, null as no note");
        assertThrows(IllegalStateException.class, () -> noting.noteOn("pc-3", null, null),
                "not caught: the host fails the dealing with the plugin named");
        assertEquals(Optional.of("pc-1 last read other@example.com"), noting.narrowedBy(PluginPoolTest::linked)
                .offering((s, services) -> List.of()).sharing(device -> 1).claiming((d, s) -> List.of())
                .noteOn("pc-1", null, null), "the later steps keep it");
        assertEquals(PluginPool.Sharing.ANY, once.sharing(device -> PluginPool.Sharing.ANY)
                .noting((d, p, s) -> null).sharedBy("pc-1"), "it keeps the sharing");
        assertThrows(NullPointerException.class, () -> once.noting(null));
    }

    @Test
    void poolsAreDeclaredByHolderInTheOrderARowIsDealt() {
        StudioPlugin plugin = new DeclaredPlugin(StudioPlugin.declare().pools(Pools.class).pools(Pools.class)) {};
        assertEquals(List.of(Pools.ACCOUNTS, Pools.DEVICES), plugin.pools());
        assertTrue(new DeclaredPlugin(StudioPlugin.declare()) {}.pools().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> StudioPlugin.declare().pools());
    }

    @Test
    void twoPoolsForOneParameterAreRefused() {
        StudioPlugin plugin = new DeclaredPlugin(StudioPlugin.declare().pools(Pools.class, SameParameter.class)) {};
        IllegalStateException refused = assertThrows(IllegalStateException.class, plugin::pools);
        assertTrue(refused.getMessage().contains("DEVICE"), refused.getMessage());
    }
}
