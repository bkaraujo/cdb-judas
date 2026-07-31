package br.cdb.context.monetary;

import br.cdb.feature.f002._0_domain.model.Account;
import br.cdb.feature.f002._0_domain.model.Balance;
import br.cdb.feature.f002._1_application.service.BalanceService;
import br.cdb.feature.f999._0_domain.DeletionQueueEntry;
import br.cdb.feature.f999._0_domain.DeletionQueueRepository;
import br.cdb.feature.f999._1_application.DeletionQueueService;
import br.commons.chrono.Time;
import br.commons.framework.cdi.Context;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** {@code runOnce()} é o método testado direto (sem depender do {@code @Scheduled}) — plan.md fase 5. */
class DeletionQueueServiceTest extends AbstractUseCaseTest {

    private static final UUID PERSON = UUID.randomUUID();

    private FakeQueue queue;
    private DeletionQueueService service;

    @BeforeEach
    void setUp() {
        queue = new FakeQueue();
        service = new DeletionQueueService(queue);
    }

    @Test
    void runOnce_republishesAndRemovesSuccessfulEntry() {
        service.enqueue(DeletionQueueService.TYPE_ACCOUNT_DELETED, UUID.randomUUID(), PERSON);
        assertEquals(1, queue.findAllUnlocked().size());

        service.runOnce();

        assertTrue(queue.findAllUnlocked().isEmpty());
    }

    @Test
    void runOnce_incrementsAttemptsOnFailureWithoutRemoving() {
        val entry = queue.save(bogusEntry());

        service.runOnce();

        val reloaded = queue.findById(entry.id()).orElseThrow();
        assertEquals(1, reloaded.attempts());
        assertFalse(reloaded.locked());
    }

    @Test
    void runOnce_locksAfterMaxAttemptsAndStopsShowingUpAsUnlocked() {
        val entry = queue.save(bogusEntry());

        for (int i = 0; i < 5; i++) {
            service.runOnce();
        }

        val reloaded = queue.findById(entry.id()).orElseThrow();
        assertEquals(5, reloaded.attempts());
        assertTrue(reloaded.locked());
        assertTrue(queue.findAllUnlocked().isEmpty(), "travado não deve mais aparecer entre os não travados");
    }

    @Test
    void runOnce_recomputesDirtyBalanceSnapshots() {
        val account = accountRepository().save(new Account(UUID.randomUUID(), "Conta", Account.Type.CHECKING, true));
        val balanceService = Context.tryGet(BalanceService.class);
        balanceService.save(new Balance(account, YearMonth.now(), new BigDecimal("100.00")));
        balanceService.markDirty(account.id());
        assertTrue(balanceRepository().findDirtyAccountIds().contains(account.id()));

        service.runOnce();

        assertFalse(balanceRepository().findDirtyAccountIds().contains(account.id()));
    }

    /** Tipo desconhecido → {@code republish} lança determinístico, sem depender de um listener real falhar. */
    private static DeletionQueueEntry bogusEntry() {
        val now = Time.now();
        return new DeletionQueueEntry(UUID.randomUUID(), "BOGUS_TYPE", UUID.randomUUID(), PERSON, 0, false, now, now);
    }

    private static final class FakeQueue implements DeletionQueueRepository {
        private final Map<UUID, DeletionQueueEntry> data = new LinkedHashMap<>();

        public List<DeletionQueueEntry> findAll() { return List.copyOf(data.values()); }
        public Optional<DeletionQueueEntry> findById(UUID id) { return Optional.ofNullable(data.get(id)); }
        public DeletionQueueEntry save(DeletionQueueEntry e) { data.put(e.id(), e); return e; }
        public void deleteById(UUID id) { data.remove(id); }
        public void clearCache() { data.clear(); }

        public List<DeletionQueueEntry> findAllUnlocked() {
            return data.values().stream().filter(e -> !e.locked()).toList();
        }
    }
}
