package br.community.context.monetary._2_infrastructure;

import br.commons.framework.persistence.Storage;
import br.community.context.monetary._0_domain.repository.ClosingRepository;
import br.community.core.web.security.CurrentUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.io.UncheckedIOException;
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

        try {
            val s = mapper.readValue(bytes, String.class);
            return Optional.ofNullable(s).map(YearMonth::parse);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    @Override
    public void save(YearMonth ym) {
        try {
            storage.write(fileName(), KEY, mapper.writeValueAsBytes(ym.toString()));
        } catch (IOException e) {
            throw new UncheckedIOException("Error writing closing", e);
        }
    }

    @Override
    public void clear() {
        try {
            storage.write(fileName(), KEY, mapper.writeValueAsBytes(null));
        } catch (IOException e) {
            throw new UncheckedIOException("Error clearing closing", e);
        }
    }

    private String fileName() {
        return CurrentUser.getId() + ".json";
    }
}
