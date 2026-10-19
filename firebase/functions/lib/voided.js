// Revokes entitlements for refunded / charged-back purchases (Google Play Voided Purchases API).
// Pure logic so it can be unit-tested; index.js wires in the real Play and Firestore clients.
import { PRODUCTS } from './verify.js';

/**
 * @param {object} args
 * @param {Array<{purchaseToken: string, voidedTimeMillis?: string}>} args.voided  from purchases.voidedpurchases.list
 * @param {object} args.store  { ownerOfToken(token) -> {uid, productId} | null, revoke(uid, field, token) }
 * @returns {Promise<{revoked: number, unknown: number}>}
 */
export async function revokeVoided({ voided, store }) {
  let revoked = 0;
  let unknown = 0;
  for (const purchase of voided ?? []) {
    const token = purchase?.purchaseToken;
    if (typeof token !== 'string' || token.length === 0) continue;
    const owner = await store.ownerOfToken(token);
    const field = owner && Object.hasOwn(PRODUCTS, owner.productId) ? PRODUCTS[owner.productId] : undefined;
    if (!owner || !field) {
      unknown++; // never verified through us (or a product we don't sell): nothing to revoke
      continue;
    }
    await store.revoke(owner.uid, field, token);
    revoked++;
  }
  return { revoked, unknown };
}
