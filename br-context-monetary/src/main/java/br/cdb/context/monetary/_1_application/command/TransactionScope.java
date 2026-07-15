package br.cdb.context.monetary._1_application.command;

import org.jspecify.annotations.NullMarked;

/** Alcance de uma edição/exclusão de transação parcelada: só esta ou esta e as futuras do grupo. */
@NullMarked
public sealed interface TransactionScope {

    @NullMarked
    record Single() implements TransactionScope {}

    @NullMarked
    record Future() implements TransactionScope {}
}
