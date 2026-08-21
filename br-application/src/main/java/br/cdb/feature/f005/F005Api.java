package br.cdb.feature.f005;

import br.cdb.feature.f005._0_domain.model.Nature;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

/** Cliente da API pública de {@code f005} */
@NullMarked
public interface F005Api {

    /** Categoria de sistema de transferência ("9. Outros / Transferência") da natureza pedida. */
    UUID transferCategoryId(Nature nature);

}
