package br.community.context.monetary;

import br.community.context.monetary._0_domain.event.AccountEvents;
import br.community.context.monetary._0_domain.model.Account;
import br.community.context.monetary._1_application.event.AccountEventListener;
import br.community.context.monetary._1_application.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AccountEventListenerTest {

    private InMemoryRepositories.Accounts accountRepo;
    private AccountEventListener listener;

    @BeforeEach
    void setUp() {
        accountRepo = new InMemoryRepositories.Accounts();
        listener = new AccountEventListener(new AccountService(accountRepo));
    }

    private Account seedChecking(String color) {
        UUID id = UUID.randomUUID();
        Account acc = new Account(id, "Banco", Account.Type.CHECKING,
                new BigDecimal("100.00"), color, true, null);
        return accountRepo.save(acc);
    }

    @Test
    @DisplayName("onAccountUpdated propaga cor para cartões vinculados")
    void propagatesColorToLinkedCreditCards() {
        Account checking = seedChecking("#112233");
        UUID c1 = UUID.randomUUID();
        UUID c2 = UUID.randomUUID();
        accountRepo.save(new Account(c1, "C1", Account.Type.CREDIT_CARD,
                new BigDecimal("100.00"), "#112233", true, checking.id()));
        accountRepo.save(new Account(c2, "C2", Account.Type.CREDIT_CARD,
                new BigDecimal("200.00"), "#112233", true, checking.id()));

        // Simula conta atualizada com nova cor
        Account updatedChecking = new Account(
                checking.id(), "Banco", Account.Type.CHECKING,
                new BigDecimal("100.00"), "#FF0000", true, null);
        accountRepo.save(updatedChecking);

        listener.onAccountUpdated(new AccountEvents.Updated(updatedChecking));

        Account updatedC1 = accountRepo.findById(c1).orElseThrow();
        Account updatedC2 = accountRepo.findById(c2).orElseThrow();
        assertEquals("#FF0000", updatedC1.color());
        assertEquals("#FF0000", updatedC2.color());
    }

    @Test
    @DisplayName("onAccountUpdated não altera cartões que já têm a mesma cor")
    void doesNotUpdateCardsWithSameColor() {
        Account checking = seedChecking("#112233");
        UUID c1 = UUID.randomUUID();
        Account card = new Account(c1, "C1", Account.Type.CREDIT_CARD,
                new BigDecimal("100.00"), "#112233", true, checking.id());
        accountRepo.save(card);

        listener.onAccountUpdated(new AccountEvents.Updated(checking));

        // Cartão permanece inalterado (mesma instância no repo)
        Account unchanged = accountRepo.findById(c1).orElseThrow();
        assertEquals("#112233", unchanged.color());
    }

    @Test
    @DisplayName("onAccountUpdated não afeta cartões de outras contas")
    void doesNotAffectCardsFromOtherAccounts() {
        Account checking1 = seedChecking("#112233");
        Account checking2 = seedChecking("#AABBCC");
        UUID c1 = UUID.randomUUID();
        accountRepo.save(new Account(c1, "C1", Account.Type.CREDIT_CARD,
                new BigDecimal("100.00"), "#AABBCC", true, checking2.id()));

        // Atualiza checking1 com nova cor
        Account updated = new Account(
                checking1.id(), "Banco", Account.Type.CHECKING,
                new BigDecimal("100.00"), "#FF0000", true, null);
        accountRepo.save(updated);

        listener.onAccountUpdated(new AccountEvents.Updated(updated));

        // Cartão de checking2 não é afetado
        Account unchanged = accountRepo.findById(c1).orElseThrow();
        assertEquals("#AABBCC", unchanged.color());
    }
}
