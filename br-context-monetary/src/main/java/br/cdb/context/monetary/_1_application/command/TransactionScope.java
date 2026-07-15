package br.cdb.context.monetary._1_application.command;

import org.jspecify.annotations.NullMarked;

/** Alcance de uma edição/exclusão de transação parcelada: só esta ou esta e as futuras do grupo. */
@NullMarked
public sealed interface TransactionScope {

    record Single() implements TransactionScope {}

    record Future() implements TransactionScope {}
}
