/** O trio novo/editar/excluir que toda página de cadastro religava à mão. Resolve o
 * `data-id` do elemento clicado contra a lista da página e entrega o registro pronto. */
import $ from 'jquery';

export interface RecordActionsOptions<T> {
  /** Resolve o registro pelo `data-id` do botão. Tipicamente `(id) => byId(state.xs, id)`. */
  find: (id: string) => T | null;
  onNew?: () => void;
  onEdit?: (record: T) => void;
  onDelete?: (record: T) => void;
  /** Roda antes de resolver o registro — ex.: fechar menus de contexto abertos. */
  before?: () => void;
  /** Chamado quando `find` devolve null. Sem isto o clique é ignorado em silêncio. */
  onMissing?: () => void;
}

export function bindRecordActions<T>($root: JQuery, ns: string, opts: RecordActionsOptions<T>): void {
  const onNew = opts.onNew;
  if (onNew) $root.on('click' + ns, '[data-act=new]', () => onNew());

  function dispatch(handler: (record: T) => void) {
    return function (this: HTMLElement, e: JQuery.TriggeredEvent): void {
      e.stopPropagation();
      if (opts.before) opts.before();
      const record = opts.find($(this).attr('data-id') as string);
      if (record) handler(record);
      else if (opts.onMissing) opts.onMissing();
    };
  }

  const onEdit = opts.onEdit;
  if (onEdit) $root.on('click' + ns, '[data-act=edit]', dispatch(onEdit));
  const onDelete = opts.onDelete;
  if (onDelete) $root.on('click' + ns, '[data-act=trash]', dispatch(onDelete));
}
