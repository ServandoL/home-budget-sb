package com.servando.homebudget.encryption;

import com.servando.homebudget.models.database.SharedProperties;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Rewrites the records saved before encryption: each one is read and saved again, which encrypts
 * it and gives it a nameIndex. A record whose name isn't a plain string is done already, so this
 * only does work once. Each save replaces the whole document, so a stop halfway leaves every
 * record either fully old or fully encrypted.
 */
public final class EncryptionMigration {

    private static final Logger log = LoggerFactory.getLogger(EncryptionMigration.class);

    private EncryptionMigration() {
    }

    public static long run(MongoTemplate mongo, List<Class<? extends SharedProperties>> models) {
        long total = 0;
        for (Class<? extends SharedProperties> model : models) {
            String collection = mongo.getCollectionName(model);
            // Read them all first, so the cursor can't hand back a record already rewritten.
            List<Document> plain = mongo.getCollection(collection)
                    .find(new Document("name", new Document("$type", "string")))
                    .into(new ArrayList<>());
            for (Document stored : plain) {
                mongo.save(mongo.getConverter().read(model, stored), collection);
            }
            long count = plain.size();
            if (count > 0) {
                log.info("Encrypted {} {} records saved before encryption", count, collection);
            }
            total += count;
        }
        return total;
    }
}
