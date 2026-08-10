/**
 * Três responsabilidades (não-HTTP), última fatia — pode depender de todas:
 *
 * <p><b>(a) Provisionamento no startup</b> — {@code F999Module.onStart} chama
 * {@code f000.UserService.createUser("admin", ...)} dentro de uma única transação; o próprio
 * {@code UserService} publica {@code UserEvents.Created}, reagido sincronamente (mesma transação,
 * {@code MessageBus} despacha na mesma thread) pelo listener anônimo em {@code f005.F005Module.onStart},
 * que semeia o catálogo default de categorias para a pessoa recém-criada.
 *
 * <p><b>(b) Único dono do dispatch SSE</b> — {@code AccountStreamListener}/{@code TagStreamListener}/
 * {@code CategoryStreamListener} (em {@code _1_application}) reagem ao vocabulário de eventos
 * publicado por f002/f004/f005/f006/f007 após cada mutação já persistida e chamam {@code SSE.dispatch}
 * — nenhuma outra fatia o faz.
 *
 * <p><b>(c) Composition root</b> — {@code _2_infrastructure.adapter} é onde mora o adapter que liga a
 * porta de uma fatia ao provedor de outra, resolvido por CDI puro (sem {@code @Produces}/
 * {@code Context}). Os 4 casos que existiam até a fase 3 (transferência de categoria, overlay de
 * conta/categoria/import) viraram evento (best-effort, cascata de exclusão) ou leitura síncrona via
 * {@code f000.InternalApi} (HTTP interno contra o endpoint público da fatia dona) na fase 4. Duas
 * instâncias vivas hoje: {@code DeletionQueueAdapter} (fase 5) liga {@code f002.DeletionQueue} (porta)
 * a {@code f999.DeletionQueueService} (provedor), já que f002 não pode importar f999 direto;
 * {@code TransactionWriterAdapter} ({@code .claude/plan.md}, extração de f007) liga
 * {@code f007.TransactionWriter} (porta) a {@code f006.WriteUseCases} (provedor) — a escrita da
 * importação, que precisa ficar em processo (não HTTP por linha).
 */
@NullMarked
package br.cdb.feature.f999;

import org.jspecify.annotations.NullMarked;
