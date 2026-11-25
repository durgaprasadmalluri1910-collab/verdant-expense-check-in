const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write any finance collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("budgets").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("bills").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("tx_1")
      .set({
        userId: ALICE_UID,
        type: "EXPENSE",
        amountCents: 1250,
        title: "Coffee",
        category: "Dining",
        occurredAt: now,
        createdAt: now,
        updatedAt: now,
      })
  );
});

test("Owner can list empty subcollections and create/update/delete valid transactions", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const txCol = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("transactions");
  const created = new Date(Date.now() - 10000);
  const updated = new Date(Date.now() - 1000);

  // Listing empty subcollection must succeed
  await assertSucceeds(txCol.get());

  // Valid create payload (no @DocumentId `id` field, optional note omitted)
  await assertSucceeds(
    txCol.doc("tx_1").set({
      userId: ALICE_UID,
      type: "EXPENSE",
      amountCents: 4599,
      title: "Whole Foods Market",
      category: "Groceries",
      occurredAt: created,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Valid create with optional note and recurringBillId
  await assertSucceeds(
    txCol.doc("tx_2").set({
      userId: ALICE_UID,
      type: "INCOME",
      amountCents: 250000,
      title: "Payroll Deposit",
      category: "Salary",
      note: "Bi-weekly paycheck",
      recurringBillId: "bill_1",
      occurredAt: created,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Valid partial update (preserves createdAt and userId)
  await assertSucceeds(
    txCol.doc("tx_1").update({
      title: "Whole Foods Organic",
      amountCents: 4850,
      note: "Weekly produce",
      updatedAt: updated,
    })
  );

  // Reject `@DocumentId` id field in body
  await assertFails(
    txCol.doc("tx_3").set({
      id: "tx_3",
      userId: ALICE_UID,
      type: "EXPENSE",
      amountCents: 1000,
      title: "Bad ID Field",
      category: "Other",
      occurredAt: created,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    txCol.doc("tx_1").update({
      createdAt: updated,
      updatedAt: updated,
    })
  );

  // Reject negative or zero amountCents
  await assertFails(
    txCol.doc("tx_4").set({
      userId: ALICE_UID,
      type: "EXPENSE",
      amountCents: 0,
      title: "Zero",
      category: "Other",
      occurredAt: created,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject ghost field (shadow update)
  await assertFails(
    txCol.doc("tx_1").update({
      isAdminVerified: true,
      updatedAt: updated,
    })
  );

  // Reject tampered future createdAt
  await assertFails(
    txCol.doc("tx_5").set({
      userId: ALICE_UID,
      type: "EXPENSE",
      amountCents: 999,
      title: "Future CreatedAt",
      category: "Other",
      occurredAt: created,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: created,
    })
  );

  // Valid delete
  await assertSucceeds(txCol.doc("tx_1").delete());
});

test("Cross-user isolation blocks Bob from reading or modifying Alice's data", async () => {
  const created = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const aliceRoot = context.firestore().collection("users").doc(ALICE_UID);
    await aliceRoot.collection("transactions").doc("tx_alice").set({
      userId: ALICE_UID,
      type: "EXPENSE",
      amountCents: 2000,
      title: "Alice Lunch",
      category: "Dining",
      occurredAt: created,
      createdAt: created,
      updatedAt: created,
    });
    await aliceRoot.collection("budgets").doc("bud_alice").set({
      userId: ALICE_UID,
      category: "Dining",
      limitCents: 30000,
      createdAt: created,
      updatedAt: created,
    });
    await aliceRoot.collection("bills").doc("bill_alice").set({
      userId: ALICE_UID,
      title: "Internet",
      amountCents: 6500,
      category: "Utilities",
      cadence: "MONTHLY",
      nextDueAt: new Date(Date.now() + 86400000 * 10),
      autoPay: true,
      active: true,
      createdAt: created,
      updatedAt: created,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceRootAsBob = bobDb.collection("users").doc(ALICE_UID);

  await assertFails(aliceRootAsBob.collection("transactions").get());
  await assertFails(aliceRootAsBob.collection("transactions").doc("tx_alice").get());
  await assertFails(
    aliceRootAsBob.collection("transactions").doc("tx_alice").update({
      title: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(aliceRootAsBob.collection("budgets").doc("bud_alice").delete());
  await assertFails(aliceRootAsBob.collection("bills").get());

  // Bob cannot spoof Alice's userId under his own path
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).collection("transactions").doc("spoof").set({
      userId: ALICE_UID,
      type: "EXPENSE",
      amountCents: 500,
      title: "Spoofed",
      category: "Other",
      occurredAt: created,
      createdAt: created,
      updatedAt: created,
    })
  );
});

test("Budgets, recurring bills, atomic bill payment batch, and preferences succeed with valid payloads", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const aliceRoot = aliceDb.collection("users").doc(ALICE_UID);
  const created = new Date(Date.now() - 10000);
  const updated = new Date(Date.now() - 1000);
  const nextDue = new Date(Date.now() + 86400000 * 30);

  // Category Budget CRUD
  await assertSucceeds(
    aliceRoot.collection("budgets").doc("Dining").set({
      userId: ALICE_UID,
      category: "Dining",
      limitCents: 40000,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    aliceRoot.collection("budgets").doc("Dining").update({
      limitCents: 45000,
      updatedAt: updated,
    })
  );

  // Recurring Bill CRUD + Atomic Batch Payment
  await assertSucceeds(
    aliceRoot.collection("bills").doc("bill_spotify").set({
      userId: ALICE_UID,
      title: "Music Stream",
      amountCents: 1199,
      category: "Subscriptions",
      cadence: "MONTHLY",
      nextDueAt: updated,
      autoPay: false,
      active: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  const batch = aliceDb.batch();
  const paidTxRef = aliceRoot.collection("transactions").doc("tx_paid_bill");
  const billRef = aliceRoot.collection("bills").doc("bill_spotify");
  batch.set(paidTxRef, {
    userId: ALICE_UID,
    type: "EXPENSE",
    amountCents: 1199,
    title: "Music Stream",
    category: "Subscriptions",
    note: "Recurring bill payment",
    recurringBillId: "bill_spotify",
    occurredAt: updated,
    createdAt: updated,
    updatedAt: updated,
  });
  batch.update(billRef, {
    nextDueAt: nextDue,
    updatedAt: updated,
  });
  await assertSucceeds(batch.commit());

  // Preferences CRUD
  await assertSucceeds(
    aliceRoot.collection("preferences").doc("profile").set({
      userId: ALICE_UID,
      currencyCode: "USD",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    aliceRoot.collection("preferences").doc("profile").update({
      currencyCode: "EUR",
      updatedAt: updated,
    })
  );
  await assertFails(
    aliceRoot.collection("preferences").doc("profile").update({
      currencyCode: "INVALID",
      updatedAt: updated,
    })
  );
});
