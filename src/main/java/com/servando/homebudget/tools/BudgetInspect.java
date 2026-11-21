package com.servando.homebudget.tools;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.servando.homebudget.encryption.EncryptingMongoListener;
import com.servando.homebudget.encryption.FieldCipher;
import org.bson.Document;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Prints budget records decrypted, for debugging. Reads only; it never writes.
 *
 * <pre>
 * BudgetInspect &lt;collection&gt; [householdId] [name]
 *
 *   Bills                          every bill
 *   Bills 6afe67e6c0a8bf150c541825 one household's bills
 *   Bills 6afe67e6c0a8bf150c541825 Rent   that household's bill named Rent (exact)
 * </pre>
 *
 * It needs MONGO_APP_URL, MONGO_DATABASE and BUDGET_ENCRYPTION_KEY, which the API's container
 * already has. See the README for running it there or from a checkout.
 */
public final class BudgetInspect {

    static final List<String> COLLECTIONS = List.of("Bills", "CreditCards", "Debts", "HouseRepairs", "Incomes",
            "Subscriptions");

    private BudgetInspect() {
    }

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 3 || !COLLECTIONS.contains(args[0])) {
            System.err.println("Usage: BudgetInspect <collection> [householdId] [name]");
            System.err.println("Collections: " + String.join(", ", COLLECTIONS));
            System.exit(2);
        }
        // The driver logs its connection at INFO to stdout, in among the records.
        if (LoggerFactory.getLogger("org.mongodb.driver") instanceof ch.qos.logback.classic.Logger driver) {
            driver.setLevel(ch.qos.logback.classic.Level.WARN);
        }
        String collection = args[0];
        FieldCipher cipher = FieldCipher.fromBase64(System.getenv("BUDGET_ENCRYPTION_KEY"));
        Document filter = new Document();
        if (args.length >= 2) {
            filter.append("householdId", new ObjectId(args[1]));
        }
        if (args.length == 3) {
            filter.append(EncryptingMongoListener.NAME_INDEX, new Binary(cipher.nameIndex(args[1], args[2])));
        }

        JsonWriterSettings json = JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).indent(true).build();
        try (MongoClient client = MongoClients.create(require("MONGO_APP_URL"))) {
            MongoCollection<Document> records = client.getDatabase(require("MONGO_DATABASE")).getCollection(collection);
            int count = 0;
            for (Document record : records.find(filter)) {
                record.remove(EncryptingMongoListener.NAME_INDEX);
                record.remove("_class");
                System.out.println(cipher.decryptDocument(record, collection).toJson(json));
                count++;
            }
            System.err.println(count + " " + collection + " record" + (count == 1 ? "" : "s"));
        }
    }

    private static String require(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Set " + name + ".");
        }
        return value;
    }
}
