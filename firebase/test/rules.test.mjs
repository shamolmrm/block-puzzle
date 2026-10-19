// Firestore rules tests against the local emulator (demo project; nothing reaches real Firebase).
import { after, before, beforeEach, test } from 'node:test';
import { readFileSync } from 'node:fs';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import { deleteDoc, doc, getDoc, serverTimestamp, setDoc, Timestamp } from 'firebase/firestore';

let env;

const progress = (overrides = {}) => ({
  schemaVersion: 1,
  levels: { L0001: { s: 3, b: 90 } },
  daily: { 20745: { s: 2, b: 120 } },
  achievements: { first_game: 1 },
  bests: { best_classic_score: 800, best_combo: 3 },
  counters: { 'device-0001': { xp: 300, lines_cleared: 50 } },
  updatedAt: serverTimestamp(),
  ...overrides,
});

const mine = (uid) => env.authenticatedContext(uid).firestore();

before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-pbb',
    firestore: { rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') },
  });
});

beforeEach(async () => env.clearFirestore());
after(async () => env.cleanup());

test('a player can create, read, update and delete their own progress', async () => {
  const db = mine('alice');
  const ref = doc(db, 'users/alice/progress/main');
  await assertSucceeds(setDoc(ref, progress()));
  await assertSucceeds(getDoc(ref));
  await assertSucceeds(setDoc(ref, progress({ levels: { L0001: { s: 3, b: 95 }, L0002: { s: 1, b: 10 } } })));
  await assertSucceeds(deleteDoc(ref));
});

test("nobody can read or write another player's progress", async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'users/alice/progress/main'), progress({ updatedAt: Timestamp.now() }));
  });
  const bob = mine('bob');
  await assertFails(getDoc(doc(bob, 'users/alice/progress/main')));
  await assertFails(setDoc(doc(bob, 'users/alice/progress/main'), progress()));
  await assertFails(deleteDoc(doc(bob, 'users/alice/progress/main')));
});

test('signed-out clients get nothing', async () => {
  const anon = env.unauthenticatedContext().firestore();
  await assertFails(getDoc(doc(anon, 'users/alice/progress/main')));
  await assertFails(setDoc(doc(anon, 'users/alice/progress/main'), progress()));
});

test('only the main progress document exists', async () => {
  const db = mine('alice');
  await assertFails(setDoc(doc(db, 'users/alice/progress/other'), progress()));
  await assertFails(setDoc(doc(db, 'users/alice'), { name: 'x' }));
  await assertFails(setDoc(doc(db, 'leaderboard/alice'), { score: 999999 }));
  await assertFails(getDoc(doc(db, 'leaderboard/alice')));
});

test('malformed or forged documents are rejected', async () => {
  const ref = doc(mine('alice'), 'users/alice/progress/main');
  await assertFails(setDoc(ref, progress({ schemaVersion: 2 })));
  await assertFails(setDoc(ref, progress({ updatedAt: Timestamp.fromMillis(0) })));
  await assertFails(setDoc(ref, progress({ coins: 1000000 })));
  await assertFails(setDoc(ref, progress({ levels: 'all of them' })));
  await assertFails(setDoc(ref, progress({ bests: { best_classic_score: 1, premium: 1 } })));
  const { counters, ...missingCounters } = progress();
  await assertFails(setDoc(ref, missingCounters));
});

test('players can read but never write their paid entitlements', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'users/alice/entitlements/main'), { removeAds: true });
  });
  const alice = mine('alice');
  await assertSucceeds(getDoc(doc(alice, 'users/alice/entitlements/main')));
  await assertFails(setDoc(doc(alice, 'users/alice/entitlements/main'), { removeAds: true }));
  await assertFails(setDoc(doc(mine('mallory'), 'users/mallory/entitlements/main'), { removeAds: true }));
  await assertFails(getDoc(doc(mine('mallory'), 'users/alice/entitlements/main')));
  await assertFails(getDoc(doc(alice, 'purchaseTokens/some-token')));
  await assertFails(setDoc(doc(alice, 'purchaseTokens/some-token'), { uid: 'alice' }));
});

test('oversized maps are rejected', async () => {
  const ref = doc(mine('alice'), 'users/alice/progress/main');
  const devices = Object.fromEntries(Array.from({ length: 21 }, (_, i) => [`device-${i}`, { xp: 1 }]));
  await assertFails(setDoc(ref, progress({ counters: devices })));
});
