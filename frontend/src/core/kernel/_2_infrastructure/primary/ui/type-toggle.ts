import { esc } from '@/core/kernel/_0_domain/format.ts';

export interface TypeToggleOption {
  value: string;
  label: string;
  color: string;
}

export interface TypeToggleOptions {
  disabled?: boolean;
  act?: string;
  disabledTitle?: string;
}

/** `options`: [{value,label,color}]; `activeValue` picks which is highlighted. `opts.disabled`
 * greys out and locks every button (transactions locks the type while editing a transfer leg);
 * `opts.act` overrides the data-act (default 'set-form-type'). Returns the concatenated <button>
 * HTML — works both interpolated into a template and passed straight to `$row.append(...)`. */
export function typeToggleHtml(options: readonly TypeToggleOption[] | null | undefined, activeValue: string, opts: TypeToggleOptions = {}): string {
  const disabled = !!opts.disabled;
  return (options || [])
    .map((o) => {
      const active = o.value === activeValue;
      const cls = 'type-toggle-btn' + (active ? ' is-active type-toggle-btn--' + esc(o.color) : '');
      return (
        '<button type="button" class="' + cls + '" data-act="' + esc(opts.act || 'set-form-type') + '" ' +
        'data-type="' + esc(o.value) + '" ' +
        (disabled ? 'disabled title="' + esc(opts.disabledTitle || '') + '"' : '') + '>' +
        esc(o.label) +
        '</button>'
      );
    })
    .join('');
}
