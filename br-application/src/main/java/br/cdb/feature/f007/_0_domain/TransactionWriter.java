package br.cdb.feature.f007._0_domain;

import br.commons.Result;
import br.commons.business.BusinessError;
import org.jspecify.annotations.NullMarked;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Escrita de transação para a importação — contrato f006↔f007 (D3 de {@code .claude/plan.md}). A
 * leitura cross-slice vai por {@code F006Api}/{@code F002Api}/{@code F003Api} (HTTP real via
 * {@code InternalApi}), mas uma fatura grande vira centenas de linhas: uma chamada loopback por linha
 * seria uma regressão de desempenho real, e a importação precisa do {@code groupId} determinístico
 * (dedup entre reimportações) que {@code WriteUseCases.createInstallments} não produz. Por isso a
 * escrita continua em processo, atrás desta porta — implementada por
 * {@code f999.TransactionWriterAdapter}, que chama {@code f006.WriteUseCases} direto.
 *
 * <p>Resolvida por chamada ({@code Context.get(TransactionWriter.class)}), nunca guardada em campo:
 * {@code Context.set} é eager e {@code F007Module} roda antes de {@code F999Module} na lista de
 * {@code FeatureBootstrap} — uma classe de f007 construída durante {@code F007Module.initialize()}
 * que resolvesse esta porta em campo a encontraria ainda não publicada.
 */
@NullMarked
public interface TransactionWriter {

    Result<UUID, BusinessError> create(ImportedTransaction row);

    Result<Void, BusinessError> confirmStatus(UUID transactionId, LocalDate paymentDate);
}
