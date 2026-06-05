package br.community.feature.user.accounts.statement.importer.preview;

import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * The parsed result of one credit-card statement: every kept charge line. A statement can carry
 * charges from several cards (titular + additional cardholders), so the distinct last4s are derived
 * from the lines rather than assumed to be a single card.
 */
@NullMarked
public record ParsedStatement(List<ParsedStatementLine> lines) {

    public List<String> last4s() {
        return lines.stream().map(ParsedStatementLine::last4).distinct().toList();
    }
}
