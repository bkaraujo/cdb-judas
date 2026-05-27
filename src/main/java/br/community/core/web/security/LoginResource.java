package br.community.core.web.security;

import br.commons.Logger;
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

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenStore tokenStore;

    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        try {
            val userDetails = userDetailsService.loadUserByUsername(request.username());
            if (!passwordEncoder.matches(request.password(), userDetails.getPassword())) {
                Logger.debug("LOGIN => invalid password for '%s'", request.username());
                return ResponseEntity.status(401).build();
            }
            val token = tokenStore.issue(request.username());
            response.setHeader(TOKEN_HEADER, token);
            Logger.debug("LOGIN => '%s' issued token", request.username());
            return ResponseEntity.ok().build();
        } catch (UsernameNotFoundException e) {
            Logger.debug("LOGIN => user '%s' not found", request.username());
            return ResponseEntity.status(401).build();
        }
    }
}
