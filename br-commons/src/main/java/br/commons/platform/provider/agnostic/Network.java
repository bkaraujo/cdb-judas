package br.commons.platform.provider.agnostic;

import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

@NullMarked
public abstract class Network {

    private Network() {}

    /**
     * Verifica se a porta TCP já está em uso no host local.
     * <p>
     * Implementação 100% Java (tentativa de bind de {@link ServerSocket}),
     * portável entre sistemas operacionais — não requer provider específico de OS.
     *
     * @param port porta TCP a verificar
     * @return {@code true} se a porta já estiver em uso (bind falhou)
     */
    public static boolean isPortInUse(int port) {
        try (val socket = new ServerSocket()) {
            socket.setReuseAddress(false);
            socket.bind(new InetSocketAddress(port));
            return false;
        } catch (IOException ignored) {
            return true;
        }
    }
}
