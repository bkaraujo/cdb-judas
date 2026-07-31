package br.cdb.feature.f000;

import br.cdb.feature.f000._0_domain.SSE;
import br.cdb.feature.f000._0_domain.repository.CostCenterRepository;
import br.cdb.feature.f000._0_domain.repository.PersonRepository;
import br.cdb.feature.f000._1_application.service.CostCenterService;
import br.cdb.feature.f000._1_application.service.PersonService;
import br.cdb.feature.f000._2_infrastructure.persistence.CachingPersonRepository;
import br.cdb.feature.f000._2_infrastructure.persistence.CostCenterJDBCRepository;
import br.cdb.feature.f000._2_infrastructure.persistence.PersonJDBCRepository;
import br.cdb.feature.f000._2_infrastructure.service.SseService;
import br.commons.Logger;
import br.commons.Result;
import br.commons.annotation.Lifecycle;
import br.commons.framework.cdi.Context;
import org.jspecify.annotations.NullMarked;

/**
 * Módulo da fatia {@code f000} (kernel). Sem CDI: classe pura montada pelo {@link Context} de
 * {@code br-commons} e inicializada por {@code F999Module.FeatureBootstrap}, na ordem da lista de
 * módulos.
 */
@NullMarked
public class F000Module implements Lifecycle {

    @Override
    public Result<Void, Throwable> initialize() {
        Logger.debug("Iniciando módulo..");

        Context.set(SSE.class, SseService::new);
        Context.set(CostCenterRepository.class, CostCenterJDBCRepository::new);
        Context.set(PersonRepository.class, () -> new CachingPersonRepository(new PersonJDBCRepository()));
        // Os dois services alcançados por Context.get() estrito (PersonUseCase/CostCenterUseCase);
        // as demais engines resolvem suas portas em campo e o Context as instancia sozinho via tryGet.
        Context.set(PersonService.class, () -> new PersonService(Context.get(PersonRepository.class)));
        Context.set(CostCenterService.class, CostCenterService::new);

        return Result.success();
    }
}
