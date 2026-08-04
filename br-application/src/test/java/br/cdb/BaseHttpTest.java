package br.cdb;

import br.cdb.core.JsonStorageProperties;
import br.cdb.core.persistence.Database;
import br.cdb.core.persistence.repository.UserRepository;
import br.cdb.core.security.AccessTokenStore;
import br.cdb.core.web.HTTPRequest;
import br.cdb.feature.f000._0_domain.repository.CostCenterRepository;
import br.cdb.feature.f000._0_domain.repository.PersonRepository;
import br.cdb.feature.f000._1_application.service.CostCenterService;
import br.cdb.feature.f000._1_application.service.PersonService;
import br.cdb.feature.f000._2_infrastructure.persistence.CachingPersonRepository;
import br.cdb.feature.f000._2_infrastructure.persistence.CostCenterJDBCRepository;
import br.cdb.feature.f000._2_infrastructure.persistence.PersonJDBCRepository;
import br.cdb.feature.f001._0_domain.PreferencesRepository;
import br.cdb.feature.f001._1_application.service.ProfileService;
import br.cdb.feature.f001._2_infrastructure.persistence.PreferencesJDBCRepository;
import br.cdb.feature.f002._0_domain.repository.AccountRepository;
import br.cdb.feature.f002._0_domain.repository.BalanceRepository;
import br.cdb.feature.f002._1_application.service.AccountService;
import br.cdb.feature.f002._1_application.service.BalanceService;
import br.cdb.feature.f002._1_application.usecase.ReadUseCase;
import br.cdb.feature.f002._1_application.usecase.WriteUseCase;
import br.cdb.feature.f002._2_infrastructure.persistence.AccountJDBCRepository;
import br.cdb.feature.f002._2_infrastructure.persistence.UserAccountBalanceJDBCRepository;
import br.cdb.feature.f003._0_domain.repository.CreditCardRepository;
import br.cdb.feature.f003._1_application.service.CreditCardService;
import br.cdb.feature.f003._2_infrastructure.persistence.CreditCardJDBCRepository;
import br.cdb.feature.f004._0_domain.repository.TagRepository;
import br.cdb.feature.f004._0_domain.repository.TransactionTagRepository;
import br.cdb.feature.f004._1_application.service.TagService;
import br.cdb.feature.f004._1_application.service.TransactionTagService;
import br.cdb.feature.f004._2_infrastructure.persistence.TagJDBCRepository;
import br.cdb.feature.f004._2_infrastructure.persistence.TransactionTagJDBCRepository;
import br.cdb.feature.f005._0_domain.CategoryRepository;
import br.cdb.feature.f005._1_application.service.UserCategoryService;
import br.cdb.feature.f005._2_infrastructure.persistence.CategoryJDBCRepository;
import br.cdb.feature.f006._0_domain.repository.TransactionRepository;
import br.cdb.feature.f006._1_application.service.TransactionService;
import br.cdb.feature.f006._1_application.usecase.ReadUseCases;
import br.cdb.feature.f006._1_application.usecase.WriteUseCases;
import br.cdb.feature.f006._2_infrastructure.persistence.TransactionJDBCRepository;
import br.commons.Result;
import br.commons.chrono.Time;
import br.commons.framework.cdi.Context;
import br.commons.framework.persistence.Storage;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.Comparator;
import java.util.UUID;

@QuarkusTest
public abstract class BaseHttpTest {

    protected static final String TEST_USER_ID = "00000000-0000-0000-0000-0000000000ad";
    protected static final String TEST_USERNAME = "test-user";

    @Inject
    protected Storage storage;

    @Inject
    protected JsonStorageProperties storageProperties;

    // Context-wired (não são mais beans CDI): publicados no startup por CoreModule/FNNNModule.
    protected final DataSource dataSource = Context.get(DataSource.class);

    protected final UserRepository userRepository = Context.get(UserRepository.class);

    @Inject
    protected AccessTokenStore tokenStore;

    @BeforeEach
    void setUpBase() throws IOException {
        // Clean storage directory
        val path = Path.of(storageProperties.path());
        deleteRecursively(path);
        Files.createDirectories(path);

        // Reset relational tables — H2 in-memory é compartilhado entre métodos de teste.
        val tx = dataSource.begin().get();
        for (val sql : Database.reset()) tx.execute(sql).get();
        tx.commit().get();
        // begin/close balanceados: sem o close o slot da thread ficava preso a uma transação já
        // commitada, e o begin() seguinte (nova transação ou introspecção de repositório) reentrava
        // numa conexão já devolvida ao pool.
        tx.close();

        // Clear repository caches — só os adaptadores com cache próprio sobrevivem ao reset abaixo.
        Context.get(PersonRepository.class).clearCache();

        // Reancora o contexto monetário nos adaptadores JDBC: os testes de unidade puros
        // (br.cdb.context.monetary) trocam as portas do Context por fakes in-memory e removem
        // services/use cases — sem este reset, uma requisição HTTP posterior reconstruiria o
        // grafo do contexto em cima dos fakes do último teste puro executado.
        resetMonetaryRegistry();

        seedUser(TEST_USER_ID, TEST_USERNAME, "test");
    }

    /**
     * Semeia login + pessoa de forma determinística e idempotente: o personId da PESSOA é igual ao personId do
     * usuário, então a mesma constante ({@link #TEST_USER_ID}) serve simultaneamente de identidade
     * de login (token) e de dono das rotas/overlays ({@code /api/{personId}/…}). Insere direto (em
     * vez de {@code userRepository.save}, que cunha um personId aleatório). {@code F000_USER}/
     * {@code F000_PERSON} sobrevivem ao {@link Database#reset()} — daí o guarda de existência.
     */
    protected void seedUser(String id, String username, String rawPassword) {
        dataSource.transaction(tx -> {
            val exists = tx.query(
                    "SELECT 1 FROM F000_USER WHERE ID = ?",
                    JDBCParameter.of(id),
                    rs -> rs.next().get()
            ).get();
            if (!exists) {
                val now = Timestamp.valueOf(Time.now());
                tx.execute(
                        "INSERT INTO F000_PERSON (ID, TXT_NAME, TXT_LOCALE, TXT_LANGUAGE, TMS_CREATE_AT, TMS_UPDATED_AT) VALUES (?, ?, ?, ?, ?, ?)",
                        JDBCParameter.of(id, username, "pt-BR", "pt-BR", now, now)
                ).get();
                tx.execute(
                        "INSERT INTO F000_USER (ID, COD_PERSON, TXT_USERNAME, FLG_ACTIVE, TMS_CREATE_AT, TMS_UPDATED_AT) VALUES (?, ?, ?, ?, ?, ?)",
                        JDBCParameter.of(id, id, username, "Y", now, now)
                ).get();
                tx.execute(
                        "INSERT INTO F000_USER_CREDENTIAL (ID, COD_USER, TXT_PASSWORD, TMS_CREATE_AT) VALUES (?, ?, ?, ?)",
                        JDBCParameter.of(UUID.randomUUID().toString(), id, BcryptUtil.bcryptHash(rawPassword), now)
                ).get();
            }
            return Result.success(true);
        });
    }

    private static void resetMonetaryRegistry() {
        Context.set(AccountRepository.class, AccountJDBCRepository::new);
        Context.set(BalanceRepository.class, UserAccountBalanceJDBCRepository::new);
        Context.set(CostCenterRepository.class, CostCenterJDBCRepository::new);
        Context.set(TransactionRepository.class, TransactionJDBCRepository::new);
        Context.set(CreditCardRepository.class, CreditCardJDBCRepository::new);
        Context.set(TagRepository.class, TagJDBCRepository::new);
        Context.set(TransactionTagRepository.class, TransactionTagJDBCRepository::new);
        Context.set(CategoryRepository.class, CategoryJDBCRepository::new);
        // Pessoa/preferências: os testes de unidade de f001 publicam fakes no mesmo Context global.
        // O repositório de pessoa é reancorado só quando o que está publicado não é o real — trocar
        // a instância a cada teste descartaria o cache write-ahead que o próprio request acabou de
        // popular (ver CachingPersonRepository).
        if (!(Context.get(PersonRepository.class) instanceof CachingPersonRepository)) {
            Context.set(PersonRepository.class, () -> new CachingPersonRepository(new PersonJDBCRepository()));
        }
        Context.set(PreferencesRepository.class, PreferencesJDBCRepository::new);

        Context.remove(AccountService.class);
        Context.remove(BalanceService.class);
        Context.remove(TransactionService.class);
        Context.remove(CreditCardService.class);
        Context.remove(TagService.class);
        Context.remove(TransactionTagService.class);
        Context.remove(UserCategoryService.class);
        // Alcançados por Context.get() estrito (não se auto-instanciam): re-registra em vez de remover.
        Context.set(CostCenterService.class, CostCenterService::new);
        Context.set(PersonService.class, () -> new PersonService(Context.get(PersonRepository.class)));
        Context.remove(WriteUseCase.class);
        Context.remove(ReadUseCase.class);
        Context.remove(WriteUseCases.class);
        Context.remove(ReadUseCases.class);
        Context.remove(br.cdb.feature.f000._1_application.usecase.ReadUseCase.class);
        Context.remove(br.cdb.feature.f000._1_application.usecase.WriteUseCase.class);
        // FQN: nome simples colide com o par de f002.
        Context.remove(br.cdb.feature.f003._1_application.usecase.ReadUseCase.class);
        Context.remove(br.cdb.feature.f003._1_application.usecase.WriteUseCase.class);
        Context.remove(br.cdb.feature.f004._1_application.usecase.ReadUseCase.class);
        Context.remove(br.cdb.feature.f004._1_application.usecase.WriteUseCase.class);
        Context.remove(br.cdb.feature.f005._1_application.usecase.ReadUseCase.class);
        Context.remove(br.cdb.feature.f005._1_application.usecase.WriteUseCase.class);
        Context.remove(ProfileService.class);
        Context.remove(br.cdb.feature.f001._1_application.usecase.ReadUseCase.class);
        Context.remove(br.cdb.feature.f001._1_application.usecase.WriteUseCase.class);
    }

    /** Context-wired (deixou de ser bean CDI): resolvido a cada uso, depois do reset do registry. */
    protected UserCategoryService userCategoryService() {
        return Context.tryGet(UserCategoryService.class);
    }

    /**
     * Nova requisição autenticada como {@link #TEST_USER_ID}. Emite um token novo a cada chamada —
     * o token de acesso real rotaciona a cada resposta bem-sucedida, então reaproveitar um token
     * capturado de uma resposta anterior quebraria a segunda chamada em diante.
     */
    protected RequestSpecification asTestUser() {
        val session = tokenStore.open(TEST_USER_ID, TEST_USER_ID, TEST_USERNAME);
        return RestAssured.given()
                .header(HTTPRequest.TOKEN_HEADER, session.token())
                .contentType(ContentType.JSON);
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) return;
        try (val walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException ignored) {
                    // best-effort cleanup entre testes
                }
            });
        }
    }
}
