package br.commons.i18n;

import org.jspecify.annotations.NullMarked;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Resolve chave de tradução → texto no locale pedido, bundle {@code messages} (raiz pt-BR,
 * {@code messages_en_US.properties} para inglês — ver {@code br-application/src/main/resources}).
 *
 * <p>Chave sem entrada em nenhum bundle não estoura: o {@code format} roda em cima da própria chave.
 * É o que permite migração incremental de {@link br.commons.business.BusinessError} — um call site
 * ainda não migrado para chave continua passando a prosa pt-BR original como "chave", que nunca bate
 * em nenhum bundle e volta formatada do jeito que sempre voltou.
 */
@NullMarked
public abstract class Messages {

    private Messages() {}

    public static String of(String key, Locale locale, Object... args) {
        try {
            var bundle = ResourceBundle.getBundle("messages", locale, Messages.class.getClassLoader());
            var pattern = bundle.getString(key);
            return args.length == 0 ? pattern : String.format(locale, pattern, args);
        } catch (MissingResourceException e) {
            return args.length == 0 ? key : String.format(locale, key, args);
        }
    }
}
