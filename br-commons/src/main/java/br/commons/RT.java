package br.commons;

import br.commons.tools.Meta;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@NullMarked
public abstract class RT {
    private RT(){}

    public static volatile boolean running = false;

    /**
     * <p>
     * Prefixos de pacote tratados como "framework"/infraestrutura por {@link Meta},
     * usados para descartar frames irrelevantes ao localizar código do usuário:
     * <ul>
     *   <li>{@link Meta#stackFrame()} — filtra esses frames antes de resolver
     *       o caller real (ex.: origem de log);</li>
     *   <li>{@link Meta#userMainClassName()} — percorre a pilha de baixo pra cima
     *       procurando a primeira classe cujo nome NÃO comece com nenhum destes
     *       prefixos, tratando-a como ponto de entrada do usuário.</li>
     * </ul>
     * </p>
     * <p>
     * Mutável em runtime: bootstrap/testes podem registrar prefixos adicionais
     * (ex. pacotes de outro módulo) para que também sejam tratados como
     * infraestrutura.</p>
     *
     * <p>Por isso {@code Meta.isStackFrameExcluded} não cacheia
     * a decisão por classe — cache ficaria preso ao estado desta lista na
     * 1ª consulta e ignoraria adições posteriores.
     * </p>
     */
    public static final List<String> packages = new CopyOnWriteArrayList<>(
            List.of(
                    "java.",
                    "javax.",
                    "jdk.",
                    "sun.",
                    "br.commons.framework.logger.",
                    "br.commons.Logger",
                    "br.commons.tools.Meta",
                    "br.commons.debug.Execution"
            )
    );
}

