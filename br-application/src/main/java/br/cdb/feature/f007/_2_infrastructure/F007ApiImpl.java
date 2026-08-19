package br.cdb.feature.f007._2_infrastructure;

import br.cdb.core.web.AbstractApiClient;
import br.cdb.core.web.HTTPApi;
import br.cdb.feature.f007.F007Api;
import br.cdb.feature.f007._2_infrastructure.web.ImportResource;
import org.jspecify.annotations.NullMarked;

/**
 * Cliente tipado da própria API pública de {@code f007}, para consumo cross-slice, no mesmo papel de
 * {@code f002.F002Api}/{@code f003.F003Api}/{@code f004.F004Api}/{@code f006.F006Api}. Hoje sem
 * consumidor: nenhuma fatia lê a importação de fora dela.
 *
 * <p>Espelha só {@link ImportResource#confirm} — {@code POST /import/preview}
 * ({@link ImportResource#preview}) é {@code multipart/form-data}, e {@link HTTPApi} só monta
 * corpo JSON (Etapa 0 de {@code .claude/plan.md}); montar multipart no {@code HttpClient} do JDK à
 * mão não se justifica para um método sem chamador. Ver "Ponto em aberto" no plano.
 *
 * <p>Context-wired ({@code Context.get(F007Api.class)}), sem estado próprio: {@link HTTPApi}
 * é bean CDI, resolvido a cada chamada.
 */
@NullMarked
public class F007ApiImpl extends AbstractApiClient implements F007Api {

    @Override
    public ConfirmResult confirm(ConfirmBody body) {
        return post("/accounts/transactions/import/confirm", body, ConfirmResult.class);
    }
}
