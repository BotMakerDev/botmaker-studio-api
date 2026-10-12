package com.botmaker.plugin.api.source;

import com.botmaker.plugin.api.StudioServices;
import com.botmaker.plugin.api.managed.ManagedMarker;
import com.botmaker.plugin.api.slot.TypeRef;
import com.botmaker.plugin.api.slot.ValueContext;
import com.botmaker.plugin.api.source.PluginValues.Member;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The typed {@link PluginValues} and {@link ValueContext#member(ManagedValue)} operations: a {@link Member} owns the
 * {@code "Group.NAME"} spelling, and each typed operation reaches a host through the id its declaration carries.
 */
class PluginValuesTest {

    @ManagedMarker
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface Mark {

        Id value();

        enum Id { NAMES }
    }

    private static final ManagedValue<String> NAMES =
            ManagedValue.openSet(Mark.Id.NAMES).of(String.class).in("Names").because("Mine.");

    @Test
    void aMemberIsSpelledAsItsEnumAndNameAndParsedBack() {
        assertEquals("ORE", Member.of("ORE").toString());
        assertEquals("Collect.WON", Member.of("Collect", "WON").toString());
        assertEquals(Member.of("Collect", "WON"), Member.parse("Collect.WON"));
        assertEquals(Member.of("ORE"), Member.parse("ORE"));
        assertTrue(Member.parse("Collect.WON").grouped());
        assertFalse(Member.of("ORE").grouped());
        assertEquals(Member.of("Collect", "LOST"), Member.of("Collect", "WON").renamed("LOST"));
        assertNotEquals(Member.of("WON"), Member.of("Collect", "WON"));
    }

    @Test
    void aMemberWithNoNameOrABlankEnumIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> Member.of(" "));
        assertThrows(IllegalArgumentException.class, () -> Member.of(" ", "WON"));
        assertThrows(IllegalArgumentException.class, () -> Member.parse(null));
        assertThrows(IllegalArgumentException.class, () -> Member.parse("a.b.c"));
        assertThrows(IllegalArgumentException.class, () -> Member.parse("Collect."));
    }

    @Test
    void eachTypedOperationReachesTheHostByTheDeclarationsId() {
        List<String> calls = new ArrayList<>();
        PluginValues host = new PluginValues() {
            @Override public List<String> ids() { return List.of(); }
            @Override public Optional<ValueContext> open(String id) { calls.add("open " + id); return Optional.empty(); }
            @Override public List<String> members(String id) { return List.of("ORE", "a.b.c", "Collect.WON"); }
            @Override public Optional<String> add(String id, String member, Object value) {
                calls.add("add " + id + " " + member + " " + value);
                return Optional.empty();
            }
            @Override public Optional<String> repoint(String id, String member, String replacement, String note) {
                calls.add("repoint " + member + " " + replacement);
                return Optional.empty();
            }
            @Override public List<String> methods(String id, String group) {
                calls.add("methods " + id + " " + group);
                return List.of();
            }
        };

        host.open(NAMES);
        host.add(NAMES, Member.of("ORE"), "ore.png");
        host.repoint(NAMES, Member.of("Collect", "WON"), Member.of("Collect", "LOST"), "");
        host.voidMethods();

        assertEquals(List.of("open " + NAMES.id(), "add " + NAMES.id() + " ORE ore.png",
                "repoint Collect.WON Collect.LOST", "methods null null"), calls);
        assertEquals(List.of(Member.of("ORE"), Member.of("Collect", "WON")), host.members(NAMES));
    }

    @Test
    void aHostWithNoSourceTreeRefusesTheTypedWritesToo() {
        assertEquals(Optional.of(PluginValues.NO_SOURCE_TREE), PluginValues.NONE.create(NAMES));
        assertEquals(Optional.of(PluginValues.NO_SOURCE_TREE), PluginValues.NONE.remove(NAMES, Member.of("ORE")));
        assertFalse(PluginValues.NONE.createVoidMethod("Bot", "goHome").written());
        assertTrue(PluginValues.NONE.members(NAMES).isEmpty());
    }

    @Test
    void aContextNamesItsMemberTypedAndWritesOneByReference() {
        List<String> written = new ArrayList<>();
        ValueContext ctx = new ValueContext() {
            @Override public TypeRef type() { return TypeRef.unresolved("String"); }
            @Override public <T> Optional<T> value(Class<T> type) { return Optional.empty(); }
            @Override public void set(Object value) { }
            @Override public String source() { return "Names.ORE"; }
            @Override public StudioServices services() { return null; }
            @Override public Optional<String> member(String id) { return Optional.of("Collect.WON"); }
            @Override public Optional<String> writeMember(String id, String member) {
                written.add(id + " " + member);
                return Optional.empty();
            }
        };

        assertEquals(Optional.of(Member.of("Collect", "WON")), ctx.member(NAMES));
        assertTrue(ctx.writeMember(NAMES, Member.of("ORE")).isEmpty());
        assertEquals(List.of(NAMES.id() + " ORE"), written);
        assertTrue(ctx.writeMember(NAMES, null).isPresent(), "no member is refused, not thrown");
    }
}
