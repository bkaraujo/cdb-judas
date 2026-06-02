package br.community.feature.user.accounts.statementimport.preview;

import org.jspecify.annotations.NullMarked;

import java.util.List;

/** The parsed result of one bank statement: every kept movement, in document order. */
@NullMarked
public record ParsedBankStatement(List<ParsedBankStatementLine> lines) {}
