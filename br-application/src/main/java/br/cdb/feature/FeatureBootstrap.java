package br.cdb.feature;

import br.cdb.core.web.HTTPApi;
import br.cdb.feature.f000.F000Module;
import br.cdb.feature.f000._1_application.service.UserGuards;
import br.cdb.feature.f001.F001Module;
import br.cdb.feature.f002.F002Module;
import br.cdb.feature.f002._1_application.service.BalanceService;
import br.cdb.feature.f003.F003Module;
import br.cdb.feature.f004.F004Module;
import br.cdb.feature.f005.F005Module;
import br.cdb.feature.f006.F006Module;
import br.cdb.feature.f007.F007Module;
import br.cdb.feature.f009.F009Module;
import br.cdb.feature.f010.F010Module;
import br.cdb.feature.f999.F999Module;
import br.commons.Logger;
import br.commons.annotation.Lifecycle;
import br.commons.framework.cdi.Context;
import br.commons.tools.Meta;
import br.commons.tools.Threads;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.jspecify.annotations.NullMarked;

import java.time.Duration;
import java.util.List;

/**
 * Composition root das fatias — única costura CDI↔{@link Context} do pacote {@code feature} (a outra
 * é {@code br.cdb.core.CoreModule}, que publica o {@code DataSource} antes desta, em
 * {@code @Priority(0)}). Os {@code FNNNModule} são classes puras ({@link Lifecycle}) montadas pelo
 * {@link Context} de {@code br-commons}: nenhuma delas conhece Quarkus/CDI.
 *
 * <p>A ordem de inicialização é a ordem da lista (antes: {@code @Priority} espalhado por módulo).
 * {@code F999Module} vem por último porque cria o usuário {@code admin}, o que dispara pelo
 * {@code MessageBus} o seed de categorias registrado por {@code F005Module}.
 *
 * <p>Os dois beans CDI que as engines Context-wired precisam entram no {@link Context} antes de
 * qualquer módulo: {@link UserGuards} (guarda anti-IDOR, {@code @RequestScoped} — o que se registra é
 * o api proxy, então cada chamada resolve a instância da requisição corrente) e
 * {@link HTTPApi} (leitura cross-slice). Nenhum dos dois toca porta de repositório ao ser
 * construído — quem toca (ex.: {@code UserService}, que resolve {@code PersonRepository}/
 * {@code UserRepository} em campo) fica fora daqui e é montado pelo próprio módulo que o usa.
 *
 * <p>Vive em {@code f999} por ser o único pacote autorizado a importar todas as fatias
 * ({@code ArchitectureTest.feature_slices_must_not_depend_on_sibling_slices}).
 */
@NullMarked
@ApplicationScoped
public class FeatureBootstrap {

    /** Folga generosa: o servidor sobe logo depois deste observer, mas boot frio em disco lento demora. */
    private static final Duration SERVER_READY_TIMEOUT = Duration.ofSeconds(60);

    void onStart(@Observes @Priority(1) StartupEvent ev, UserGuards guards, HTTPApi httpApi) {
        Context.set(UserGuards.class, () -> guards);
        Context.set(HTTPApi.class, () -> httpApi);

        initialize(new F000Module());

        List.of(
                new F001Module(),
                new F002Module(),
                new F003Module(),
                new F004Module(),
                new F005Module(),
                new F006Module(),
                new F007Module(),
                new F009Module(),
                new F010Module()
        ).forEach(FeatureBootstrap::initialize);

        initialize(new F999Module());

        // Depois de todos os módulos: os snapshots de F002_BALANCE gravados por versões anteriores
        // podem ter sido calculados por outra regra (ex.: compra de cartão debitada na data da
        // compra, antes de InvoiceCycle). Recomputa tudo uma vez — sem isto só uma escrita na conta
        // corrigiria o valor.
        Threads.virtual(() -> recomputeBalances(httpApi));
    }

    /**
     * Fora da thread de boot, e só depois de a porta abrir: o recálculo lê as transações pela API
     * pública de {@code f006} ({@code F006Api}, HTTP real), e o servidor desta JVM só passa a aceitar
     * conexões <b>depois</b> que este observer retorna — chamar aqui dentro dava
     * {@link java.net.ConnectException} e derrubava a aplicação inteira no boot.
     *
     * <p>Falhar aqui não pode mais derrubar nada: no pior caso os snapshots seguem com o valor da
     * regra antiga até a próxima escrita na conta ou até o job de reconciliação de {@code f999}.
     */
    private static void recomputeBalances(HTTPApi httpApi) {
        Threads.name("balance-recompute-boot");
        if (!httpApi.awaitAvailable(SERVER_READY_TIMEOUT)) {
            Logger.warn("Servidor HTTP não subiu em %s: recálculo de saldos do boot pulado", SERVER_READY_TIMEOUT);
            return;
        }

        try {
            Context.tryGet(BalanceService.class).recomputeAll();
        } catch (RuntimeException e) {
            Logger.warn("Falha no recálculo de saldos do boot: %s", e.toString());
        }
    }

    private static void initialize(Lifecycle module) {
        module
                .initialize()
                .ifFailure(error -> Logger.fatal("Falha ao iniciar %s: %s", Meta.fqn(module), error));
    }
}
