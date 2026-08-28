package br.cdb.core.web;

import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Quem age quando <b>não há requisição HTTP</b>. Os clientes {@code FNNNApi} falam com a fatia dona
 * por HTTP real ({@link HTTPApi}), e toda rota é {@code /api/{personId}/...} — logo é preciso uma
 * pessoa. Dentro de uma requisição ela vem de {@link HTTPRequest#user()}; fora dela (boot em
 * {@code FeatureBootstrap}, job {@code @Scheduled}, listener do {@code MessageBus}) não vinha de
 * lugar nenhum, e o {@code FNNNApi} estourava — o boot inteiro caía por causa disso.
 *
 * <p>Aqui o chamador declara a pessoa: {@code InternalCall.as(personId, () -> f006Api.algo())}. É a
 * mesma identidade de negócio das rotas, então o {@code OwnershipFilter} continua valendo — não é
 * bypass de autorização, é o dono da linha dizendo por quem age. O token sai de
 * {@code AccessTokenStore.ephemeral(personId)}, que minta um do nada quando não há sessão de
 * navegador viva (é o caso do boot).
 *
 * <p>{@link ThreadLocal} porque a chamada loopback é síncrona e roda inteira na thread do chamador;
 * o valor anterior é restaurado no {@code finally}, então aninhar {@code as(..)} é seguro.
 */
@NullMarked
public final class InternalCall {

    private static final ThreadLocal<@Nullable String> ACTOR = new ThreadLocal<>();

    private InternalCall() {}

    /** Pessoa declarada pelo chamador, ou {@code null} quando ninguém declarou. */
    public static @Nullable String personId() {
        return ACTOR.get();
    }

    public static <T> T as(String personId, Supplier<T> action) {
        val previous = ACTOR.get();
        ACTOR.set(personId);
        try {
            return action.get();
        } finally {
            if (previous == null) ACTOR.remove(); else ACTOR.set(previous);
        }
    }

    public static void as(String personId, Runnable action) {
        as(personId, () -> {
            action.run();
            return new Object();
        });
    }
}
