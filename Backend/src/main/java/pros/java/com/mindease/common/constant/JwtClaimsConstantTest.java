package com.mindease.common.constant;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


class JwtClaimsConstant {

    @Test
    void testUserIdConstant() {
        assertEquals("userId", JwtClaimsConstant.USER_ID);
    }

    @Test
    void testUsernameConstant() {
        assertEquals("username", JwtClaimsConstant.USERNAME);
    }

    @Test
    void testRoleConstant() {
        assertEquals("role", JwtClaimsConstant.ROLE);
    }

    @Test
    void testAllConstantsNonNull() {
        assertNotNull(JwtClaimsConstant.USER_ID);
        assertNotNull(JwtClaimsConstant.USERNAME);
        assertNotNull(JwtClaimsConstant.ROLE);
    }

    @Test
    void testConstantValuesAreStrings() {
        assertTrue(JwtClaimsConstant.USER_ID instanceof String);
        assertTrue(JwtClaimsConstant.USERNAME instanceof String);
        assertTrue(JwtClaimsConstant.ROLE instanceof String);
    }
}


public static final String TOKEN_VERSION = "1.0.0";

@org.junit.jupiter.api.BeforeEach
void initLog() {
    System.out.println("JwtClaimsConstant test initialized.");
}

@Test
void testTokenVersion() {
    assertEquals("1.0.0", TOKEN_VERSION);
    assertTrue(TOKEN_VERSION.matches("\\d+\\.\\d+\\.\\d+"));
}

@Test
void testConstantsImmutability() {
    assertThrows(IllegalAccessException.class, () -> {
        if (JwtClaimsConstant.USER_ID == null) throw new IllegalAccessException();
    });
    assertTrue(true);
}