package br.cdb.feature.system.user;

import br.cdb.context.monetary._0_domain.model.Transaction;
import br.cdb.core.web.security.User;
import br.cdb.core.web.security.UserRepository;
import br.cdb.feature.user.categories.UserCategory;
import br.cdb.feature.user.categories.UserCategoryRepository;
import br.cdb.feature.user.profile.PreferencesRepository;
import br.cdb.feature.user.profile.Profile;
import br.commons.Logger;
import br.commons.Result;
import br.commons.business.BusinessError;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.*;

@Singleton
@NullMarked
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PreferencesRepository preferencesRepository;
    private final UserCategoryRepository repository;

    public void createUser(String username, String name, char[] password) {
        // Semeia o admin e captura o ID para atrelar às categorias
        val userId = UUID.fromString(userRepository.findByUsername(username)
                .map(User::id)
                .orElseGet(() -> {
                    val id = UUID.randomUUID().toString();
                    userRepository.save(new User(
                            id,
                            username,
                            name,
                            BcryptUtil.bcryptHash(new String(password))
                    ));
                    Logger.info("usuário '%s' criado com id %s", username, id);
                    return id;
                }));

        initializeCategories(userId);
    }

    /** Atualiza o nome de exibição. Aplica trim; nome em branco vira nulo (exibição cai para username). */
    public Result<Profile, BusinessError> updateName(String userId, @Nullable String name) {
        val user = userRepository.findById(userId).orElse(null);
        if (user == null) return Result.failure(new BusinessError.NotFound("Usuário não encontrado"));

        val trimmed = (name == null || name.isBlank()) ? null : name.trim();
        val saved = userRepository.save(new User(user.id(), user.username(), trimmed, user.password(), user.active(), user.createdAt(), user.updatedAt()));

        return Result.success(new Profile(saved, preferencesRepository.findByUserId(userId)));
    }

    private void initializeCategories(UUID userId) {
        create(userId, Transaction.Type.INCOME,
                Map.of(
                        "0. CLT", List.of("Salário", "Benefício", "13º Salário", "Férias", "Restituição", "PLR"),
                        "1. CNPJ", List.of("Pró labore"),
                        "2. Investimento", List.of("Dividendos", "Juros sobre capital", "Resgate"),
                        "9. Outros", List.of("Restituição", "Freelance", "Vendas", "IRPF", "FGTS")
                ));

        create(userId, Transaction.Type.EXPENSE,
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
    public void create(UUID userId, Transaction.Type nature, Map<String, List<String>> categories) {
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
