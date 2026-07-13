package br.cdb.infra.persistence;

import org.jspecify.annotations.NullMarked;

import java.util.concurrent.locks.ReentrantReadWriteLock;

@NullMarked
public class AbstractCachingRepository {
    protected final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(false);
}
