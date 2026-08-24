package br.cdb.feature.f005;

import br.cdb.core.persistence.Database;
import br.cdb.feature.f000._0_domain.event.CategoryDeleted;
import br.cdb.feature.f000._0_domain.event.UserEvents;
import br.cdb.feature.f005._0_domain.model.Category;
import br.cdb.feature.f005._0_domain.model.Nature;
import br.cdb.feature.f005._0_domain.repository.CategoryRepository;
import br.cdb.feature.f005._1_application.service.UserCategoryService;
import br.cdb.feature.f005._2_infrastructure.F005ApiImpl;
import br.cdb.feature.f005._2_infrastructure.persistence.CategoryJDBCRepository;
import br.commons.Logger;
import br.commons.MessageBus;
import br.commons.Result;
import br.commons.annotation.Lifecycle;
import br.commons.framework.cdi.Context;
import br.commons.framework.message.MessageListener;
import br.commons.framework.message.MessageResult;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.*;

/**
 * Módulo da fatia {@code f005} (categories). Semeia as categorias padrão reagindo a
 * {@link UserEvents.Created} — o listener é registrado no {@link MessageBus} aqui, antes de
 * {@code F999Module} criar o usuário {@code admin} (ordem da lista de módulos em
 * {@code FeatureBootstrap}).
 *
 * <p>{@code F006_TRANSACTION_CATEGORY} (DDL, leitura e escrita) é de {@code f006}
 * ({@code TransactionCategoryRepository}) — é vínculo da transação, não da categoria.</p>
 */
@NullMarked
public class F005Module implements Lifecycle {


    private static List<String> model() {
        return List.of(
                """
                CREATE TABLE F005_CATEGORY_NATURE (
                    ID VARCHAR(20) PRIMARY KEY,
                    TXT_DESCRIPTION VARCHAR(10) NOT NULL
                )
                """,
                "INSERT INTO F005_CATEGORY_NATURE (ID, TXT_DESCRIPTION) VALUES ('EXPENSE', 'Despesa')",
                "INSERT INTO F005_CATEGORY_NATURE (ID, TXT_DESCRIPTION) VALUES ('INCOME', 'Receita')",
                """
                CREATE TABLE F005_CATEGORY (
                    ID CHAR(36) PRIMARY KEY,
                    COD_PERSON CHAR(36) NOT NULL,
                    COD_PARENT CHAR(36),
                    COD_NATURE VARCHAR(20) NOT NULL REFERENCES F005_CATEGORY_NATURE(ID),
                    TXT_NAME VARCHAR(80) NOT NULL,
                    FLG_SYSTEM CHAR(1) NOT NULL,
                    FLG_ACTIVE CHAR(1) NOT NULL,
                    TMS_CREATE_AT TIMESTAMP NOT NULL,
                    TMS_UPDATED_AT TIMESTAMP NOT NULL
                )
                """
        );
    }

    @Override
    public Result<Void, Throwable> initialize() {
        Logger.debug("Iniciando módulo..");

        Database.initialize(model());

        Context.set(CategoryRepository.class, CategoryJDBCRepository::new);
        Context.set(F005Api.class, F005ApiImpl::new);

        MessageBus.subscribe(new Object(){
            @MessageListener
            public MessageResult on(UserEvents.Created event) {
                Logger.debug("Cadastrando categorias para usuário %s", event.id());
                val personId = UUID.fromString(event.personId());

                seed(personId, Nature.INCOME,
                        Map.of(
                                "0. CLT", List.of("Salário", "Benefício", "13º Salário", "Férias", "Restituição", "PLR"),
                                "1. CNPJ", List.of("Pró labore"),
                                "2. Investimento", List.of("Dividendos", "Juros sobre capital", "Resgate"),
                                "9. Outros", List.of("Restituição", "Freelance", "Vendas", "IRPF", "FGTS", "Transferência")
                        ));

                seed(personId, Nature.EXPENSE,
                        Map.of(
                                "1. Moradia", List.of("Aluguél, Prestação", "Condomínio", "Imposto, Tarifa", "Conta de energia", "Conta de água", "Conta de gás", "Telefone fixo", "Internet", "Supermercado", "Feira", "Padaria", "Empregados", "Lavanderia", "Decoração", "Utensilios", "Restaurantes", "Assinaturas", "Manutenção", "Outros"),
                                "2. Transporte", List.of("Prestação", "Imposto, Tarifa", "Seguro", "Combustível", "Estacionamento", "Multas", "Transporte Público", "Aplicativo", "Aluguél", "Pedágio", "Manutenção", "Outros"),
                                "3. Educação", List.of("Escola, Faculdade", "Imposto, Tarifa", "Atividade Externa", "Cursos", "Material escolar", "Uniformes", "Outros"),
                                "4. Saúde", List.of("Plano de Saúde", "Médico, Terapeuta", "Dentista", "Medicamento", "Utensílio", "Procedimento", "Academia", "Outros"),
                                "5. Lazer", List.of("Cinema, Teatro, Concerto", "Cafés, Bares, Restaurantes", "Utensílios", "Atrações Turísticas", "Livraria, jornais e revistas", "Games", "Midias e acessórios", "Passagens", "Hospedagens"),
                                "6. Despesas Pessoais", List.of("Higiene Pessoal", "Estética, Beleza", "Vestuário", "Mesadas", "Utensílios", "Presentes", "Assinaturas", "Art. Infantis", "Art. desportívos", "Telefonia", "Outros"),
                                "7. Investimento", List.of("Renda Fixa", "Renda Variável", "Crypto"),
                                "9. Outros", List.of("Tarifas Bancárias", "Impostos", "Pensões", "Doações, Dízimos", "Empréstimos", "Eventos", "Outros", "Transferência")
                        ));

                return MessageResult.CONSUMED;
            }

            void seed(UUID personId, Nature nature, Map<String, List<String>> categories) {
                Logger.trace("Cadastrando categorias %s para usuário %s", nature, personId);
                val repository = Context.get(CategoryRepository.class);
                val existing = repository.findByNature(personId, nature);

                // 1. Mapeamento O(1) na memória para evitar loops aninhados (Stream/Filter)
                val rootsByName = new HashMap<String, Category>();
                val childrenNamesByParentId = new HashMap<UUID, Set<String>>();

                for (val cat : existing) {
                    if (cat.parentId() == null) {
                        rootsByName.put(cat.name(), cat);
                    } else {
                        childrenNamesByParentId
                                .computeIfAbsent(cat.parentId(), k -> new HashSet<>())
                                .add(cat.name());
                    }
                }

                // 2. Iteração das categorias solicitadas
                for (val entry : categories.entrySet()) {
                    val parentName = entry.getKey();
                    val names = entry.getValue();

                    var parentCategory = rootsByName.get(parentName);
                    UUID parentId;

                    if (parentCategory == null) {
                        parentId = UUID.randomUUID();
                        parentCategory = new Category(parentId, personId, nature, parentName, null, false);
                        repository.save(parentCategory);
                    } else {
                        parentId = parentCategory.id();
                    }

                    val children = childrenNamesByParentId.getOrDefault(parentId, Collections.emptySet());
                    for (val name : names) {
                        if (!children.contains(name)) {
                            repository.save(new Category(
                                    UUID.randomUUID(),
                                    personId,                               // COD_PERSON
                                    nature,                                 // COD_NATURE
                                    name,                                   // TXT_NAME
                                    parentId,                               // COD_PARENT
                                    "Transferência".equalsIgnoreCase(name)  // FLG_SYSTEM: Transferência é categoria de sistema (não excluível)
                            ));
                        }
                    }
                }
            }
        });

                MessageBus.subscribe(new Object() {
                    @MessageListener
                    public MessageResult on(CategoryDeleted event) {
                Context.tryGet(UserCategoryService.class).deletePlain(event.categoryIds(), event.personId());
                return MessageResult.CONSUMED;
            }
        });

        return Result.success();
    }

}
