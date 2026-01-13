package org.rutebanken.tiamat.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.rutebanken.helper.organisation.user.UserInfoExtractor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UsernameFetcherTest {

    private final UserInfoExtractor userInfoExtractor = mock(UserInfoExtractor.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final UsernameFetcher usernameFetcher = new UsernameFetcher(userInfoExtractor, request);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void headerTakesPriorityOverAuthenticatedName() {
        authenticate();
        when(request.getHeader("X-Hasura-User-Id")).thenReturn("header-user");

        assertEquals("header-user", usernameFetcher.getUserNameForAuthenticatedUser());
        verifyNoInteractions(userInfoExtractor);
    }

    @Test
    void missingHeaderFallsBackToAuthenticatedName() {
        authenticate();
        when(userInfoExtractor.getPreferredName()).thenReturn("authenticated-user");

        assertEquals("authenticated-user", usernameFetcher.getUserNameForAuthenticatedUser());
    }

    @Test
    void emptyHeaderFallsBackToAuthenticatedName() {
        authenticate();
        when(request.getHeader("X-Hasura-User-Id")).thenReturn("");
        when(userInfoExtractor.getPreferredName()).thenReturn("authenticated-user");

        assertEquals("authenticated-user", usernameFetcher.getUserNameForAuthenticatedUser());
    }

    @Test
    void unavailableRequestFallsBackToAuthenticatedName() {
        authenticate();
        when(request.getHeader("X-Hasura-User-Id")).thenThrow(new IllegalStateException("No request"));
        when(userInfoExtractor.getPreferredName()).thenReturn("authenticated-user");

        assertEquals("authenticated-user", usernameFetcher.getUserNameForAuthenticatedUser());
    }

    @Test
    void missingHeaderAndAuthenticationReturnsNull() {
        assertNull(usernameFetcher.getUserNameForAuthenticatedUser());
        verifyNoInteractions(userInfoExtractor);
    }

    @Test
    void existingConstructorRetainsAuthenticatedFallback() {
        authenticate();
        when(userInfoExtractor.getPreferredName()).thenReturn("authenticated-user");

        assertEquals("authenticated-user", new UsernameFetcher(userInfoExtractor).getUserNameForAuthenticatedUser());
    }

    private void authenticate() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(mock(Jwt.class));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}