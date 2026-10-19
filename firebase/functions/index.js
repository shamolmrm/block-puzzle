// Cloud Functions. Deploying needs the Blaze plan and a Play Console service account with
// "View financial data" + "Manage orders" — both are release steps that need explicit approval.
import { createHash } from 'node:crypto';
import { initializeApp } from 'firebase-admin/app';
import { FieldValue, getFirestore } from 'firebase-admin/firestore';
import { HttpsError, onCall } from 'firebase-functions/v2/https';
import { onSchedule } from 'firebase-functions/v2/scheduler';
import { androidpublisher, auth as googleAuth } from '@googleapis/androidpublisher';
import { PACKAGE_NAME, verifyPurchase as verify } from './lib/verify.js';
import { revokeVoided } from './lib/voided.js';

initializeApp();
const db = getFirestore();

function publisher() {
  const auth = new googleAuth.GoogleAuth({ scopes: ['https://www.googleapis.com/auth/androidpublisher'] });
  return androidpublisher({ version: 'v3', auth });
}

function playClient() {
  const api = publisher();
  return {
    async getProduct(productId, token) {
      const res = await api.purchases.products.get({ packageName: PACKAGE_NAME, productId, token });
      return res.data;
    },
    async acknowledge(productId, token) {
      await api.purchases.products.acknowledge({ packageName: PACKAGE_NAME, productId, token });
    },
  };
}

/** Token claims are keyed by a hash of the token (fixed length, no token stored as an id). */
const tokenDoc = (token) => db.collection('purchaseTokens').doc(createHash('sha256').update(token).digest('hex'));

const store = {
  /** Records which player a token belongs to. Returns 'claimed', 'already-mine' or 'other-user'. */
  async claimToken(token, uid, productId) {
    const ref = tokenDoc(token);
    return db.runTransaction(async (tx) => {
      const snap = await tx.get(ref);
      if (snap.exists) return snap.get('uid') === uid ? 'already-mine' : 'other-user';
      tx.set(ref, { uid, productId, claimedAt: FieldValue.serverTimestamp() });
      return 'claimed';
    });
  },
  async grant(uid, field, orderId) {
    await db.doc(`users/${uid}/entitlements/main`).set(
      { [field]: true, [`${field}OrderId`]: orderId, updatedAt: FieldValue.serverTimestamp() },
      { merge: true },
    );
  },
  async ownerOfToken(token) {
    const snap = await tokenDoc(token).get();
    return snap.exists ? { uid: snap.get('uid'), productId: snap.get('productId') } : null;
  },
  async revoke(uid, field, token) {
    await db.doc(`users/${uid}/entitlements/main`).set(
      { [field]: false, [`${field}RevokedAt`]: FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp() },
      { merge: true },
    );
    await tokenDoc(token).set({ revokedAt: FieldValue.serverTimestamp() }, { merge: true });
  },
};

export const verifyPurchase = onCall({ enforceAppCheck: true }, async (request) => {
  if (!request.auth) throw new HttpsError('unauthenticated', 'Sign-in required');
  const result = await verify({ uid: request.auth.uid, data: request.data, play: playClient(), store });
  if (!result.verified && result.reason === 'token-used') {
    throw new HttpsError('permission-denied', 'This purchase belongs to another account');
  }
  return result;
});

/**
 * Daily: revokes "Remove ads" for refunded or charged-back purchases (Play Voided Purchases API).
 * A refunded token also fails verifyPurchase (Play reports it as cancelled), so it can't be re-granted.
 */
export const revokeVoidedPurchases = onSchedule('every 24 hours', async () => {
  const cursorRef = db.doc('system/voidedPurchasesCursor');
  const cursor = (await cursorRef.get()).get('startTimeMillis');
  const startTime = String(cursor ?? Date.now() - 30 * 24 * 60 * 60 * 1000); // first run: last 30 days
  const endTime = String(Date.now());
  const api = publisher();

  let pageToken;
  do {
    const res = await api.purchases.voidedpurchases.list({ packageName: PACKAGE_NAME, startTime, endTime, token: pageToken });
    await revokeVoided({ voided: res.data.voidedPurchases, store });
    pageToken = res.data.tokenPagination?.nextPageToken;
  } while (pageToken);

  await cursorRef.set({ startTimeMillis: Number(endTime) });
});
