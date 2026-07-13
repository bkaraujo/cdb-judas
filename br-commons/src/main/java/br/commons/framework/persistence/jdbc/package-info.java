/**
 * JDBC Framework - Database connection pooling and management.
 * <p>
 * This package provides infrastructure for managing database connections with automatic pooling,
 * lifecycle management, and configuration support. The framework is designed to be simple to use
 * while providing enterprise-grade connection management capabilities.
 * </p>
 *
 * <h2>Main Components:</h2>
 * <ul>
 *   <li>{@link br.commons.JDBC} - Main facade for the framework</li>
 *   <li>{@link br.commons.framework.jdbc.JDBCProperties} - Configuration properties for data sources</li>
 *   <li>{@link br.commons.framework.jdbc.DataSource} - Named data source with pooling</li>
 *   <li>{@link br.commons.framework.jdbc.pool.ConnectionPool} - Low-level connection pool implementation</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Configure and register a data source
 * val props = JDBCProperties.builder()
 *     .name("myapp-db")
 *     .driver("org.postgresql.Driver")
 *     .url("jdbc:postgresql://localhost:5432/mydb")
 *     .username("appuser")
 *     .password("secret")
 *     .minPoolSize(5)
 *     .maxPoolSize(20)
 *     .build();
 *
 * JDBC.register(props);
 *
 * // Use connections
 * val connResult = JDBC.getConnection("myapp-db");
 * connResult.ifSuccess(conn -> {
 *     try (conn) {
 *         try (val stmt = conn.prepareStatement("SELECT * FROM users WHERE id = ?")) {
 *             stmt.setLong(1, userId);
 *             try (val rs = stmt.query()) {
 *                 // Process results...
 *             }
 *         }
 *     } catch (Exception e) {
 *         Logger.error("Query error: %s", e.getMessage());
 *     }
 * });
 * connResult.ifFailure(error -> Logger.error("Connection failed: %s", error));
 *
 * // Check pool statistics
 * val stats = JDBC.stats("myapp-db");
 * System.out.println("Active: " + stats.get("active"));
 * System.out.println("Available: " + stats.get("available"));
 *
 * // Cleanup on shutdown (automatic via shutdown hook)
 * JDBC.close();
 * }</pre>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li>Automatic connection pooling with configurable min/max sizes</li>
 *   <li>Connection validation and lifecycle management</li>
 *   <li>Idle connection cleanup and pool maintenance</li>
 *   <li>Multiple named data sources support</li>
 *   <li>Configurable timeouts for connections, idle time, and max lifetime</li>
 *   <li>Custom validation queries for database-specific health checks</li>
 *   <li>Automatic shutdown via JVM shutdown hook</li>
 *   <li>Comprehensive logging and monitoring</li>
 * </ul>
 *
 * @since 1.0
 */
@NullMarked
package br.commons.framework.persistence.jdbc;

import org.jspecify.annotations.NullMarked;