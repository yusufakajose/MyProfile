import { isValidHttpUrl, sanitizeAlias, isValidAlias, aliasError, dateErrors } from '../../utils/validation';

describe('validation utils', () => {
  test('isValidHttpUrl', () => {
    expect(isValidHttpUrl('http://example.com')).toBe(true);
    expect(isValidHttpUrl('https://example.com/a')).toBe(true);
    expect(isValidHttpUrl('ftp://x')).toBe(false);
    expect(isValidHttpUrl('not a url')).toBe(false);
  });

  test('sanitizeAlias collapses and trims separators', () => {
    expect(sanitizeAlias('--a..b__c--')).toBe('a-b-c');
    expect(sanitizeAlias('  -._Hello-.- ')).toBe('Hello');
  });

  test('isValidAlias basic regex', () => {
    expect(isValidAlias('abc')).toBe(true);
    expect(isValidAlias('a_b.c-1')).toBe(true);
    expect(isValidAlias('-bad')).toBe(true); // sanitizes to 'bad'
    expect(isValidAlias('bad-')).toBe(true); // sanitizes to 'bad'
    expect(isValidAlias('a')).toBe(false);
  });

  test('aliasError messages', () => {
    expect(aliasError('a')).toMatch(/3–50/);
    expect(aliasError('-a')).toMatch(/3–50/);
    expect(aliasError('good$bad')).toMatch(/Only letters/);
    expect(aliasError('a--b')).toMatch(/Avoid repeated/);
    expect(aliasError('good')).toBe('');
  });

  test('dateErrors detects invalid ordering', () => {
    const s = '2024-01-01T10:00';
    const e = '2024-01-01T09:00';
    const errs = dateErrors(s, e);
    expect(errs.startAt).toMatch(/Start/);
    expect(errs.endAt).toMatch(/End/);
    expect(dateErrors('', '')).toEqual({ startAt: '', endAt: '' });
  });
});


