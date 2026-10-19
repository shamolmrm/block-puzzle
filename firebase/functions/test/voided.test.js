import { test } from 'node:test';
import assert from 'node:assert/strict';
import { revokeVoided } from '../lib/voided.js';

function fakeStore(owners) {
  const revoked = [];
  return {
    revoked,
    async ownerOfToken(token) {
      return owners[token] ?? null;
    },
    async revoke(uid, field, token) {
      revoked.push({ uid, field, token });
    },
  };
}

test('refunded purchases lose Remove ads', async () => {
  const store = fakeStore({ t1: { uid: 'alice', productId: 'remove_ads' } });

  const result = await revokeVoided({ voided: [{ purchaseToken: 't1' }], store });

  assert.deepEqual(result, { revoked: 1, unknown: 0 });
  assert.deepEqual(store.revoked, [{ uid: 'alice', field: 'removeAds', token: 't1' }]);
});

test('tokens we never verified, unknown products and junk are ignored', async () => {
  const store = fakeStore({ t2: { uid: 'bob', productId: 'something_else' } });

  const result = await revokeVoided({
    voided: [{ purchaseToken: 'never-seen' }, { purchaseToken: 't2' }, {}, null, { purchaseToken: '' }],
    store,
  });

  assert.deepEqual(result, { revoked: 0, unknown: 2 });
  assert.equal(store.revoked.length, 0);
});

test('an empty or missing list does nothing', async () => {
  const store = fakeStore({});
  assert.deepEqual(await revokeVoided({ voided: undefined, store }), { revoked: 0, unknown: 0 });
});
