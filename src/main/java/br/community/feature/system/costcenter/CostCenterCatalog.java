package br.community.feature.system.costcenter;

import br.commons.framework.persistence.Storage;
import br.community.context.monetary._0_domain.model.CostCenter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.util.List;

/** Fonte global (somente leitura) de centros de custo, servida do arquivo cost-centers.json. */
@ApplicationScoped
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
        } catch (IOException e) {
            throw new RuntimeException("Erro ao ler centros de custo globais", e);
        }
    }
}
