package br.cdb.feature.f000._1_application.usecase;

import br.cdb.feature.f000._0_domain.model.CostCenter;
import br.cdb.feature.f000._0_domain.model.Person;
import br.cdb.feature.f000._1_application.command.CostCenterCommand;
import br.cdb.feature.f000._1_application.service.CostCenterService;
import br.cdb.feature.f000._1_application.service.PersonService;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

/**
 * Toda a mutação da fatia-base {@code f000} — o par de {@link ReadUseCase}, mesmo arranjo CQRS de
 * {@code f001}–{@code f006}. Context-wired ({@code Context.tryGet(WriteUseCase.class)}, nunca
 * {@code @Inject}).
 *
 * <p>Ao contrário das outras fatias, o par de {@code f000} não serve a um {@code *Resource}: os
 * recursos daqui são o catálogo estático de centro de custo, o login (que acessa
 * {@code UserRepository} direto, exceção nomeada no {@code ArchitectureTest}), o SSE e a versão.
 * Quem consome este par é <b>outra fatia</b> — {@code f001.ProfileService} (renomear a pessoa) e o
 * {@code UserService} da própria f000 (criar a pessoa dona dos recursos, antes do login).
 *
 * <p>{@code f000} é o kernel plano, sem sub-pacote por assunto: pessoa e centro de custo dividem o
 * mesmo par porque só cabe um {@code WriteUseCase} por pacote. Se um terceiro assunto entrar aqui,
 * vale reavaliar — a alternativa é voltar a nomear por assunto ({@code PersonUseCase} etc.).
 */
@NullMarked
public class WriteUseCase {

    private final PersonService service = Context.get(PersonService.class);
    private final CostCenterService costCenterService = Context.get(CostCenterService.class);

    // ── Pessoa ─────────────────────────────────────────────────────

    /** Cria uma nova pessoa (dona dos recursos). O login apenas a referencia depois. */
    public Result<Person, BusinessError> registerPerson(String name, String locale, String language) {
        return Result.success(service.save(new Person(UUID.randomUUID(), name, locale, language)));
    }

    public Result<Person, BusinessError> renamePerson(Person person, String newName) {
        return service.rename(person, newName);
    }

    // ── Centro de custo ────────────────────────────────────────────

    public Result<CostCenter, BusinessError> upsertCostCenter(CostCenterCommand.Upsert cmd) {
        return switch (cmd) {
            case CostCenterCommand.Create(var description) -> {
                val created = costCenterService.save(UUID.randomUUID(), description);
                yield Result.success(created);
            }
            case CostCenterCommand.Update(var id, var description) -> {
                val updated = costCenterService.save(id, description);
                yield Result.success(updated);
            }
        };
    }

    public Result<Void, BusinessError> deleteCostCenter(CostCenterCommand.Delete command) {
        costCenterService.deleteById(command.id());
        return Result.success();
    }
}
