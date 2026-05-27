package br.community.context.monetary._2_infrastructure;

import br.commons.Logger;
import br.commons.framework.persistence.Storage;
import br.commons.framework.persistence.json.EntityDiff;
import br.commons.tools.Strings;
import br.community.context.monetary._0_domain.model.MonetaryTransaction;
import br.community.context.monetary._0_domain.repository.TransactionRepository;
import br.community.core.web.security.CurrentUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@NullMarked
public final class TransactionJsonRepository implements TransactionRepository {

    private static final String JSON_KEY = "transactions";
    private static final String FILE_SUFFIX = ".json";

    private final ObjectMapper mapper;
    private final Storage storage;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public TransactionJsonRepository(ObjectMapper mapper, Storage storage) {
        this.mapper = mapper;
        this.storage = storage;
    }

    @Override
    public List<MonetaryTransaction> findAll() {
        lock.readLock().lock();
        try {
            val all = new ArrayList<MonetaryTransaction>();
            for (val file : storage.listFiles(userFilePrefix(), FILE_SUFFIX)) {
                if (!isYearFile(file)) continue;
                all.addAll(readFile(file));
            }
            return all;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Optional<MonetaryTransaction> findById(UUID id) {
        return findAll().stream().filter(t -> t.id().equals(id)).findFirst();
    }

    @Override
    public MonetaryTransaction save(MonetaryTransaction entity) {
        lock.writeLock().lock();
        try {
            val before = findById(entity.id()).orElse(null);
            removeFromAllFiles(entity.id());
            val year = entity.date().getYear();
            val file = fileName(year);
            val list = new ArrayList<>(readFile(file));
            list.add(entity);
            writeFile(file, list);
            Logger.verbose(() -> "[%s] transactions/%s diff:%s".formatted(file, entity.id(), EntityDiff.of(mapper, before, entity)));
            return entity;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void deleteById(UUID id) {
        lock.writeLock().lock();
        try {
            removeFromAllFiles(id);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void clearCache() {
        // No cache to clear
    }

    private void removeFromAllFiles(UUID id) {
        for (val file : storage.listFiles(userFilePrefix(), FILE_SUFFIX)) {
            if (!isYearFile(file)) continue;
            val list = new ArrayList<>(readFile(file));
            if (list.removeIf(t -> t.id().equals(id))) {
                writeFile(file, list);
            }
        }
    }

    private String fileName(int year) {
        return CurrentUser.getUsername() + "-" + year + FILE_SUFFIX;
    }

    private String userFilePrefix() {
        return CurrentUser.getUsername() + "-";
    }

    private boolean isYearFile(String fileName) {
        val prefix = userFilePrefix();
        if (!fileName.startsWith(prefix) || !fileName.endsWith(FILE_SUFFIX)) return false;
        val middle = fileName.substring(prefix.length(), fileName.length() - FILE_SUFFIX.length());
        return middle.length() == 4 && Strings.isNumeric(middle);
    }

    private List<MonetaryTransaction> readFile(String file) {
        try {
            val bytes = storage.read(file, JSON_KEY);
            if (bytes == null || bytes.length == 0) return List.of();
            val listType = mapper.getTypeFactory().constructCollectionType(List.class, MonetaryTransaction.class);
            return mapper.readValue(bytes, listType);
        } catch (IOException e) {
            throw new UncheckedIOException("Error reading " + file + ":" + JSON_KEY, e);
        }
    }

    private void writeFile(String file, List<MonetaryTransaction> list) {
        try {
            storage.write(file, JSON_KEY, mapper.writeValueAsBytes(list));
        } catch (IOException e) {
            throw new UncheckedIOException("Error writing " + file + ":" + JSON_KEY, e);
        }
    }
}
