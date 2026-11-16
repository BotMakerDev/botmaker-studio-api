// SPDX-FileCopyrightText: 2026 LiQiyeDev
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package com.botmaker.plugin.api.params;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClaimsTest {

    /** A run with no agent: no other run to meet, so every claim is granted. */
    @Test
    void withNoAgentEveryClaimIsGrantedAgain() {
        assertTrue(Claims.claim("guild:north"));
        assertTrue(Claims.claim("guild:north"), "held twice by one run");
        assertDoesNotThrow(() -> Claims.release("guild:north"));
        assertDoesNotThrow(() -> Claims.release("never-held"));
    }

    @Test
    void aBlankKeyIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> Claims.claim(" "));
        assertThrows(IllegalArgumentException.class, () -> Claims.claim(null));
        assertThrows(IllegalArgumentException.class, () -> Claims.release(""));
    }

    /**
     * The agent's service answers every claim once served, and a second one is refused. In a class loader of its own,
     * since a service stays for the JVM's life and the test above needs none.
     */
    @Test
    void aServedServiceAnswersAndASecondIsRefused() throws Exception {
        URL classes = Claims.class.getProtectionDomain().getCodeSource().getLocation();
        try (URLClassLoader loader = new URLClassLoader(new URL[]{classes}, ClassLoader.getPlatformClassLoader())) {
            Class<?> claims = loader.loadClass(Claims.class.getName());
            Class<?> service = loader.loadClass(Claims.Service.class.getName());
            Set<String> held = new HashSet<>(Set.of("guild:taken"));
            Object served = Proxy.newProxyInstance(loader, new Class<?>[]{service}, (proxy, method, args) -> switch (method.getName()) {
                case "claim" -> !held.contains((String) args[0]) && held.add((String) args[0]);
                case "release" -> {
                    held.remove((String) args[0]);
                    yield null;
                }
                default -> throw new UnsupportedOperationException(method.getName());
            });
            Method serve = claims.getMethod("serve", service);
            Method claim = claims.getMethod("claim", String.class);
            Method release = claims.getMethod("release", String.class);

            serve.invoke(null, served);
            assertFalse((Boolean) claim.invoke(null, "guild:taken"));
            assertTrue((Boolean) claim.invoke(null, "guild:north"));
            assertFalse((Boolean) claim.invoke(null, "guild:north"), "this service holds a key once");
            release.invoke(null, "guild:north");
            assertTrue((Boolean) claim.invoke(null, "guild:north"));

            InvocationTargetException again = assertThrows(InvocationTargetException.class, () -> serve.invoke(null, served));
            assertInstanceOf(IllegalStateException.class, again.getCause());
            assertEquals(Set.of("guild:taken", "guild:north"), held);
        }
    }
}
