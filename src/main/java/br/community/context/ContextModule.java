package br.community.context;

import br.community.context.monetary.MonetaryModule;
import br.community.context.security.SecurityModule;
import br.community.context.shared.SharedModule;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@NullMarked
@Import({SharedModule.class, SecurityModule.class, MonetaryModule.class})
public class ContextModule {
}
