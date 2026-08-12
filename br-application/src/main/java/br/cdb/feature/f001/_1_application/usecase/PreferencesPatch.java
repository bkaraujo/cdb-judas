package br.cdb.feature.f001._1_application.usecase;

import br.cdb.feature.f001._0_domain.model.Preferences;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Patch parcial de preferências (todos os campos anuláveis). Distinto do record de
 * domínio {@link Preferences}: um campo nulo significa "manter o valor atual" na regra
 * de merge ({@link Preferences#merge(PreferencesPatch)}).
 */
@NullMarked
public record PreferencesPatch(
        @Nullable String theme,
        @Nullable String language,
        @Nullable String locale,
        @Nullable Boolean sidebarCollapsed
) {}
