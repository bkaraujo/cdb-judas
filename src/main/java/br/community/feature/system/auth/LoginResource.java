package br.community.feature.system.auth;

import br.commons.Logger;
import br.community.core.web.security.AccessTokenStore;
import br.community.core.web.security.AuthUserDetails;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@NullMarked
@RequiredArgsConstructor
public class LoginResource {

    public static final String TOKEN_HEADER = "X-Access-Token";
    public static final String USER_ID_HEADER = "X-User-Id";

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenStore tokenStore;

    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        try {
            val userDetails = (AuthUserDetails) userDetailsService.loadUserByUsername(request.username());
            if (!passwordEncoder.matches(request.password(), userDetails.getPassword())) {
                Logger.debug("LOGIN => invalid password for '%s'", request.username());
                return ResponseEntity.status(401).build();
            }
            val token = tokenStore.issue(userDetails.getId());
            response.setHeader(TOKEN_HEADER, token);
            response.setHeader(USER_ID_HEADER, userDetails.getId());
            Logger.debug("LOGIN => '%s' (%s) issued token", request.username(), userDetails.getId());
            return ResponseEntity.ok().build();
        } catch (UsernameNotFoundException e) {
            Logger.debug("LOGIN => user '%s' not found", request.username());
            return ResponseEntity.status(401).build();
        }
    }
}
