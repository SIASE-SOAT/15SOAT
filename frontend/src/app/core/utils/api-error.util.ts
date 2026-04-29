export function extractApiError(err: unknown, fallback: string): string {
  if (err && typeof err === 'object') {
    const e = err as { error?: { message?: string }; message?: string };
    return e.error?.message ?? e.message ?? fallback;
  }
  return fallback;
}
