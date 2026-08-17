/** data-theme toggle, sourced from PreferencesService. Reads/writes the theme via the
 * preferences service (mirror cache + write-through PATCH), not raw localStorage. Boot reads the
 * mirror so there is no flash of the wrong theme. */
import $ from 'jquery';

const DEFAULT_THEME = 'dark';

export interface ThemePort {
  getTheme(): string | null;
  setTheme(t: string): void;
}

export interface Theme {
  get(): string;
  set(t: string): void;
  toggle(): void;
  /** Apply the mirror-cached theme immediately (no network wait). */
  restore(): void;
}

export function createTheme(prefs?: ThemePort): Theme {
  function get(): string {
    return document.documentElement.getAttribute('data-theme') || DEFAULT_THEME;
  }

  function apply(t: string): void {
    document.documentElement.setAttribute('data-theme', t);
    $(document).trigger('theme:change', [t]);
  }

  function set(t: string): void {
    apply(t);
    if (prefs) prefs.setTheme(t); // write-through: mirror now, debounced PATCH /api/me
  }

  function toggle(): void {
    set(get() === 'dark' ? 'light' : 'dark');
  }

  function restore(): void {
    const t = (prefs && prefs.getTheme()) || DEFAULT_THEME;
    apply(t);
  }

  return { get, set, toggle, restore };
}
