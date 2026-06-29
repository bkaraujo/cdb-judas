package br.commons.framework.persistence.jdbc;

import br.commons.tools.Strings;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class JDBCProperties implements Cloneable {

    private String name = "default";
    public String name() { return name; }
    public void name(String name) { this.name = name; }

    private String driver = Strings.EMPTY;
    public String driver() { return driver; }
    public void driver(String driver) { this.driver = driver; }

    private String url = Strings.EMPTY;
    public String url() { return url; }
    public void url(String url) { this.url = url; }

    private String username = Strings.EMPTY;
    public String username() { return username; }
    public void username(String username) { this.username = username; }

    private String password = Strings.EMPTY;
    public String password() { return password; }
    public void password(String password) { this.password = password; }

    private int minPoolSize = 1;
    public int minPoolSize() { return minPoolSize; }
    public void minPoolSize(int minPoolSize) { this.minPoolSize = minPoolSize; }

    private int maxPoolSize = 3;
    public int maxPoolSize() { return maxPoolSize; }
    public void maxPoolSize(int maxPoolSize) { this.maxPoolSize = maxPoolSize; }

    private int connectionTimeout = 30_000;
    public int connectionTimeout() { return connectionTimeout; }
    public void connectionTimeout(int connectionTimeout) { this.connectionTimeout = connectionTimeout; }

    private int idleTimeout = 600_000;
    public int idleTimeout() { return idleTimeout; }
    public void idleTimeout(int idleTimeout) { this.idleTimeout = idleTimeout; }

    private int maxLifetime = 1_800_000;
    public int maxLifetime() { return maxLifetime; }
    public void maxLifetime(int maxLifetime) { this.maxLifetime = maxLifetime; }

    private boolean autoCommit = false;
    public boolean autoCommit() { return autoCommit; }
    public void autoCommit(boolean autoCommit) { this.autoCommit = autoCommit; }

    private @Nullable String validationQuery;
    public @Nullable String validationQuery() { return validationQuery; }
    public void validationQuery(String validationQuery) { this.validationQuery = validationQuery; }

    public JDBCProperties clone()  {
        val clone = new JDBCProperties();

        clone.name = name;
        clone.driver = driver;
        clone.url = url;
        clone.username = username;
        clone.password = password;
        clone.minPoolSize = minPoolSize;
        clone.maxPoolSize = maxPoolSize;
        clone.connectionTimeout = connectionTimeout;
        clone.idleTimeout = idleTimeout;
        clone.maxLifetime = maxLifetime;
        clone.autoCommit = autoCommit;
        clone.validationQuery = validationQuery;

        return clone;
    }

}
