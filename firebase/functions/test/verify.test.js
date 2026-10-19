import { test } from 'node:test';
import assert from 'node:assert/strict';
import { accountTagFor, verifyPurchase } from '../lib/verify.js';

const TOKEN = 'token-abcdefghijklmnop';

function fakes({ purchase, getError, claimed } = {}) {
  const calls = { acknowledged: 0, grants: [] };
  const play = {
    async getProduct() {
      if (getError) throw getError;
      return purchase ?? { purchaseState: 0, acknowledgementState: 0, orderId: 'GPA.1', obfuscatedExternalAccountId: accountTagFor('alice') };
    },
    async acknowledge() {
      calls.acknowledged++;
    },
  };
  const tokens = new Map(claimed ?? []);
  const store = {
    async claimToken(token, uid) {
      if (tokens.has(token)) return tokens.get(token) === uid ? 'already-mine' : 'other-user';
      tokens.set(token, uid);
      return 'claimed';
    },
    async grant(uid, field, orderId) {
      calls.grants.push({ uid, field, orderId });
    },
  };
  return { play, store, calls };
}

const data = { productId: 'remove_ads', purchaseToken: TOKEN };

test('a real, completed purchase grants Remove ads and is acknowledged server-side', async () => {
  const { play, store, calls } = fakes();
  assert.deepEqual(await verifyPurchase({ uid: 'alice', data, play, store }), { verified: true });
  assert.equal(calls.acknowledged, 1);
  assert.deepEqual(calls.grants, [{ uid: 'alice', field: 'removeAds', orderId: 'GPA.1' }]);
});

test('re-verifying (restore) is idempotent and does not re-acknowledge', async () => {
  const { play, store, calls } = fakes({
    purchase: { purchaseState: 0, acknowledgementState: 1, orderId: 'GPA.1' },
    claimed: [[TOKEN, 'alice']],
  });
  assert.deepEqual(await verifyPurchase({ uid: 'alice', data, play, store }), { verified: true });
  assert.equal(calls.acknowledged, 0);
});

test('nothing is granted without sign-in, for unknown products or bad tokens', async () => {
  const { play, store, calls } = fakes();
  assert.equal((await verifyPurchase({ uid: undefined, data, play, store })).reason, 'unauthenticated');
  assert.equal((await verifyPurchase({ uid: 'alice', data: { ...data, productId: 'coins_9999' }, play, store })).reason, 'unknown-product');
  assert.equal((await verifyPurchase({ uid: 'alice', data: { ...data, productId: '__proto__' }, play, store })).reason, 'unknown-product');
  assert.equal((await verifyPurchase({ uid: 'alice', data: { ...data, purchaseToken: 'x' }, play, store })).reason, 'bad-token');
  assert.equal(calls.grants.length, 0);
});

test('pending, cancelled or unknown purchases are not granted', async () => {
  for (const purchaseState of [1, 2]) {
    const { play, store, calls } = fakes({ purchase: { purchaseState, acknowledgementState: 0 } });
    assert.equal((await verifyPurchase({ uid: 'alice', data, play, store })).reason, 'not-purchased');
    assert.equal(calls.grants.length, 0);
  }
  const notFound = fakes({ getError: Object.assign(new Error('nope'), { code: 404 }) });
  assert.equal((await verifyPurchase({ uid: 'alice', data, ...notFound })).reason, 'not-found');
});

test("someone else's token is refused", async () => {
  const tagged = fakes(); // tagged for alice
  assert.equal((await verifyPurchase({ uid: 'mallory', data, ...tagged })).reason, 'account-mismatch');

  const shared = fakes({ purchase: { purchaseState: 0, acknowledgementState: 1 }, claimed: [[TOKEN, 'alice']] });
  assert.equal((await verifyPurchase({ uid: 'mallory', data, ...shared })).reason, 'token-used');
  assert.equal(shared.calls.grants.length, 0);
});

test('transient Play API errors propagate so the app retries later', async () => {
  const { play, store } = fakes({ getError: Object.assign(new Error('backend'), { code: 503 }) });
  await assert.rejects(verifyPurchase({ uid: 'alice', data, play, store }));
});
