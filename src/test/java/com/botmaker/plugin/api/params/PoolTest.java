package com.botmaker.plugin.api.params;

import com.botmaker.plugin.api.value.Ref;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PoolTest {

    enum Hero { ARIA, BRAM, CYRA }

    /** An open set: a class whose constants are its own static fields. */
    static final class Account {
        public static final Account MAIN = new Account();
        public static final Account ALT = new Account();
        public static final String NOT_ONE = "x";
    }

    interface Named {}

    enum Leader implements Named { ARIA, BRAM }

    @Param
    public static final class Farm {
        public String account() { return "main"; }
        public List<Hero> team() { return List.of(Hero.ARIA); }
        public Named leader() { return Leader.ARIA; }
        public int level() { return 1; }
    }

    @Test
    void aParameterOfATypeAboveTheElementsIsFilledAndAnotherIsRefused() {
        assertEquals("Pool.of(Leader.ARIA, Leader.BRAM).fills(Farm::leader)",
                Pool.of(Leader.ARIA, Leader.BRAM).fills(Farm::leader).toString());
        assertEquals(List.of(1, 2), Pool.of(1, 2).fills(Farm::level).elements(), "a primitive parameter, boxed");
        assertThrows(IllegalArgumentException.class, () -> Pool.of(Hero.ARIA).fills(Farm::account));
        assertThrows(IllegalArgumentException.class, () -> Pool.of(Hero.ARIA).take(1).fills(Farm::leader));
    }

    @Test
    void aMethodThatIsNotAGroupsParameterIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> Pool.of("a").fills(Object::toString));
        assertThrows(IllegalArgumentException.class, () -> Pool.of(1).fills(Integer::bitCount), "a static method");
    }

    @Test
    void anEmptyPoolOrATakeOfMoreThanItListsIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> Pool.of());
        assertThrows(IllegalArgumentException.class, () -> Pool.of("a", "b").take(3));
    }

    @Test
    void ofKeepsItsElementsInOrderAndFillsByMethodReference() throws Exception {
        Pool<String> pool = Pool.of("main", "alt-1", "alt-2").fills(Farm::account);
        assertEquals(List.of("main", "alt-1", "alt-2"), pool.elements());
        Method account = Farm.class.getMethod("account");
        assertEquals(account, pool.filled().orElseThrow());
        assertEquals(0, pool.taken());
        assertEquals(1, pool.heldBy());
        assertFalse(pool.rotates());
    }

    @Test
    void allTakesAnEnumsConstantsOrAnOpenSetsOwn() {
        assertEquals(List.of(Hero.ARIA, Hero.BRAM, Hero.CYRA), Pool.all(Hero.class).elements());
        assertEquals(List.of(Account.MAIN, Account.ALT), Pool.all(Account.class).elements());
        assertEquals(Hero.class, Pool.all(Hero.class).allOf().orElseThrow());
    }

    @Test
    void takeMakesAPoolOfListsFillingAListParameter() throws Exception {
        Pool<List<Hero>> pool = Pool.all(Hero.class).take(3).fills(Farm::team);
        assertEquals(3, pool.taken());
        assertEquals(Farm.class.getMethod("team"), pool.filled().orElseThrow());
        assertThrows(IllegalStateException.class, () -> Pool.all(Hero.class).take(2).take(1));
        assertThrows(IllegalArgumentException.class, () -> Pool.all(Hero.class).take(0));
    }

    @Test
    void eachAndRotateAreKeptWhereverTheyAreSaid() {
        Pool<String> pool = Pool.of("a", "b").each(2).fills(Farm::account).rotate();
        assertEquals(2, pool.heldBy());
        assertTrue(pool.rotates());
        assertEquals(pool, Pool.of("a", "b").fills(Farm::account).rotate().each(2));
        assertThrows(IllegalArgumentException.class, () -> Pool.of("a").each(0));
    }

    @Test
    void aLambdaIsRefused() {
        Ref.Of1<Farm, String> lambda = farm -> farm.account();
        assertThrows(IllegalArgumentException.class, () -> Pool.of("a").fills(lambda));
    }

    @Test
    void onlyOneParameterIsFilled() {
        Pool<String> pool = Pool.of("a").fills(Farm::account);
        assertThrows(IllegalStateException.class, () -> pool.fills(Farm::account));
    }

    @Test
    void aNullOrRepeatedElementIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> Pool.of("a", null));
        assertThrows(IllegalArgumentException.class, () -> Pool.of("a", "a"));
    }

    @Test
    void itSaysItselfAsItIsWritten() {
        assertEquals("Pool.all(Hero.class).take(3).fills(Farm::team).rotate()",
                Pool.all(Hero.class).take(3).fills(Farm::team).rotate().toString());
        assertEquals("Pool.of(\"main\", \"alt\").fills(Farm::account).each(2)",
                Pool.of("main", "alt").fills(Farm::account).each(2).toString());
    }
}
