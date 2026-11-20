// Gives every budget record without a householdId the household it belongs to, before the
// household-aware API is deployed. Records written since then already have one.
//
// Dry run by default; it prints what it would change. To write:
//
//   mongosh "$MONGO_APP_URL" --eval 'const HOUSEHOLD = "<id>"; const APPLY = true' scripts/backfill-household.js
//
// Safe to run again: it only touches records with no householdId.

if (typeof HOUSEHOLD === 'undefined') throw new Error('Set HOUSEHOLD to the household id.');
const apply = typeof APPLY !== 'undefined' && APPLY === true;
const householdId = ObjectId(HOUSEHOLD);
const budget = db.getSiblingDB(typeof DATABASE !== 'undefined' ? DATABASE : 'BUDGET');

// The household must exist in household-api's database, or no one could reach the records.
const household = db.getSiblingDB('HOUSEHOLD').households.findOne({ _id: householdId, deletedAt: null });
if (!household) throw new Error(`No live household ${HOUSEHOLD} in HOUSEHOLD.households.`);

print(`${apply ? 'APPLYING' : 'DRY RUN'}: records without a household go to "${household.name}" (${HOUSEHOLD}),`);
print(`members ${household.members.map((m) => m.email).join(', ')}`);

const collections = ['Bills', 'CreditCards', 'Debts', 'HouseRepairs', 'Incomes', 'Subscriptions'];
let total = 0;
for (const name of collections) {
  const coll = budget.getCollection(name);
  const missing = { householdId: { $exists: false } };
  const count = coll.countDocuments(missing);
  if (apply && count > 0) coll.updateMany(missing, { $set: { householdId } });
  print(`  ${name.padEnd(14)} ${count} of ${coll.countDocuments()}`);
  total += count;
}
print(`${total} records ${apply ? 'updated' : 'to update'}.`);
