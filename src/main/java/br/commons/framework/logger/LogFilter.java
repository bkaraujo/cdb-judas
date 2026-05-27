package br.commons.framework.logger;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Sistema de filtros hierárquicos para configuração de nível de log por pacote.
 *
 * <p>Permite configurar diferentes níveis de log para diferentes pacotes,
 * com suporte a herança hierárquica e caching para performance.
 *
 * <h2>Exemplo de Uso:</h2>
 * <pre>{@code
 * LogFilter filter = new LogFilter(LogLevel.INFO);
 * filter.setPackageLevel("br.framework.mcp", LogLevel.DEBUG);
 *
 * // Herda DEBUG de br.framework.mcp
 * LogLevel level = filter.getEffectiveLevel("br.framework.mcp.MCPServer");
 * }</pre>
 *
 * <h2>Thread-Safety:</h2>
 * Esta classe é thread-safe. Leituras e escritas podem ocorrer concorrentemente.
 *
 * @since 1.0
 */
@NullMarked
public final class LogFilter {

    /**
     * Mapa ordenado: pacote → nível.
     * ConcurrentSkipListMap mantém ordem lexicográfica e é thread-safe.
     */
    private final ConcurrentSkipListMap<String, LogLevel> packageFilters;

    /**
     * Cache: className → LogLevel efetivo.
     * Evita recalcular hierarquia repetidamente.
     */
    private final ConcurrentHashMap<String, LogLevel> cache;

    /**
     * Nível global (fallback).
     */
    private volatile LogLevel globalLevel;

    /**
     * Cria um novo LogFilter com nível padrão.
     *
     * @param defaultLevel Nível de log padrão para classes sem configuração específica
     */
    public LogFilter(LogLevel defaultLevel) {
        this.packageFilters = new ConcurrentSkipListMap<>();
        this.cache = new ConcurrentHashMap<>();
        this.globalLevel = defaultLevel;
    }

    /**
     * Configura o nível de log global.
     * Invalida todo o cache.
     *
     * @param level Novo nível global
     */
    public void level(LogLevel level) {
        this.globalLevel = level;
        this.cache.clear();
    }

    /**
     * Retorna o nível de log global atual.
     *
     * @return Nível global
     */
    public LogLevel level() {
        return globalLevel;
    }

    /**
     * Configura nível de log para um pacote específico.
     * Invalida todo o cache.
     *
     * @param packageName Nome completo do pacote (ex: "br.framework.mcp")
     * @param level Nível de log para o pacote
     */
    public void setPackageLevel(String packageName, LogLevel level) {
        packageFilters.put(packageName, level);
        cache.clear();
    }

    /**
     * Remove configuração de nível para um pacote específico.
     * Invalida todo o cache.
     *
     * @param packageName Nome do pacote a remover
     */
    public void removePackageLevel(String packageName) {
        packageFilters.remove(packageName);
        cache.clear();
    }

    /**
     * Limpa todas as configurações de pacote.
     * Invalida todo o cache.
     */
    public void clearPackageFilters() {
        packageFilters.clear();
        cache.clear();
    }

    /**
     * Verifica se há filtros de pacote configurados.
     *
     * @return true se há filtros específicos, false caso contrário
     */
    public boolean hasPackageFilters() {
        return !packageFilters.isEmpty();
    }

    /**
     * Retorna o nível configurado para um pacote específico, ou null se não configurado.
     *
     * @param packageName Nome do pacote
     * @return Nível configurado ou null
     */
    @Nullable
    public LogLevel getPackageLevel(String packageName) {
        return packageFilters.get(packageName);
    }

    /**
     * Retorna o nível efetivo para uma classe, considerando hierarquia de pacotes.
     * Utiliza cache para otimizar consultas repetidas.
     *
     * @param className Nome completo da classe (ex: "br.framework.mcp.MCPServer")
     * @return Nível efetivo (nunca null)
     */
    public LogLevel getEffectiveLevel(String className) {
        // Tenta cache primeiro
        LogLevel cached = cache.get(className);
        if (cached != null) {
            return cached;
        }

        // Calcula nível efetivo
        LogLevel effective = computeEffectiveLevel(className);
        cache.put(className, effective);
        return effective;
    }

    /**
     * Computa nível efetivo baseado em hierarquia de pacotes.
     * Busca do mais específico ao mais genérico.
     *
     * @param className Nome completo da classe
     * @return Nível efetivo
     */
    private LogLevel computeEffectiveLevel(String className) {
        // Busca do mais específico ao mais genérico
        // Para "br.framework.mcp.MCPServer":
        //   1. Tenta "br.framework.mcp.MCPServer" (classe exata)
        //   2. Tenta "br.framework.mcp" (pacote)
        //   3. Tenta "br.framework"
        //   4. Tenta "br.judia"
        //   5. Usa globalLevel

        String current = className;
        while (true) {
            LogLevel level = packageFilters.get(current);
            if (level != null) {
                return level;
            }

            // Remove último segmento
            int lastDot = current.lastIndexOf('.');
            if (lastDot == -1) {
                break;
            }
            current = current.substring(0, lastDot);
        }

        return globalLevel;
    }
}
