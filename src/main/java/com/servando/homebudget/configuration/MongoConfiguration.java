package com.servando.homebudget.configuration;

import com.mongodb.client.MongoClient;
import com.servando.homebudget.encryption.EncryptingMongoListener;
import com.servando.homebudget.encryption.EncryptionMigration;
import com.servando.homebudget.models.database.BillsModel;
import com.servando.homebudget.models.database.CreditCardsModel;
import com.servando.homebudget.models.database.DebtsModel;
import com.servando.homebudget.models.database.HouseRepairsModel;
import com.servando.homebudget.models.database.IncomesModel;
import com.servando.homebudget.models.database.SharedProperties;
import com.servando.homebudget.models.database.SubscriptionsModel;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.domain.Sort;

import java.util.List;

@Configuration
public class MongoConfiguration {

    @Value("${spring.data.mongodb.uri}")
    private String uri;

    @Value("${spring.data.mongodb.database}")
    private String databaseName;

    @Bean
    public MongoClient mongoClient() {
        return MongoClients.create(uri);
    }

    @Bean
    public MongoDatabaseFactory mongoDatabaseFactory() {
        return new SimpleMongoClientDatabaseFactory(mongoClient(), databaseName);
    }

    @Bean
    public MongoTemplate mongoTemplate() throws Exception {
        return new MongoTemplate(mongoClient(), databaseName);
    }

    /** Every record type: the purge clears them all, and each gets the per-household name index. */
    public static final List<Class<? extends SharedProperties>> MODELS = List.of(BillsModel.class,
            CreditCardsModel.class, DebtsModel.class, HouseRepairsModel.class, IncomesModel.class,
            SubscriptionsModel.class);

    static final String OLD_NAME_INDEX = "householdId_name_unique";
    static final String NAME_INDEX = "householdId_nameIndex_unique";

    /**
     * At startup: first encrypt the records saved before encryption, which gives each one a
     * nameIndex; then make names unique within a household on that hash (the names themselves
     * are encrypted, so an index on them can't compare). Not through auto-index creation (which is
     * off), so an index that can't be built stops the deploy loudly. A record without a householdId
     * counts as householdId null, so backfill before deploying. Tests without a database turn it
     * off with app.mongo.create-indexes=false.
     */
    @Bean
    @ConditionalOnProperty(name = "app.mongo.create-indexes", matchIfMissing = true)
    SmartInitializingSingleton indexInitializer(MongoTemplate mongo) {
        return () -> {
            EncryptionMigration.run(mongo, MODELS);
            for (Class<? extends SharedProperties> model : MODELS) {
                var indexes = mongo.indexOps(model);
                if (indexes.getIndexInfo().stream().anyMatch(i -> i.getName().equals(OLD_NAME_INDEX))) {
                    indexes.dropIndex(OLD_NAME_INDEX);
                }
                indexes.createIndex(new Index().on("householdId", Sort.Direction.ASC)
                        .on(EncryptingMongoListener.NAME_INDEX, Sort.Direction.ASC)
                        .unique()
                        .named(NAME_INDEX));
            }
        };
    }
}
