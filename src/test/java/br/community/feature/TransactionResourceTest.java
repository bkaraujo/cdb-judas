package br.community.feature;

import br.community.feature.user.accounts.transactions.Transaction;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionResourceTest extends BaseHttpTest {

    private UUID createAccount(String color) throws Exception {
        String json = """
            {"name":"Conta","balance":1000.00,"type":"CHECKING","color":"%s","active":true}
            """.formatted(color);
        String resp = mockMvc.perform(post("/api/{u}/accounts", TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON).content(json))
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(resp).get("id").asText());
    }

    // Transações só podem ser lançadas em subcategorias (não em macro-categorias).
    private UUID createLeafCategory() throws Exception {
        String macroResp = mockMvc.perform(post("/api/" + TEST_USER_ID + "/categories")
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Moradia\",\"nature\":\"EXPENSE\"}"))
                .andReturn().getResponse().getContentAsString();
        UUID macroId = UUID.fromString(objectMapper.readTree(macroResp).get("id").asText());

        String subJson = """
            {"name":"Aluguel","nature":"EXPENSE","parentId":"%s"}
            """.formatted(macroId);
        String subResp = mockMvc.perform(post("/api/" + TEST_USER_ID + "/categories")
                .contentType(MediaType.APPLICATION_JSON).content(subJson))
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(subResp).get("id").asText());
    }

    @Test
    void deveGerenciarTransacoesPorConta() throws Exception {
        UUID accountId = createAccount("#000000");
        UUID categoryId = createLeafCategory();

        // Criar com o id da conta vindo do caminho (sem accountId no corpo).
        String createJson = """
            {
              "description": "Pagamento Aluguel",
              "amount": -2500.00,
              "date": "2024-04-01",
              "categoryId": "%s",
              "costCenterId": "d0000000-0000-0000-0000-000000000002",
              "status": "pending",
              "type": "expense",
              "installments": 1,
              "editMode": "single"
            }
            """.formatted(categoryId);

        String response = mockMvc.perform(post("/api/{u}/accounts/{acc}/transactions", TEST_USER_ID, accountId)
                .contentType(MediaType.APPLICATION_JSON).content(createJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.amount").value(-2500.00))
                .andExpect(jsonPath("$.accountId").value(accountId.toString()))
                .andReturn().getResponse().getContentAsString();
        UUID id = objectMapper.readValue(response, Transaction.class).id();

        // Lista entre contas com filtros.
        mockMvc.perform(get("/api/{u}/accounts/transactions", TEST_USER_ID).param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].description").value("Pagamento Aluguel"));

        mockMvc.perform(get("/api/{u}/accounts/transactions", TEST_USER_ID).param("status", "pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/{u}/accounts/transactions", TEST_USER_ID).param("status", "confirmed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // Lista por conta.
        mockMvc.perform(get("/api/{u}/accounts/{acc}/transactions", TEST_USER_ID, accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // Alterar status informando data de pagamento.
        mockMvc.perform(patch("/api/{u}/accounts/{acc}/transactions/{id}/status", TEST_USER_ID, accountId, id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"confirmed\",\"paymentDate\":\"2024-04-02\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("confirmed"))
                .andExpect(jsonPath("$.paymentDate").value("2024-04-02"));

        // Excluir.
        mockMvc.perform(delete("/api/{u}/accounts/{acc}/transactions/{id}", TEST_USER_ID, accountId, id))
                .andExpect(status().isNoContent());
    }

    @Test
    void deveCriarParcelasEExcluirFuturas() throws Exception {
        UUID accountId = createAccount("#222222");
        UUID categoryId = createLeafCategory();

        String createJson = """
            {"description":"TV","amount":-300.00,"date":"2024-06-01","categoryId":"%s","costCenterId":"d0000000-0000-0000-0000-000000000002","status":"pending","type":"expense","installments":3,"editMode":"single"}
            """.formatted(categoryId);

        mockMvc.perform(post("/api/{u}/accounts/{acc}/transactions", TEST_USER_ID, accountId)
                .contentType(MediaType.APPLICATION_JSON).content(createJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.groupId").exists())
                .andExpect(jsonPath("$.installmentNumber").value(1))
                .andExpect(jsonPath("$.totalInstallments").value(3));

        String listResp = mockMvc.perform(get("/api/{u}/accounts/transactions", TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andReturn().getResponse().getContentAsString();

        UUID firstId = null;
        for (var node : objectMapper.readTree(listResp)) {
            if (node.get("installmentNumber").asInt() == 1) {
                firstId = UUID.fromString(node.get("id").asText());
                break;
            }
        }

        mockMvc.perform(delete("/api/{u}/accounts/{acc}/transactions/{id}", TEST_USER_ID, accountId, firstId)
                .param("mode", "FUTURE"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/{u}/accounts/transactions", TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void deveTransferirEntreContas() throws Exception {
        UUID origem = createAccount("#010101");
        UUID destino = createAccount("#020202");

        String transferJson = """
            {"fromAccountId":"%s","toAccountId":"%s","date":"2024-07-01","amount":150.00}
            """.formatted(origem, destino);

        mockMvc.perform(post("/api/{u}/accounts/transactions/transfer", TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON).content(transferJson))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/{u}/accounts/transactions", TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // Saída pertence à conta de origem.
        mockMvc.perform(get("/api/{u}/accounts/{acc}/transactions", TEST_USER_ID, origem)
                .param("type", "expense"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
