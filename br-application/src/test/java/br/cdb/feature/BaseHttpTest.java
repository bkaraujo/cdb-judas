package br.cdb.feature;

import br.cdb.context.monetary._0_domain.repository.*;
import br.cdb.context.monetary._1_application.service.*;
import br.cdb.context.monetary._1_application.usecase.AccountUseCase;
import br.cdb.context.monetary._1_application.usecase.CostCenterUseCase;
import br.cdb.context.monetary._1_application.usecase.CreditCardUseCase;
import br.cdb.context.monetary._1_application.usecase.TransactionUseCase;
import br.cdb.core.JsonStorageProperties;
import br.cdb.core.web.security.User;
import br.cdb.core.web.security.UserRepository;
import br.cdb.core.web.security.core.AccessTokenStore;
import br.cdb.feature.system.auth.LoginResource;
import br.cdb.infra.persistence.Database;
import br.cdb.infra.persistence.features.UserAccountBalanceJDBCRepository;
import br.cdb.infra.persistence.monetary.AccountJDBCRepository;
import br.cdb.infra.persistence.monetary.CostCenterJDBCRepository;
import br.cdb.infra.persistence.monetary.CreditCardJDBCRepository;
import br.cdb.infra.persistence.monetary.TransactionJDBCRepository;
import br.commons.Registry;
import br.commons.framework.persistence.Storage;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.json.Repository;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

@QuarkusTest
public abstract class BaseHttpTest {

    protected static final String TEST_USER_ID = "00000000-0000-0000-0000-0000000000ad";
    protected static final String TEST_USERNAME = "test-user";

    @Inject
    protected Storage storage;

    @Inject
    protected JsonStorageProperties storageProperties;

    @Inject
    DataSource dataSource;

    @Inject
    Instance<Repository<?, ?>> repositories;

    @Inject
    UserRepository userRepository;

    @Inject
    AccessTokenStore tokenStore;

    @BeforeEach
    void setUpBase() throws IOException {
        // Clean storage directory
        val path = Path.of(storageProperties.path());
        deleteRecursively(path);
        Files.createDirectories(path);

        // Reset relational tables — H2 in-memory é compartilhado entre métodos de teste. Também
        // garante que o DataSource já está publicado no Registry (JDBCRepository.<init> o exige)
        // antes de repositories.forEach abaixo forçar a construção de todos os *JDBCRepository.
        val tx = dataSource.begin().get();
        for (val sql : Database.reset()) tx.execute(sql).get();
        tx.commit().get();

        // Clear repository caches
        repositories.forEach(Repository::clearCache);

        // Reancora o contexto monetário nos adaptadores JDBC: os testes de unidade puros
        // (br.cdb.context.monetary) trocam as portas do Registry por fakes in-memory e removem
        // services/use cases — sem este reset, uma requisição HTTP posterior reconstruiria o
        // grafo do contexto em cima dos fakes do último teste puro executado.
        resetMonetaryRegistry();

        userRepository.save(new User(TEST_USER_ID, TEST_USERNAME, null, BcryptUtil.bcryptHash("test")));
    }

    private static void resetMonetaryRegistry() {
        Registry.set(AccountRepository.class, new AccountJDBCRepository());
        Registry.set(BalanceRepository.class, new UserAccountBalanceJDBCRepository());
        Registry.set(CostCenterRepository.class, new CostCenterJDBCRepository());
        Registry.set(TransactionRepository.class, new TransactionJDBCRepository());
        Registry.set(CreditCardRepository.class, new CreditCardJDBCRepository());

        Registry.remove(AccountService.class);
        Registry.remove(BalanceService.class);
        Registry.remove(TransactionService.class);
        Registry.remove(CreditCardService.class);
        Registry.remove(CostCenterService.class);
        Registry.remove(AccountUseCase.class);
        Registry.remove(TransactionUseCase.class);
        Registry.remove(CostCenterUseCase.class);
        Registry.remove(CreditCardUseCase.class);
    }

    /**
     * Nova requisição autenticada como {@link #TEST_USER_ID}. Emite um token novo a cada chamada —
     * o token de acesso real rotaciona a cada resposta bem-sucedida, então reaproveitar um token
     * capturado de uma resposta anterior quebraria a segunda chamada em diante.
     */
    protected RequestSpecification asTestUser() {
        return RestAssured.given()
                .header(LoginResource.TOKEN_HEADER, tokenStore.issue(TEST_USER_ID))
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
