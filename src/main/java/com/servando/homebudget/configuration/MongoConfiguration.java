package com.servando.homebudget.configuration;

import com.mongodb.client.MongoClient;
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

    /**
     * Names are unique within a household. Created at startup rather than through auto-index
     * creation (which is off), so an index that can't be built stops the deploy loudly. A record
     * without a householdId counts as householdId null, so backfill before deploying. Tests without
     * a database turn it off with app.mongo.create-indexes=false.
     */
    @Bean
    @ConditionalOnProperty(name = "app.mongo.create-indexes", matchIfMissing = true)
    SmartInitializingSingleton indexInitializer(MongoTemplate mongo) {
        return () -> {
            for (Class<? extends SharedProperties> model : MODELS) {
                mongo.indexOps(model).createIndex(new Index().on("householdId", Sort.Direction.ASC)
                        .on("name", Sort.Direction.ASC)
                        .unique()
                        .named("householdId_name_unique"));
            }
        };
    }
}
