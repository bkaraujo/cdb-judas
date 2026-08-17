/** Usage-percent → CSS variable token, shared by CreditCard.barColorByUsage and Budget.barColor
 * (both were the exact same 80/60 threshold, defined twice). */
export function thresholdColorToken(pct: number): string {
  if (pct >= 80) return 'expense';
  if (pct >= 60) return 'warning';
  return 'accent';
}

export interface ProgressBarOptions {
  size?: 'sm' | 'md';
  marginBottom?: string;
}

/** `color` is a resolved CSS color (e.g. 'var(--expense)'), not a bare token — callers that only
 * have a percent should resolve it via thresholdColorToken first. `opts.size` 'sm' (6px/3px,
 * dashboard panels) vs the default 'md' (8px/4px, credit-cards/budget). */
export function progressBarHtml(pct: number, color: string, opts: ProgressBarOptions = {}): string {
  const sm = opts.size === 'sm';
  const mb = opts.marginBottom ? ' style="margin-bottom:' + opts.marginBottom + ';"' : '';
  return (
    '<div class="progress' + (sm ? ' progress-sm' : '') + '"' + mb + '>' +
    '<div class="progress-fill" style="width:' + pct + '%;background:' + color + ';"></div>' +
    '</div>'
  );
}
