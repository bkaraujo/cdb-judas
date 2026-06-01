package br.community.context.monetary;

import br.community.context.monetary._0_domain.event.MonetaryEvent;
import br.community.context.monetary._0_domain.model.AccountType;
import br.community.context.monetary._0_domain.model.MonetaryAccount;
import br.community.context.monetary._1_application.event.AccountEventListener;
import br.community.context.monetary._1_application.service.AccountService;
import br.community.context.monetary._1_application.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AccountEventListenerTest {

    private InMemoryRepositories.Accounts accountRepo;
    private InMemoryRepositories.Transactions transactionRepo;
    private AccountEventListener listener;

    @BeforeEach
    void setUp() {
        accountRepo = new InMemoryRepositories.Accounts();
        transactionRepo = new InMemoryRepositories.Transactions();
        listener = new AccountEventListener(
                new AccountService(accountRepo),
                new TransactionService(transactionRepo)
        );
    }

    private MonetaryAccount seedChecking(String color) {
        UUID id = UUID.randomUUID();
        MonetaryAccount acc = new MonetaryAccount(id, "Banco", AccountType.CHECKING,
                new BigDecimal("100.00"), color, true, null);
        return accountRepo.save(acc);
    }

    @Test
    @DisplayName("onAccountUpdated propaga cor para cartões vinculados")
    void propagatesColorToLinkedCreditCards() {
        MonetaryAccount checking = seedChecking("#112233");
        UUID c1 = UUID.randomUUID();
        UUID c2 = UUID.randomUUID();
        accountRepo.save(new MonetaryAccount(c1, "C1", AccountType.CREDIT_CARD,
                new BigDecimal("100.00"), "#112233", true, checking.id()));
        accountRepo.save(new MonetaryAccount(c2, "C2", AccountType.CREDIT_CARD,
                new BigDecimal("200.00"), "#112233", true, checking.id()));

        // Simula conta atualizada com nova cor
        MonetaryAccount updatedChecking = new MonetaryAccount(
                checking.id(), "Banco", AccountType.CHECKING,
                new BigDecimal("100.00"), "#FF0000", true, null);
        accountRepo.save(updatedChecking);

        listener.onAccountUpdated(new MonetaryEvent.AccountUpdated(updatedChecking));

        MonetaryAccount updatedC1 = accountRepo.findById(c1).orElseThrow();
        MonetaryAccount updatedC2 = accountRepo.findById(c2).orElseThrow();
        assertEquals("#FF0000", updatedC1.color());
        assertEquals("#FF0000", updatedC2.color());
    }

    @Test
    @DisplayName("onAccountUpdated não altera cartões que já têm a mesma cor")
    void doesNotUpdateCardsWithSameColor() {
        MonetaryAccount checking = seedChecking("#112233");
        UUID c1 = UUID.randomUUID();
        MonetaryAccount card = new MonetaryAccount(c1, "C1", AccountType.CREDIT_CARD,
                new BigDecimal("100.00"), "#112233", true, checking.id());
        accountRepo.save(card);

        listener.onAccountUpdated(new MonetaryEvent.AccountUpdated(checking));

        // Cartão permanece inalterado (mesma instância no repo)
        MonetaryAccount unchanged = accountRepo.findById(c1).orElseThrow();
        assertEquals("#112233", unchanged.color());
    }

    @Test
    @DisplayName("onAccountUpdated não afeta cartões de outras contas")
    void doesNotAffectCardsFromOtherAccounts() {
        MonetaryAccount checking1 = seedChecking("#112233");
        MonetaryAccount checking2 = seedChecking("#AABBCC");
        UUID c1 = UUID.randomUUID();
        accountRepo.save(new MonetaryAccount(c1, "C1", AccountType.CREDIT_CARD,
                new BigDecimal("100.00"), "#AABBCC", true, checking2.id()));

        // Atualiza checking1 com nova cor
        MonetaryAccount updated = new MonetaryAccount(
                checking1.id(), "Banco", AccountType.CHECKING,
                new BigDecimal("100.00"), "#FF0000", true, null);
        accountRepo.save(updated);

        listener.onAccountUpdated(new MonetaryEvent.AccountUpdated(updated));

        // Cartão de checking2 não é afetado
        MonetaryAccount unchanged = accountRepo.findById(c1).orElseThrow();
        assertEquals("#AABBCC", unchanged.color());
    }
}
