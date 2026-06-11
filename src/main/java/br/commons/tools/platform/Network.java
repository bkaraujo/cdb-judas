package br.commons.tools.platform;

import org.jspecify.annotations.NullMarked;

/**
 * Platform-agnostic network interface.
 * Provides native TCP port inspection.
 */
@NullMarked
public interface Network {

    /**
     * Verifica se a porta TCP está em uso (socket em estado LISTEN) no host local.
     * <p>
     * Consulta nativa da tabela de conexões TCP do sistema operacional, considerando
     * apenas sockets em LISTEN. Estados transitórios (ex: {@code TIME_WAIT}) são ignorados,
     * pois não representam um servidor ativo na porta.
     *
     * @param port porta TCP a verificar
     * @return {@code true} se houver um listener ativo na porta
     */
    boolean isPortInUse(int port);
}
