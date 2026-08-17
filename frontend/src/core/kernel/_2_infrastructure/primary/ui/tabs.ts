import $ from 'jquery';

export interface TabItem {
  id: string;
  label: string;
}

export function tabs(items: readonly TabItem[], active: string, onChange?: (id: string) => void): JQuery {
  const $el = $('<div class="tabs"></div>');
  items.forEach((it) => {
    const isActive = it.id === active;
    const $t = $('<button class="tab' + (isActive ? ' active' : '') + '"></button>').text(it.label).attr('data-id', it.id);
    $el.append($t);
  });
  $el.on('click', '.tab', function () {
    const id = $(this).attr('data-id') as string;
    $el.find('.tab').removeClass('active');
    $(this).addClass('active');
    if (onChange) onChange(id);
  });
  return $el;
}
