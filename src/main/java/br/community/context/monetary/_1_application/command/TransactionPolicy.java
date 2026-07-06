package br.community.context.monetary._1_application.command;

import org.jspecify.annotations.NullMarked;

import java.util.UUID;

/** O que fazer com as transações vinculadas a uma conta/cartão quando ela é excluída. */
@NullMarked
public sealed interface TransactionPolicy {

    /** Recusa a exclusão se houver transações vinculadas. */
    @NullMarked
    record Block() implements TransactionPolicy {}

    /** Reatribui as transações para {@code targetId} (mesmo tipo de entidade) antes de excluir. */
    @NullMarked
    record Move(UUID targetId) implements TransactionPolicy {}

    /** Apaga as transações vinculadas junto com a entidade. */
    @NullMarked
    record Purge() implements TransactionPolicy {}
}
