package com.sorokaandriy.event_service.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HeaderAuthenticationFilterTest {

    private static final String USER_ID = "11111111-1111-1111-1111-111111111111";

    private HeaderAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new HeaderAuthenticationFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void setsAuthenticationFromUserHeadersWithRolePrefix() throws Exception {
        request.addHeader("X-User-Id", USER_ID);
        request.addHeader("X-User-Role", "ORGANIZER");

        filter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(USER_ID);
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ORGANIZER");
        assertThat(authentication.getDetails()).isNotNull();
        assertThat(filterChain.getRequest()).isSameAs(request);
    }

    @Test
    void continuesChainWithoutAuthenticationWhenUserIdHeaderIsMissing() throws Exception {
        request.addHeader("X-User-Role", "ORGANIZER");

        filter.doFilter(request, response, filterChain);

        assertThat(filterChain.getRequest()).isSameAs(request);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void continuesChainWithoutAuthenticationWhenUserIdHeaderIsBlank() throws Exception {
        request.addHeader("X-User-Id", "   ");
        request.addHeader("X-User-Role", "ORGANIZER");

        filter.doFilter(request, response, filterChain);

        assertThat(filterChain.getRequest()).isSameAs(request);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doesNotOverrideExistingAuthentication() throws Exception {
        UsernamePasswordAuthenticationToken existing = new UsernamePasswordAuthenticationToken(
                "existing-user", null, List.of(() -> "ROLE_ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(existing);
        request.addHeader("X-User-Id", USER_ID);
        request.addHeader("X-User-Role", "ORGANIZER");

        filter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isSameAs(existing);
        assertThat(authentication.getPrincipal()).isEqualTo("existing-user");
        assertThat(filterChain.getRequest()).isSameAs(request);
    }
}
