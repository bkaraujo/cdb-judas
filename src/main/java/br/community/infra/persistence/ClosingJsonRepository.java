package br.community.infra.persistence;

import br.commons.framework.persistence.Storage;
import br.community.context.monetary._0_domain.repository.ClosingRepository;
import br.community.core.web.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import tools.jackson.databind.ObjectMapper;

import java.time.YearMonth;
import java.util.Optional;

@NullMarked
@RequiredArgsConstructor
public class ClosingJsonRepository implements ClosingRepository {

    private static final String KEY = "closing";

    private final ObjectMapper mapper;
    private final Storage storage;

    @Override
    public Optional<YearMonth> find() {
        val bytes = storage.read(fileName(), KEY);
        if (bytes == null) return Optional.empty();

        val s = mapper.readValue(bytes, String.class);
        return Optional.of(s).map(YearMonth::parse);
    }

    @Override
    public void save(YearMonth ym) {
        storage.write(fileName(), KEY, mapper.writeValueAsBytes(ym.toString()));
    }

    @Override
    public void clear() {
        storage.write(fileName(), KEY, mapper.writeValueAsBytes(null));
    }

    private String fileName() {
        return CurrentUser.getId() + ".json";
    }
}
