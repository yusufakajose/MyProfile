// Shared frontend validation helpers

export const isValidHttpUrl = (s) => {
  try {
    const u = new URL(s);
    return u.protocol === 'http:' || u.protocol === 'https:';
  } catch {
    return false;
  }
};

export const sanitizeAlias = (alias) => {
  if (!alias) return '';
  let a = alias.trim();
  a = a.replace(/[._-]{2,}/g, '-');
  a = a.replace(/^[._-]+|[._-]+$/g, '');
  return a;
};

export const isValidAlias = (s) => {
  if (!s) return true;
  const a = sanitizeAlias(s);
  return /^[A-Za-z0-9](?:[A-Za-z0-9-_.]{1,48}[A-Za-z0-9])$/.test(a);
};

export const aliasError = (s) => {
  const raw = (s || '').trim();
  if (!raw) return '';
  const a = sanitizeAlias(raw);
  if (a.length < 3 || a.length > 50) return 'Alias must be 3–50 characters';
  if (!/^[A-Za-z0-9].*[A-Za-z0-9]$/.test(a)) return 'Alias must start/end with a letter or number';
  if (/[^A-Za-z0-9._-]/.test(raw)) return 'Only letters, numbers, -, _, . allowed';
  if (/^[._-]|[._-]$/.test(raw)) return 'No leading/trailing separators';
  if (/[._-]{2,}/.test(raw)) return 'Avoid repeated separators';
  return '';
};

export const dateErrors = (startAt, endAt) => {
  if (!startAt || !endAt) return { startAt: '', endAt: '' };
  try {
    const s = new Date(startAt);
    const e = new Date(endAt);
    if (isFinite(s.getTime()) && isFinite(e.getTime()) && s >= e) {
      return { startAt: 'Start must be before End', endAt: 'End must be after Start' };
    }
  } catch {}
  return { startAt: '', endAt: '' };
};


