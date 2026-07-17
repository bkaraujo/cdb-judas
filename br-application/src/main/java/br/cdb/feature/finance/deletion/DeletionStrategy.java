package br.cdb.feature.finance.deletion;

/** Como tratar as transações vinculadas quando a exclusão simples é recusada (409). */
public enum DeletionStrategy {
    /** Reatribui os vínculos para outra entidade do mesmo tipo antes de excluir. */
    MOVE,
    /** Exclui as transações vinculadas junto com a entidade. */
    DELETE,
    /** Só tag: desvincula (apaga a associação) sem tocar nas transações. */
    DETACH
}
