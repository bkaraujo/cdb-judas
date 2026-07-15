package br.cdb.feature.user.categories;

import br.cdb.context.monetary._0_domain.model.Transaction;
import br.cdb.feature.system.user.UserProvisioningStep;
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
    public void provision(UUID userId) {
        seed(userId, Transaction.Type.INCOME,
                Map.of(
                        "0. CLT", List.of("Salário", "Benefício", "13º Salário", "Férias", "Restituição", "PLR"),
                        "1. CNPJ", List.of("Pró labore"),
                        "2. Investimento", List.of("Dividendos", "Juros sobre capital", "Resgate"),
                        "9. Outros", List.of("Restituição", "Freelance", "Vendas", "IRPF", "FGTS")
                ));

        seed(userId, Transaction.Type.EXPENSE,
                Map.of(
                        "0. Habitação", List.of("Aluguel / Prestação", "Condomínio", "IPTU", "Conta de energia", "Conta de água", "Conta de gás", "Telefone fixo", "Internet", "Supermercado", "Feira", "Padaria", "Empregados", "Lavanderia", "Decoração", "Utensilios", "Restaurantes", "Assinaturas", "Outros", "Manutenção"),
                        "1. Saúde", List.of("Plano de Saúde", "Médicos e terapeutas", "Dentista", "Medicamentos", "Utensilios", "Procedimentos", "Academia", "Outros"),
                        "2. Transporte", List.of("Prestação", "IPVA", "Seguro", "Combustível", "Estacionamento", "Manutenção", "Multas", "Público", "Aplicativo", "Aluguél", "Outros"),
                        "3. Despesas Pessoais", List.of("Higiene Pessoal", "Cosméticos", "Estética", "Vestuário", "Esportes", "Cartões de Crédito", "Mesadas", "Utensilios", "Restaurantes", "Presentes", "Assinaturas", "Brinquedos", "Outros", "Telefones celulares"),
                        "4. Educação", List.of("Escola / Faculdade", "Passeios", "Atividades", "Cursos", "Material escolar", "Uniformes", "Outros"),
                        "5. Lazer", List.of("Outros", "Passeio", "Restaurantes", "Cafés, bares e boates", "Livraria, jornais e revistas", "Games", "Midias e acessórios", "Passagens", "Hospedagens"),
                        "9. Outros", List.of("Tarifas Bancárias", "Carnê Leão", "Pensões", "Gorjetas / caixinhas", "Doações e dízimos", "Emprestimos", "Eventos", "Retiros", "Extras diários", "Outros")
                ));
    }

    @Transactional
    void seed(UUID userId, Transaction.Type nature, Map<String, List<String>> categories) {
        val existing = repository.findByNature(userId, nature);

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
                parentCategory = new UserCategory(parentId, userId, nature, parentName, null, false);
                repository.save(parentCategory);
            } else {
                parentId = parentCategory.id();
            }

            val children = childrenNamesByParentId.getOrDefault(parentId, Collections.emptySet());
            for (val name : names) {
                if (!children.contains(name)) {
                    repository.save(new UserCategory(
                            UUID.randomUUID(),
                            userId,             // COD_USER
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
