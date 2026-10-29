// Purchase verification rules, free of Firebase/Google clients so they can be unit-tested.
// The app never grants a paid entitlement itself: only this logic, running on the server, does.
import { createHash } from 'node:crypto';

export const PACKAGE_NAME = 'com.bahalabs.blockfit';

/** Product id -> entitlement field it grants. Must match the app's `Products`. */
export const PRODUCTS = { remove_ads: 'removeAds' };

/** Play purchaseState values (Android Publisher API, purchases.products). */
const PURCHASED = 0;
const ACK_PENDING = 0;

/** Same as the app's CallablePurchaseVerifier.accountTagFor: SHA-256 hex of the Firebase uid. */
export function accountTagFor(uid) {
  return createHash('sha256').update(uid).digest('hex');
}

/**
 * @param {object} args
 * @param {string|undefined} args.uid       Firebase Auth uid of the caller (from the callable context)
 * @param {object} args.data                { productId, purchaseToken } from the app — untrusted
 * @param {object} args.play                { getProduct(productId, token), acknowledge(productId, token) }
 * @param {object} args.store               { claimToken(token, uid, productId), grant(uid, field, orderId) }
 * @returns {Promise<{verified: boolean, reason?: string}>}
 */
export async function verifyPurchase({ uid, data, play, store }) {
  if (!uid) return { verified: false, reason: 'unauthenticated' };
  const productId = data?.productId;
  const token = data?.purchaseToken;
  const field = Object.hasOwn(PRODUCTS, productId) ? PRODUCTS[productId] : undefined;
  if (!field) return { verified: false, reason: 'unknown-product' };
  if (typeof token !== 'string' || token.length < 10 || token.length > 4096) {
    return { verified: false, reason: 'bad-token' };
  }

  // Ask Google Play, not the app, whether this purchase is real and completed.
  let purchase;
  try {
    purchase = await play.getProduct(productId, token);
  } catch (e) {
    if (e?.code === 404 || e?.code === 400 || e?.code === 410) return { verified: false, reason: 'not-found' };
    throw e; // transient: the app retries later
  }
  if (purchase?.purchaseState !== PURCHASED) return { verified: false, reason: 'not-purchased' };

  // Purchases made from the app carry the player's account tag; a mismatch means someone else's token.
  const tag = purchase.obfuscatedExternalAccountId;
  if (tag && tag !== accountTagFor(uid)) return { verified: false, reason: 'account-mismatch' };

  // One token, one player: blocks sharing a purchase token across accounts.
  const claim = await store.claimToken(token, uid, productId);
  if (claim === 'other-user') return { verified: false, reason: 'token-used' };

  // Acknowledge server-side, after verifying (unacknowledged purchases are refunded after 3 days).
  if (purchase.acknowledgementState === ACK_PENDING) await play.acknowledge(productId, token);

  await store.grant(uid, field, purchase.orderId ?? null);
  return { verified: true };
}
