// Cloud Functions. Deploying needs the Blaze plan and a Play Console service account with
// "View financial data" + "Manage orders" — both are release steps that need explicit approval.
import { initializeApp } from 'firebase-admin/app';
import { FieldValue, getFirestore } from 'firebase-admin/firestore';
import { HttpsError, onCall } from 'firebase-functions/v2/https';
import { androidpublisher, auth as googleAuth } from '@googleapis/androidpublisher';
import { PACKAGE_NAME, verifyPurchase as verify } from './lib/verify.js';

initializeApp();
const db = getFirestore();

function playClient() {
  const auth = new googleAuth.GoogleAuth({ scopes: ['https://www.googleapis.com/auth/androidpublisher'] });
  const api = androidpublisher({ version: 'v3', auth });
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

const store = {
  /** Records which player a token belongs to. Returns 'claimed', 'already-mine' or 'other-user'. */
  async claimToken(token, uid, productId) {
    const ref = db.collection('purchaseTokens').doc(Buffer.from(token).toString('base64url').slice(0, 1400));
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
};

export const verifyPurchase = onCall({ enforceAppCheck: true }, async (request) => {
  if (!request.auth) throw new HttpsError('unauthenticated', 'Sign-in required');
  const result = await verify({ uid: request.auth.uid, data: request.data, play: playClient(), store });
  if (!result.verified && result.reason === 'token-used') {
    throw new HttpsError('permission-denied', 'This purchase belongs to another account');
  }
  return result;
});
