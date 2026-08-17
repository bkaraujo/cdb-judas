/** Thin wrapper over $(document) trigger/on. */
import $ from 'jquery';

export interface CbdChangeDetail {
  type: string;
  action: 'upsert' | 'delete';
  payload?: unknown;
  id?: unknown;
}

export interface EventMap {
  'cbd:change': CbdChangeDetail;
}

export type EventHandler<K extends keyof EventMap> = (evt: JQuery.TriggeredEvent, detail: EventMap[K]) => void;

export interface EventBus {
  emit<K extends keyof EventMap>(name: K, detail: EventMap[K]): void;
  on<K extends keyof EventMap>(name: K, cb: EventHandler<K>): void;
  off<K extends keyof EventMap>(name: K, cb: EventHandler<K>): void;
}

export function createEventBus(): EventBus {
  return {
    emit(name, detail) {
      try {
        $(document).trigger(name, [detail]);
      } catch {
        /* noop */
      }
    },
    on(name, cb) {
      $(document).on(name, cb as (...args: unknown[]) => void);
    },
    off(name, cb) {
      $(document).off(name, cb as (...args: unknown[]) => void);
    },
  };
}
