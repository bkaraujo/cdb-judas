package br.community.core.web.security;

import br.commons.Logger;
import br.commons.framework.persistence.Storage;
import br.community.context.monetary._0_domain.model.Transaction;
import br.community.feature.user.categories.UserCategory;
import br.community.feature.user.categories.UserCategoryRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Semeia o usuário {@code admin}, a fonte global de centros de custo e as categorias padrões
 * no startup. A ordem em relação ao schema é garantida pelo observer {@code @Priority(1)}
 * de {@code ContextBridge} (DataSource antes).
 */
@Singleton
@NullMarked
@RequiredArgsConstructor
public final class UserSeeder {

    private static final String COST_CENTERS_FILE = "cost-centers.json";
    private static final String COST_CENTERS_KEY = "costCenters";
    private static final String COST_CENTERS_JSON = """
            [ {
              "id" : "d0000000-0000-0000-0000-000000000001",
              "description" : "Fixo"
            }, {
              "id" : "d0000000-0000-0000-0000-000000000002",
              "description" : "Variável"
            } ]""";

    private record CategorySeed(String id, @Nullable String parentId, String nature, String name) {}

    private static final CategorySeed[] CATEGORIES = {
            new CategorySeed("f4e2b903-8fa9-472d-8345-a03bae1daa9b", null, "INCOME", "0. CLT"),
            new CategorySeed("73227882-61c1-4483-84db-b06d6c6cd2b3", "f4e2b903-8fa9-472d-8345-a03bae1daa9b", "INCOME", "Salário"),
            new CategorySeed("befc2e24-9290-454a-b14c-0b0902bd3596", "f4e2b903-8fa9-472d-8345-a03bae1daa9b", "INCOME", "Benefício"),
            new CategorySeed("a29c535c-fc69-4bcd-bb33-00f0b8e59fa5", "f4e2b903-8fa9-472d-8345-a03bae1daa9b", "INCOME", "13º Salário"),
            new CategorySeed("0f9c6902-ffdf-4dee-9c2a-75407b8ba857", "f4e2b903-8fa9-472d-8345-a03bae1daa9b", "INCOME", "Férias"),
            new CategorySeed("f4643605-a62b-4183-ade0-58d255d9bcd4", "f4e2b903-8fa9-472d-8345-a03bae1daa9b", "INCOME", "Restituição"),
            new CategorySeed("c16595a4-3e78-4643-af69-52c1a4a4830d", "f4e2b903-8fa9-472d-8345-a03bae1daa9b", "INCOME", "PLR"),

            new CategorySeed("4bd06b6c-7490-487c-be78-96efac13b46c", null, "INCOME", "1. CNPJ"),
            new CategorySeed("caf70b88-705d-4328-a0d2-42a5a87f0225", "4bd06b6c-7490-487c-be78-96efac13b46c", "INCOME", "Pró labore"),

            new CategorySeed("476d7d27-39c4-4927-9305-e565f2fcecea", null, "INCOME", "2. Investimento"),
            new CategorySeed("71d1f78d-ed3b-4fbb-96f7-b2811614e039", "476d7d27-39c4-4927-9305-e565f2fcecea", "INCOME", "Dividendos"),
            new CategorySeed("e6370022-df8b-4c9c-a691-cb8aba9d33e9", "476d7d27-39c4-4927-9305-e565f2fcecea", "INCOME", "Juros sobre capital"),

            new CategorySeed("401a27c1-ccf9-4ef7-bdf6-dfa73f516a4e", null, "INCOME", "9. Outros"),
            new CategorySeed("bcb78018-5c5c-4d55-b3f5-4589e6f73b6b", "401a27c1-ccf9-4ef7-bdf6-dfa73f516a4e", "INCOME", "Restituição"),
            new CategorySeed("102c1dda-0f71-40ef-9f87-783cc12a2006", "401a27c1-ccf9-4ef7-bdf6-dfa73f516a4e", "INCOME", "Freelance"),
            new CategorySeed("ee15f3ba-c11c-40d8-bbcd-29572fffcc1a", "401a27c1-ccf9-4ef7-bdf6-dfa73f516a4e", "INCOME", "Vendas"),
            new CategorySeed("6f7faad6-0222-4fc0-aab1-b5cc02164328", "401a27c1-ccf9-4ef7-bdf6-dfa73f516a4e", "INCOME", "IRPF"),
            new CategorySeed("8c6d39f2-aaf0-4229-9bcd-c3dc2d13f2f4", "401a27c1-ccf9-4ef7-bdf6-dfa73f516a4e", "INCOME", "FGTS"),

            new CategorySeed("978a8e65-d785-4262-89b0-7cbb4c5cf535", null, "EXPENSE", "0. Habitação"),
            new CategorySeed("f27b40b3-a93d-4f6d-a826-dadf20bdd9fd", "978a8e65-d785-4262-89b0-7cbb4c5cf535", "EXPENSE", "Aluguel / Prestação"),
            new CategorySeed("20b40796-01c1-4e12-afa2-ee0c274772db", "978a8e65-d785-4262-89b0-7cbb4c5cf535", "EXPENSE", "Condomínio"),
            new CategorySeed("9d251e2c-6712-4ce2-ae67-39b3b73b01b3", "978a8e65-d785-4262-89b0-7cbb4c5cf535", "EXPENSE", "IPTU"),
            new CategorySeed("7c28a8c9-5336-4af3-a129-07dee70e91bb", "978a8e65-d785-4262-89b0-7cbb4c5cf535", "EXPENSE", "Conta de energia"),
            new CategorySeed("c54061bf-fb74-4ca8-94fa-372344ff2de8", "978a8e65-d785-4262-89b0-7cbb4c5cf535", "EXPENSE", "Conta de água"),
            new CategorySeed("ab577058-4f31-4f55-9736-c34c10f39612", "978a8e65-d785-4262-89b0-7cbb4c5cf535", "EXPENSE", "Conta de gás"),
            new CategorySeed("f5bca2ea-a896-44c1-9db5-3525e6b4693f", "978a8e65-d785-4262-89b0-7cbb4c5cf535", "EXPENSE", "Telefone fixo"),
            new CategorySeed("9c4cb8b3-1b94-402f-8233-f7292c9a89b8", "978a8e65-d785-4262-89b0-7cbb4c5cf535", "EXPENSE", "Internet"),
            new CategorySeed("1563a983-b4af-4abd-b863-2cdcff25ec19", "978a8e65-d785-4262-89b0-7cbb4c5cf535", "EXPENSE", "Supermercado"),
            new CategorySeed("0c990db5-141b-408c-b3f8-aaccd77357b7", "978a8e65-d785-4262-89b0-7cbb4c5cf535", "EXPENSE", "Feira"),
            new CategorySeed("4d23bb2b-e3f4-44bf-b734-82024f28d40b", "978a8e65-d785-4262-89b0-7cbb4c5cf535", "EXPENSE", "Padaria"),
            new CategorySeed("d6971af7-d618-4c07-bfae-2da7b3e7b7a5", "978a8e65-d785-4262-89b0-7cbb4c5cf535", "EXPENSE", "Empregados"),
            new CategorySeed("22738a2b-8802-44ee-86c9-259760cab3b6", "978a8e65-d785-4262-89b0-7cbb4c5cf535", "EXPENSE", "Lavanderia"),
            new CategorySeed("c95a2f4a-91b0-4d1c-8c78-fdc59064bc5f", "978a8e65-d785-4262-89b0-7cbb4c5cf535", "EXPENSE", "Decoração"),
            new CategorySeed("adf42708-41aa-4687-85fc-1452556e3722", "978a8e65-d785-4262-89b0-7cbb4c5cf535", "EXPENSE", "Utensilios"),
            new CategorySeed("5bfc32f4-e5f4-479f-a09a-0a936d8ee98f", "978a8e65-d785-4262-89b0-7cbb4c5cf535", "EXPENSE", "Restaurantes"),
            new CategorySeed("207572e7-4964-4366-ab9b-3770c66e7c47", "978a8e65-d785-4262-89b0-7cbb4c5cf535", "EXPENSE", "Assinaturas"),
            new CategorySeed("74796c14-a970-4cf9-8faa-e6719d472f24", "978a8e65-d785-4262-89b0-7cbb4c5cf535", "EXPENSE", "Outros"),
            new CategorySeed("344186ea-72f3-4e25-9b0e-227397b24f89", "978a8e65-d785-4262-89b0-7cbb4c5cf535", "EXPENSE", "Manutenção"),

            new CategorySeed("1cf8d15b-c99d-4ca7-bcb6-061cb7ed611f", null, "EXPENSE", "1. Saúde"),
            new CategorySeed("5d2fe778-26dc-4994-a78b-ed720900f42f", "1cf8d15b-c99d-4ca7-bcb6-061cb7ed611f", "EXPENSE", "Plano de Saúde"),
            new CategorySeed("c7500768-8eb3-4683-9207-56f7ffc21409", "1cf8d15b-c99d-4ca7-bcb6-061cb7ed611f", "EXPENSE", "Médicos e terapeutas"),
            new CategorySeed("38a174c2-b8bb-4841-9e13-f2c2d738cb0d", "1cf8d15b-c99d-4ca7-bcb6-061cb7ed611f", "EXPENSE", "Dentista"),
            new CategorySeed("c7653b05-6266-4a9a-866f-60f1d2990ae3", "1cf8d15b-c99d-4ca7-bcb6-061cb7ed611f", "EXPENSE", "Medicamentos"),
            new CategorySeed("8c84b0be-3f6a-4cca-8f08-b7d13e4bb124", "1cf8d15b-c99d-4ca7-bcb6-061cb7ed611f", "EXPENSE", "Utensilios"),
            new CategorySeed("3687493f-1379-4fbd-8d6a-2a40cd09257a", "1cf8d15b-c99d-4ca7-bcb6-061cb7ed611f", "EXPENSE", "Procedimentos"),
            new CategorySeed("03c37ec0-6813-4bc0-b57d-70c9fd4fae99", "1cf8d15b-c99d-4ca7-bcb6-061cb7ed611f", "EXPENSE", "Academia"),
            new CategorySeed("2a192f1e-da68-49b9-9a4e-a1a5b222f8da", "1cf8d15b-c99d-4ca7-bcb6-061cb7ed611f", "EXPENSE", "Outros"),

            new CategorySeed("5e9469c0-48a8-46c9-b878-e791817c3238", null, "EXPENSE", "2. Transporte"),
            new CategorySeed("92739336-51ed-438d-9eac-af63913186c1", "5e9469c0-48a8-46c9-b878-e791817c3238", "EXPENSE", "Prestação"),
            new CategorySeed("6e92913e-48f1-4238-b231-f077cb9aa321", "5e9469c0-48a8-46c9-b878-e791817c3238", "EXPENSE", "IPVA"),
            new CategorySeed("a087db1d-a1c0-434f-acbb-90ec2c504bb2", "5e9469c0-48a8-46c9-b878-e791817c3238", "EXPENSE", "Seguro"),
            new CategorySeed("112eb44d-6179-4b76-b39e-ee63ceb6d2d6", "5e9469c0-48a8-46c9-b878-e791817c3238", "EXPENSE", "Combustível"),
            new CategorySeed("104f4162-8d07-4639-b52e-79d9a11a4cdb", "5e9469c0-48a8-46c9-b878-e791817c3238", "EXPENSE", "Estacionamento"),
            new CategorySeed("9c9bcdf1-3088-4d41-81ea-7b11960761d2", "5e9469c0-48a8-46c9-b878-e791817c3238", "EXPENSE", "Manutenção"),
            new CategorySeed("a05e532a-d7a4-4454-a798-25a247f7a7af", "5e9469c0-48a8-46c9-b878-e791817c3238", "EXPENSE", "Multas"),
            new CategorySeed("8fbecf96-e880-4852-85ae-e0e3e7c914b2", "5e9469c0-48a8-46c9-b878-e791817c3238", "EXPENSE", "Público"),
            new CategorySeed("ab2df2e6-f615-4912-9c7c-28068d65079e", "5e9469c0-48a8-46c9-b878-e791817c3238", "EXPENSE", "Aplicativo"),
            new CategorySeed("14d4c857-b6d6-4940-9125-e4430baf9ecc", "5e9469c0-48a8-46c9-b878-e791817c3238", "EXPENSE", "Aluguél"),
            new CategorySeed("82542a07-de10-4cd4-85f7-415cd6e49265", "5e9469c0-48a8-46c9-b878-e791817c3238", "EXPENSE", "Outros"),

            new CategorySeed("e6153f9e-5968-4185-bd03-584dc1690a08", null, "EXPENSE", "3. Despesas Pessoais"),
            new CategorySeed("cb7a302e-26b8-4154-8aca-dce987831e7f", "e6153f9e-5968-4185-bd03-584dc1690a08", "EXPENSE", "Higiene Pessoal"),
            new CategorySeed("c5ae9af8-8016-4a0d-b4db-b797af698c47", "e6153f9e-5968-4185-bd03-584dc1690a08", "EXPENSE", "Cosméticos"),
            new CategorySeed("dd1659e1-5414-4a30-8421-ce54b6d06a98", "e6153f9e-5968-4185-bd03-584dc1690a08", "EXPENSE", "Estética"),
            new CategorySeed("3e94e4fc-b2f6-42ba-a9ff-01c8a4ae3fc7", "e6153f9e-5968-4185-bd03-584dc1690a08", "EXPENSE", "Vestuário"),
            new CategorySeed("f5d329e4-a7a9-4f35-9412-c659751860a6", "e6153f9e-5968-4185-bd03-584dc1690a08", "EXPENSE", "Esportes"),
            new CategorySeed("ba383003-f017-4a13-a9c2-04d47389ad12", "e6153f9e-5968-4185-bd03-584dc1690a08", "EXPENSE", "Cartões de Crédito"),
            new CategorySeed("353fd063-8d87-46d2-b868-58ca7f49648e", "e6153f9e-5968-4185-bd03-584dc1690a08", "EXPENSE", "Mesadas"),
            new CategorySeed("61e1d9b9-4a80-4ed6-a425-00f474e0ad77", "e6153f9e-5968-4185-bd03-584dc1690a08", "EXPENSE", "Utensilios"),
            new CategorySeed("0ac0585c-444d-42b6-b2f7-184133381057", "e6153f9e-5968-4185-bd03-584dc1690a08", "EXPENSE", "Restaurantes"),
            new CategorySeed("65dc47a1-bf1d-416f-a252-0d884d8004a8", "e6153f9e-5968-4185-bd03-584dc1690a08", "EXPENSE", "Presentes"),
            new CategorySeed("5861bbca-d93f-4437-988a-56d43e72fd03", "e6153f9e-5968-4185-bd03-584dc1690a08", "EXPENSE", "Assinaturas"),
            new CategorySeed("9373d206-1fe3-4d75-b0c8-064856f967c3", "e6153f9e-5968-4185-bd03-584dc1690a08", "EXPENSE", "Brinquedos"),
            new CategorySeed("eb39d033-5f3e-4179-9483-efa2e0147be0", "e6153f9e-5968-4185-bd03-584dc1690a08", "EXPENSE", "Outros"),
            new CategorySeed("bddb5c18-4c42-4d9b-89a1-7db14e4cc060", "e6153f9e-5968-4185-bd03-584dc1690a08", "EXPENSE", "Telefones celulares"),

            new CategorySeed("041153c6-d466-4776-acf3-f4d26c736195", null, "EXPENSE", "4. Educação"),
            new CategorySeed("bff3f7d2-5ce7-4758-bf2f-978c171c2e90", "041153c6-d466-4776-acf3-f4d26c736195", "EXPENSE", "Escola / Faculdade"),
            new CategorySeed("0232517c-93f2-405d-afab-21077bc933f8", "041153c6-d466-4776-acf3-f4d26c736195", "EXPENSE", "Passeios"),
            new CategorySeed("5a2e3894-4dee-48a3-b365-8ace91f7a3bf", "041153c6-d466-4776-acf3-f4d26c736195", "EXPENSE", "Atividades"),
            new CategorySeed("010bf768-cde1-4beb-ae32-ce169cc8ae1c", "041153c6-d466-4776-acf3-f4d26c736195", "EXPENSE", "Cursos"),
            new CategorySeed("c86668aa-1503-456e-879c-c310648a3920", "041153c6-d466-4776-acf3-f4d26c736195", "EXPENSE", "Material escolar"),
            new CategorySeed("4092f83b-ced4-44c8-9390-ab7ac2cc5d24", "041153c6-d466-4776-acf3-f4d26c736195", "EXPENSE", "Uniformes"),
            new CategorySeed("5af439ec-6c47-4902-bbd3-cf842fa21721", "041153c6-d466-4776-acf3-f4d26c736195", "EXPENSE", "Outros"),

            new CategorySeed("5f66447b-b1e5-4529-9fc2-16cf8ca8332f", null, "EXPENSE", "5. Lazer"),
            new CategorySeed("3ddf9729-5327-4564-a0c7-9bd96d638526", "5f66447b-b1e5-4529-9fc2-16cf8ca8332f", "EXPENSE", "Outros"),
            new CategorySeed("72de6064-4273-4349-aea2-709f6ec1e16d", "5f66447b-b1e5-4529-9fc2-16cf8ca8332f", "EXPENSE", "Passeio"),
            new CategorySeed("4fa42c30-355a-4e41-8002-148808b05b67", "5f66447b-b1e5-4529-9fc2-16cf8ca8332f", "EXPENSE", "Restaurantes"),
            new CategorySeed("315ba056-b6dc-4ed1-b8f9-fafa53d2168a", "5f66447b-b1e5-4529-9fc2-16cf8ca8332f", "EXPENSE", "Cafés, bares e boates"),
            new CategorySeed("f921401d-8c5f-4928-afa0-f45e6739a0de", "5f66447b-b1e5-4529-9fc2-16cf8ca8332f", "EXPENSE", "Livraria, jornais e revistas"),
            new CategorySeed("f8ba3d2d-cf6e-4e78-a39d-50ffa7825a23", "5f66447b-b1e5-4529-9fc2-16cf8ca8332f", "EXPENSE", "Games"),
            new CategorySeed("8abc3719-6cdd-43aa-97e3-893076da7427", "5f66447b-b1e5-4529-9fc2-16cf8ca8332f", "EXPENSE", "Midias e acessórios"),
            new CategorySeed("88c052eb-c846-4626-8f49-f82105a15e26", "5f66447b-b1e5-4529-9fc2-16cf8ca8332f", "EXPENSE", "Passagens"),
            new CategorySeed("599628ca-2aad-4003-950d-4a43ac137023", "5f66447b-b1e5-4529-9fc2-16cf8ca8332f", "EXPENSE", "Hospedagens"),

            new CategorySeed("a8e59e86-a94f-4dad-9b22-7ebef41f51c8", null, "EXPENSE", "9. Outros"),
            new CategorySeed("2e6122cd-2a72-43a4-9597-8cd3a9aa97d6", "a8e59e86-a94f-4dad-9b22-7ebef41f51c8", "EXPENSE", "Tarifas Bancárias"),
            new CategorySeed("f0a7d3ee-b488-45bd-ae63-233fd4bacaba", "a8e59e86-a94f-4dad-9b22-7ebef41f51c8", "EXPENSE", "Carnê Leão"),
            new CategorySeed("0bf48c2d-c5ca-4161-93a4-df3c9f08b163", "a8e59e86-a94f-4dad-9b22-7ebef41f51c8", "EXPENSE", "Pensões"),
            new CategorySeed("86cc32fe-c92f-4c21-a516-55cb2119a264", "a8e59e86-a94f-4dad-9b22-7ebef41f51c8", "EXPENSE", "Gorjetas / caixinhas"),
            new CategorySeed("a9114861-c5b6-4a39-be12-304fc02ac6a4", "a8e59e86-a94f-4dad-9b22-7ebef41f51c8", "EXPENSE", "Doações e dízimos"),
            new CategorySeed("1ab1f78d-47ad-4c19-8447-5f9c0d1a42dd", "a8e59e86-a94f-4dad-9b22-7ebef41f51c8", "EXPENSE", "Emprestimos"),
            new CategorySeed("b87f7019-1c99-477b-9982-86595df0b518", "a8e59e86-a94f-4dad-9b22-7ebef41f51c8", "EXPENSE", "Eventos"),
            new CategorySeed("1c6eb4b3-18ac-444e-a4f3-809d631ae833", "a8e59e86-a94f-4dad-9b22-7ebef41f51c8", "EXPENSE", "Retiros"),
            new CategorySeed("631a8c08-c794-4b22-b6c2-98c5eb760942", "a8e59e86-a94f-4dad-9b22-7ebef41f51c8", "EXPENSE", "Extras diários"),
            new CategorySeed("587e90d0-dcbd-4e5b-97e9-6fa09b57e8b0", "a8e59e86-a94f-4dad-9b22-7ebef41f51c8", "EXPENSE", "Outros")
    };

    private final UserRepository userRepository;
    private final UserCategoryRepository categoryRepository;
    private final Storage storage;

    void seed(@Observes StartupEvent event) {

        // Semeia o admin e captura o ID para atrelar às categorias
        val adminId = UUID.fromString(userRepository.findByUsername("admin")
                .map(User::id)
                .orElseGet(() -> {
                    val id = UUID.randomUUID().toString();
                    userRepository.save(new User(
                            id,
                            "admin",
                            null,
                            BcryptUtil.bcryptHash("admin")
                    ));
                    Logger.info("Seed => usuário 'admin' criado com id %s", id);
                    return id;
                }));

        // Verifica se a tabela/entidade de categorias está vazia
        if (categoryRepository.findAllByUser(adminId).isEmpty()) {
            for (val c : CATEGORIES) {
                categoryRepository.save(new UserCategory(
                        UUID.fromString(c.id()),
                        adminId,      // COD_USER
                        Transaction.Type.valueOf(c.nature()),   // TXT_NATURE
                        c.name(),     // TXT_NAME
                        c.parentId() == null ? null : UUID.fromString(c.parentId()), // COD_PARENT
                        false        // FLG_SYSTEM (N)
                ));
            }
            Logger.info("Seed => %d categorias iniciais criadas para o usuário admin", CATEGORIES.length);
        }

        // Semeia a fonte global de centros de custo via Storage
        if (!storage.exists(COST_CENTERS_FILE)) {
            storage.write(COST_CENTERS_FILE, COST_CENTERS_KEY, COST_CENTERS_JSON.getBytes(StandardCharsets.UTF_8));
            Logger.info("Seed => fonte global de centros de custo criada (%s)", COST_CENTERS_FILE);
        }
    }
}