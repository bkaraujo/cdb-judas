package br.cdb.feature.f010._1_application;

import br.cdb.context.monetary._0_domain.model.Transaction;
import br.cdb.feature.f010._0_domain.UserProvisioningStep;
import br.cdb.feature.f007._0_domain.UserCategory;
import br.cdb.feature.f007._0_domain.UserCategoryRepository;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.*;

/** Semeia o catálogo default de categorias (macro + subcategorias) para um usuário recém-criado. */
@Singleton
@NullMarked
@RequiredArgsConstructor
public class CategorySeedStep implements UserProvisioningStep {

    private final UserCategoryRepository repository;

    @Override
    public void provision(UUID personId) {
        seed(personId, Transaction.Type.INCOME,
                Map.of(
                        "0. CLT", List.of("Salário", "Benefício", "13º Salário", "Férias", "Restituição", "PLR"),
                        "1. CNPJ", List.of("Pró labore"),
                        "2. Investimento", List.of("Dividendos", "Juros sobre capital", "Resgate"),
                        "9. Outros", List.of("Restituição", "Freelance", "Vendas", "IRPF", "FGTS")
                ));

        seed(personId, Transaction.Type.EXPENSE,
                Map.of(
                        "1. Moradia", List.of("Aluguél / Prestação", "Condomínio", "Impostos/Tarifas", "Conta de energia", "Conta de água", "Conta de gás", "Telefone fixo", "Internet", "Supermercado", "Feira", "Padaria", "Empregados", "Lavanderia", "Decoração", "Utensilios", "Restaurantes", "Assinaturas", "Manutenção", "Outros"),
                        "2. Transporte", List.of("Prestação", "Impostos/Tarifas", "Seguro", "Combustível", "Estacionamento", "Multas", "Transporte Público", "Aplicativo", "Aluguél", "Pedágio", "Manutenção", "Outros"),
                        "3. Educação", List.of("Escola / Faculdade", "Impostos/Tarifas", "Atividade Externa", "Cursos", "Material escolar", "Uniformes", "Outros"),
                        "4. Saúde", List.of("Plano de Saúde", "Médicos e terapeutas", "Dentista", "Medicamentos", "Utensilios", "Procedimentos", "Academia", "Outros"),
                        "5. Lazer", List.of("Cinema/Teatro/Concerto", "Cafés/Bares/Restaurantes", "Atrações Turísticas", "Livraria, jornais e revistas", "Games", "Midias e acessórios", "Passagens", "Hospedagens"),
                        "6. Despesas Pessoais", List.of("Higiene Pessoal", "Estética/Beleza", "Vestuário", "Mesadas", "Utensílios", "Presentes", "Assinaturas", "Art. Infantis", "Art. desportívos", "Telefonia", "Outros"),
                        "7. Investimento", List.of("Renda Fixa", "Renda Variável", "Crypto"),
                        "9. Outros", List.of("Tarifas Bancárias", "Impostos", "Pensões", "Doações e dízimos", "Empréstimos", "Eventos", "Outros")
                ));
    }

    @Transactional
    void seed(UUID personId, Transaction.Type nature, Map<String, List<String>> categories) {
        val existing = repository.findByNature(personId, nature);

        // 1. Mapeamento O(1) na memória para evitar loops aninhados (Stream/Filter)
        val rootsByName = new HashMap<String, UserCategory>();
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
                parentCategory = new UserCategory(parentId, personId, nature, parentName, null, false);
                repository.save(parentCategory);
            } else {
                parentId = parentCategory.id();
            }

            val children = childrenNamesByParentId.getOrDefault(parentId, Collections.emptySet());
            for (val name : names) {
                if (!children.contains(name)) {
                    repository.save(new UserCategory(
                            UUID.randomUUID(),
                            personId,             // COD_PERSON
                            nature,             // COD_NATURE
                            name,               // TXT_NAME
                            parentId,           // COD_PARENT
                            false               // FLG_SYSTEM (N)
                    ));
                }
            }
        }
    }
}
