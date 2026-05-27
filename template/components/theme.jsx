// theme.jsx — ThemeContext, icons, shared components, utilities
const { createContext, useContext, useState, useEffect, useRef } = React;

// ── Theme ────────────────────────────────────────────────────
const ThemeCtx = createContext();
window.useTheme = () => useContext(ThemeCtx);

window.ThemeProvider = function ThemeProvider({ children }) {
  const [theme, setTheme] = useState(() => localStorage.getItem('cbd-theme') || 'dark');
  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('cbd-theme', theme);
  }, [theme]);
  const toggle = () => setTheme(t => t === 'dark' ? 'light' : 'dark');
  return React.createElement(ThemeCtx.Provider, { value: { theme, toggle } }, children);
};

// ── Utilities ────────────────────────────────────────────────
window.fmt = (v) =>
  new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(v);

window.fmtShort = (v) => {
  const abs = Math.abs(v);
  if (abs >= 1000) return (v < 0 ? '-' : '') + 'R$ ' + (abs / 1000).toFixed(1).replace('.', ',') + 'k';
  return fmt(v);
};

window.fmtDate = (d) =>
  new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: 'short' }).format(new Date(d));

window.fmtMonth = (d) =>
  new Intl.DateTimeFormat('pt-BR', { month: 'short' }).format(new Date(d)).replace('.', '');

// ── SVG Icons ────────────────────────────────────────────────
const ICONS = {
  home:        'M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z M9 22V12h6v10',
  layers:      'M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5',
  list:        'M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01',
  calendar:    'M3 4h18v18H3zM16 2v4M8 2v4M3 10h18',
  bookOpen:    'M2 3h6a4 4 0 014 4v14a3 3 0 00-3-3H2zM22 3h-6a4 4 0 00-4 4v14a3 3 0 013-3h7z',
  creditCard:  'M1 4h22v16H1zM1 10h22',
  target:      'M22 12A10 10 0 1112 2M22 12h-4M12 2v4M12 6a6 6 0 100 12M18 12a6 6 0 00-6-6',
  barChart:    'M18 20V10M12 20V4M6 20v-6',
  database:    'M12 2C6.48 2 2 4.24 2 7s4.48 5 10 5 10-2.24 10-5-4.48-5-10-5zM2 7v5c0 2.76 4.48 5 10 5s10-2.24 10-5V7M2 12v5c0 2.76 4.48 5 10 5s10-2.24 10-5v-5',
  tag:         'M20.59 13.41l-7.17 7.17a2 2 0 01-2.83 0L2 12V2h10l8.59 8.59a2 2 0 010 2.82zM7 7h.01',
  briefcase:   'M16 20H8a2 2 0 01-2-2V8h12v10a2 2 0 01-2 2zM20 8H4a2 2 0 01-2-2V5a2 2 0 012-2h16a2 2 0 012 2v1a2 2 0 01-2 2zM8 3v2M16 3v2',
  building:    'M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2zM9 22V12h6v10',
  hash:        'M4 9h16M4 15h16M10 3L8 21M16 3l-2 18',
  plus:        'M12 5v14M5 12h14',
  search:      'M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0',
  bell:        'M15 17H5a2 2 0 000 4h14a2 2 0 000-4h-4zM18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9',
  sun:         'M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42M12 17a5 5 0 100-10 5 5 0 000 10z',
  moon:        'M21 12.79A9 9 0 1111.21 3 7 7 0 0021 12.79z',
  chevronRight:'M9 18l6-6-6-6',
  chevronLeft: 'M15 18l-6-6 6-6',
  chevronDown: 'M6 9l6 6 6-6',
  chevronUp:   'M18 15l-6-6-6 6',
  x:           'M18 6L6 18M6 6l12 12',
  edit:        'M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z',
  trash:       'M3 6h18M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6M8 6V4a2 2 0 012-2h4a2 2 0 012 2v2',
  moreVertical:'M12 5h.01M12 12h.01M12 19h.01',
  filter:      'M22 3H2l8 9.46V19l4 2v-8.54L22 3z',
  download:    'M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4M7 10l5 5 5-5M12 15V3',
  arrowUp:     'M12 19V5M5 12l7-7 7 7',
  arrowDown:   'M12 5v14M19 12l-7 7-7-7',
  trendingUp:  'M23 6l-9.5 9.5-5-5L1 18',
  trendingDown:'M23 18l-9.5-9.5-5 5L1 6',
  dollarSign:  'M12 1v22M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6',
  eye:         'M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8zM12 9a3 3 0 100 6 3 3 0 000-6z',
  eyeOff:      'M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24M1 1l22 22',
  check:       'M20 6L9 17l-5-5',
  alertCircle: 'M12 22c5.52 0 10-4.48 10-10S17.52 2 12 2 2 6.48 2 12s4.48 10 10 10zM12 8v4M12 16h.01',
  settings:    'M12 15a3 3 0 100-6 3 3 0 000 6zM19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 012.83-2.83l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 014 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 010 4h-.09a1.65 1.65 0 00-1.51 1z',
  menu:        'M3 12h18M3 6h18M3 18h18',
  activity:    'M22 12h-4l-3 9L9 3l-3 9H2',
  pieChart:    'M21.21 15.89A10 10 0 118 2.83M22 12A10 10 0 0012 2v10z',
};

window.Icon = function Icon({ name, size = 18, color, strokeWidth = 1.8 }) {
  const path = ICONS[name] || ICONS.alertCircle;
  const paths = path.split('M').filter(Boolean).map((p, i) => `M${p}`);
  return React.createElement('svg', {
    width: size, height: size, viewBox: '0 0 24 24',
    fill: 'none', stroke: color || 'currentColor',
    strokeWidth, strokeLinecap: 'round', strokeLinejoin: 'round',
    style: { flexShrink: 0 }
  }, paths.map((d, i) => React.createElement('path', { key: i, d })));
};

// ── Shared UI Components ──────────────────────────────────────
window.Card = function Card({ children, style, className = '' }) {
  return React.createElement('div', {
    style: {
      background: 'var(--bg-card)',
      border: '1px solid var(--border)',
      borderRadius: 'var(--radius)',
      padding: '20px',
      ...style
    },
    className
  }, children);
};

window.Button = function Button({ children, variant = 'primary', size = 'md', onClick, style, icon, disabled }) {
  const base = {
    display: 'inline-flex', alignItems: 'center', gap: '6px',
    borderRadius: 'var(--radius-sm)', fontWeight: 600,
    fontFamily: 'var(--font)', cursor: disabled ? 'not-allowed' : 'pointer',
    opacity: disabled ? 0.5 : 1, transition: 'all var(--transition)',
    border: 'none', lineHeight: 1,
  };
  const sizes = {
    sm: { padding: '6px 12px', fontSize: '12px' },
    md: { padding: '9px 16px', fontSize: '13px' },
    lg: { padding: '12px 22px', fontSize: '14px' },
  };
  const variants = {
    primary: { background: 'var(--accent)', color: '#fff' },
    secondary: { background: 'var(--bg-hover)', color: 'var(--text-primary)', border: '1px solid var(--border)' },
    ghost: { background: 'transparent', color: 'var(--text-secondary)' },
    danger: { background: 'var(--expense-light)', color: 'var(--expense)' },
    income: { background: 'var(--income-light)', color: 'var(--income)' },
  };
  return React.createElement('button', {
    onClick, disabled,
    style: { ...base, ...sizes[size], ...variants[variant], ...style }
  },
    icon && React.createElement(Icon, { name: icon, size: size === 'sm' ? 13 : 15 }),
    children
  );
};

window.Badge = function Badge({ children, color = 'accent' }) {
  const colors = {
    accent:  { bg: 'var(--accent-light)',   text: 'var(--accent)' },
    income:  { bg: 'var(--income-light)',   text: 'var(--income)' },
    expense: { bg: 'var(--expense-light)',  text: 'var(--expense)' },
    warning: { bg: 'var(--warning-light)',  text: 'var(--warning)' },
    info:    { bg: 'var(--info-light)',     text: 'var(--info)' },
    muted:   { bg: 'var(--border)',         text: 'var(--text-secondary)' },
  };
  const c = colors[color] || colors.accent;
  return React.createElement('span', {
    style: {
      display: 'inline-flex', alignItems: 'center', gap: '4px',
      padding: '3px 8px', borderRadius: '20px',
      fontSize: '11px', fontWeight: 700,
      background: c.bg, color: c.text,
    }
  }, children);
};

window.Modal = function Modal({ open, onClose, title, children, footer }) {
  if (!open) return null;
  return React.createElement('div', {
    className: 'modal-overlay', onClick: (e) => e.target === e.currentTarget && onClose()
  },
    React.createElement('div', { className: 'modal-box fade-in' },
      React.createElement('div', { className: 'modal-header' },
        React.createElement('h3', null, title),
        React.createElement('button', { className: 'icon-btn', onClick: onClose },
          React.createElement(Icon, { name: 'x', size: 18 })
        )
      ),
      children,
      footer && React.createElement('div', { className: 'modal-footer' }, footer)
    )
  );
};

// Smooth SVG path helper
window.smoothPath = function smoothPath(pts) {
  if (!pts || pts.length < 2) return '';
  let d = `M ${pts[0].x},${pts[0].y}`;
  for (let i = 1; i < pts.length; i++) {
    const p0 = pts[Math.max(0, i - 2)];
    const p1 = pts[i - 1];
    const p2 = pts[i];
    const p3 = pts[Math.min(pts.length - 1, i + 1)];
    const cp1x = p1.x + (p2.x - p0.x) / 6;
    const cp1y = p1.y + (p2.y - p0.y) / 6;
    const cp2x = p2.x - (p3.x - p1.x) / 6;
    const cp2y = p2.y - (p3.y - p1.y) / 6;
    d += ` C ${cp1x},${cp1y} ${cp2x},${cp2y} ${p2.x},${p2.y}`;
  }
  return d;
};

// Period navigator component
window.PeriodNav = function PeriodNav({ label, onPrev, onNext }) {
  return React.createElement('div', {
    style: {
      display: 'flex', alignItems: 'center', gap: '8px',
      background: 'var(--bg-card)', border: '1px solid var(--border)',
      borderRadius: 'var(--radius-sm)', padding: '6px 12px',
    }
  },
    React.createElement('button', { className: 'icon-btn', style: { width: 24, height: 24 }, onClick: onPrev },
      React.createElement(Icon, { name: 'chevronLeft', size: 14 })
    ),
    React.createElement('span', { style: { fontSize: '13px', fontWeight: 600, minWidth: '100px', textAlign: 'center' } }, label),
    React.createElement('button', { className: 'icon-btn', style: { width: 24, height: 24 }, onClick: onNext },
      React.createElement(Icon, { name: 'chevronRight', size: 14 })
    )
  );
};

// Empty state
window.EmptyState = function EmptyState({ icon = 'activity', title, desc }) {
  return React.createElement('div', {
    style: {
      display: 'flex', flexDirection: 'column', alignItems: 'center',
      justifyContent: 'center', padding: '60px 20px', gap: '12px',
      color: 'var(--text-muted)',
    }
  },
    React.createElement('div', {
      style: {
        width: 56, height: 56, borderRadius: '50%',
        background: 'var(--bg-hover)',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
      }
    }, React.createElement(Icon, { name: icon, size: 24 })),
    React.createElement('div', { style: { textAlign: 'center' } },
      React.createElement('p', { style: { fontWeight: 600, color: 'var(--text-secondary)', marginBottom: 4 } }, title),
      desc && React.createElement('p', { style: { fontSize: '13px' } }, desc)
    )
  );
};
