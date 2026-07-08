package br.community.feature.system.costcenter;

import br.community.context.monetary._0_domain.model.CostCenter;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/** Centro de custo é dado fixo do sistema: rota global, somente leitura (sem namespace de usuário). */
@NullMarked
@Path("/api/cost-center")
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class CostCenterResource {

    @GET
    public List<CostCenter> list() {
        return List.of(CostCenter.FIXO, CostCenter.VARIAVEL);
    }
}
