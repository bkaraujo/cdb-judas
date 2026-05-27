package br.commons.framework.persistence.inmemory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryStorageTest {

    private InMemoryStorage storage;

    @BeforeEach
    void setUp() {
        storage = new InMemoryStorage();
    }

    // ── write / read ──────────────────────────────────────────────────────────

    @Test
    void writeAndRead_returnsStoredBytes() {
        byte[] data = "hello".getBytes();
        storage.write("file1", "key1", data);
        assertArrayEquals(data, storage.read("file1", "key1"));
    }

    @Test
    void read_missingFile_returnsNull() {
        assertNull(storage.read("nonexistent", "key"));
    }

    @Test
    void read_missingKey_returnsNull() {
        storage.write("file1", "key1", new byte[]{1});
        assertNull(storage.read("file1", "missing"));
    }

    @Test
    void write_overwritesExistingKey() {
        storage.write("file1", "key", "old".getBytes());
        storage.write("file1", "key", "new".getBytes());
        assertArrayEquals("new".getBytes(), storage.read("file1", "key"));
    }

    // ── exists ────────────────────────────────────────────────────────────────

    @Test
    void exists_afterWrite_returnsTrue() {
        storage.write("file1", "k", new byte[]{});
        assertTrue(storage.exists("file1"));
    }

    @Test
    void exists_beforeWrite_returnsFalse() {
        assertFalse(storage.exists("nonexistent"));
    }

    // ── listFiles ─────────────────────────────────────────────────────────────

    @Test
    void listFiles_noFilter_returnsAll() {
        storage.write("report.json", "k", new byte[]{});
        storage.write("data.csv", "k", new byte[]{});
        List<String> files = storage.listFiles(null, null);
        assertTrue(files.contains("report.json"));
        assertTrue(files.contains("data.csv"));
    }

    @Test
    void listFiles_prefixFilter() {
        storage.write("report.json", "k", new byte[]{});
        storage.write("data.csv", "k", new byte[]{});
        List<String> files = storage.listFiles("report", null);
        assertTrue(files.contains("report.json"));
        assertFalse(files.contains("data.csv"));
    }

    @Test
    void listFiles_suffixFilter() {
        storage.write("report.json", "k", new byte[]{});
        storage.write("data.json", "k", new byte[]{});
        storage.write("data.csv", "k", new byte[]{});
        List<String> files = storage.listFiles(null, ".json");
        assertTrue(files.contains("report.json"));
        assertTrue(files.contains("data.json"));
        assertFalse(files.contains("data.csv"));
    }

    @Test
    void listFiles_prefixAndSuffixFilter() {
        storage.write("report.json", "k", new byte[]{});
        storage.write("report.csv", "k", new byte[]{});
        storage.write("data.json", "k", new byte[]{});
        List<String> files = storage.listFiles("report", ".json");
        assertEquals(1, files.size());
        assertTrue(files.contains("report.json"));
    }

    @Test
    void listFiles_empty_returnsEmptyList() {
        assertTrue(storage.listFiles(null, null).isEmpty());
    }

    // ── clear ─────────────────────────────────────────────────────────────────

    @Test
    void clear_removesAllData() {
        storage.write("file1", "k", new byte[]{1});
        storage.write("file2", "k", new byte[]{2});
        storage.clear();
        assertFalse(storage.exists("file1"));
        assertFalse(storage.exists("file2"));
        assertTrue(storage.listFiles(null, null).isEmpty());
    }

    // ── multiple keys per file ────────────────────────────────────────────────

    @Test
    void singleFile_multipleKeys() {
        storage.write("file1", "key1", "a".getBytes());
        storage.write("file1", "key2", "b".getBytes());
        assertArrayEquals("a".getBytes(), storage.read("file1", "key1"));
        assertArrayEquals("b".getBytes(), storage.read("file1", "key2"));
    }
}
