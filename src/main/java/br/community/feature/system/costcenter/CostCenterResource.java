package br.community.feature.system.costcenter;

import br.community.context.monetary._0_domain.model.MonetaryCenter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Centro de custo é dado fixo do sistema: rota global, somente leitura (sem namespace de usuário). */
@NullMarked
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/cost-center", produces = MediaType.APPLICATION_JSON_VALUE)
public class CostCenterResource {

    private final CostCenterCatalog catalog;

    @GetMapping
    public List<MonetaryCenter> list() {
        return catalog.list();
    }
}
