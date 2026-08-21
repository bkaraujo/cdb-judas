package br.commons.business;

import lombok.Getter;
import org.jspecify.annotations.NullMarked;

import java.util.Locale;

@Getter
@NullMarked
public class BusinessException extends RuntimeException {

    /** Locale de log/desenvolvimento — {@link #getMessage()} é pra stack trace e log interno, não pra
     *  resposta HTTP (essa resolve {@link BusinessError#render} no locale da requisição, ver
     *  {@code DomainExceptionMapper}). Time e logs são pt-BR, fixo, independente de quem pediu. */
    private static final Locale LOG_LOCALE = Locale.of("pt", "BR");

    private final BusinessError error;

    public BusinessException(BusinessError error) {
        super(error.render(LOG_LOCALE));
        this.error = error;
    }
}
