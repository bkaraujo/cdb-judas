package br.community.feature.system.costcenter;

import br.commons.framework.persistence.Storage;
import br.community.context.monetary._0_domain.model.CostCenter;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/** Fonte global (somente leitura) de centros de custo, servida do arquivo cost-centers.json. */
@Service
@NullMarked
@RequiredArgsConstructor
public class CostCenterCatalog {

    private static final String FILE = "cost-centers.json";
    private static final String KEY = "costCenters";

    private final Storage storage;
    private final ObjectMapper mapper;

    public List<CostCenter> list() {
        val bytes = storage.read(FILE, KEY);
        if (bytes == null || bytes.length == 0) return List.of();
        try {
            val listType = mapper.getTypeFactory().constructCollectionType(List.class, CostCenter.class);
            return mapper.readValue(bytes, listType);
        } catch (JacksonException e) {
            throw new RuntimeException("Erro ao ler centros de custo globais", e);
        }
    }
}
