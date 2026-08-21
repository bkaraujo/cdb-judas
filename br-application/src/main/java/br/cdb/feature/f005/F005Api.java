package br.cdb.feature.f005;

import br.cdb.feature.f005._0_domain.model.Nature;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

/** Cliente da API pública de {@code f005} */
@NullMarked
public interface F005Api {

    /** Categoria de sistema de transferência ("9. Outros / Transferência") da natureza pedida. */
    UUID transferCategoryId(Nature nature);

    /** Natureza da categoria — dedicada porque Transaction não guarda mais a própria Nature (agora é
     *  atributo só de Category); consumido por f006.RequestMapper#toDto ao montar a resposta HTTP de
     *  uma transação já com categoryId resolvido. */
    Nature natureOf(UUID categoryId);

}
