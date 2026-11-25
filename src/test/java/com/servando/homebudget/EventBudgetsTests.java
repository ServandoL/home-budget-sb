package com.servando.homebudget;

import com.home.household.HouseholdFilter;
import com.jayway.jsonpath.JsonPath;
import com.mongodb.client.MongoClient;
import com.servando.homebudget.models.dto.AddEventPurchaseDto;
import com.servando.homebudget.services.BudgetPurger;
import com.servando.homebudget.services.EventBudgetsServiceImpl;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Event budgets against a real MongoDB: purchases, done and delete, per household, encrypted. */
@SpringBootTest(properties = {"MONGO_DATABASE=BUDGET", "SERVER_PORT=0", "household.database=HOUSEHOLD",
        "access.team-domain=test.cloudflareaccess.com", "access.aud=test-aud",
        "budget.encryption-key=" + HouseholdScopingTests.KEY})
@AutoConfigureMockMvc
@Testcontainers
class EventBudgetsTests {

    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:8.0");

    @DynamicPropertySource
    static void mongo(DynamicPropertyRegistry registry) {
        registry.add("MONGO_APP_URL", MONGO::getConnectionString);
    }

    static final String SAM = "sam@example.com";
    static final String KIM = "kim@example.com";

    @Autowired
    MockMvc mvc;

    @Autowired
    MongoClient client;

    @Autowired
    MongoTemplate mongo;

    @Autowired
    CacheManager caches;

    @Autowired
    EventBudgetsServiceImpl service;

    @Autowired
    BudgetPurger purger;

    String samHome;
    String kimHome;

    @BeforeEach
    void households() {
        client.getDatabase("HOUSEHOLD").getCollection("households").deleteMany(new Document());
        for (String collection : mongo.getCollectionNames()) {
            mongo.getCollection(collection).deleteMany(new Document());
        }
        caches.getCacheNames().forEach(name -> caches.getCache(name).clear());
        samHome = household(SAM);
        kimHome = household(KIM);
    }

    @Test
    void purchasesCountAgainstTheBudgetUntilTheEventIsDeleted() throws Exception {
        String con = createEvent(SAM, samHome, "Comic Con", 500).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.spent").value(0.0))
                .andExpect(jsonPath("$.data.doneAt").doesNotExist())
                .andExpect(jsonPath("$.data.version").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(con, "$.data.id");

        addPurchase(SAM, samHome, id, "Prop replica", 120).andExpect(status().isCreated());
        String body = addPurchase(SAM, samHome, id, "Lunch", 28.555).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.purchases[*].name", contains("Lunch", "Prop replica")))
                .andExpect(jsonPath("$.data.purchases[0].amount").value(28.56))
                .andExpect(jsonPath("$.data.spent").value(148.56))
                .andReturn().getResponse().getContentAsString();
        String lunch = JsonPath.read(body, "$.data.purchases[0].id");

        mvc.perform(delete("/api/v1/event-budgets/" + id + "/purchases/" + lunch).with(as(SAM, samHome)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.purchases[*].name", contains("Prop replica")));

        mvc.perform(json(put("/api/v1/event-budgets/" + id), "{\"name\":\"Comic Con\",\"done\":true}").with(as(SAM, samHome)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.doneAt").isNotEmpty())
                .andExpect(jsonPath("$.data.budget").value(500.0));
        events(SAM, samHome).andExpect(jsonPath("$.data[0].doneAt").isNotEmpty())
                .andExpect(jsonPath("$.data[0].purchases", hasSize(1)));

        mvc.perform(json(put("/api/v1/event-budgets/" + id), "{\"name\":\"Comic Con\",\"done\":false}").with(as(SAM, samHome)))
                .andExpect(jsonPath("$.data.doneAt").doesNotExist());

        mvc.perform(delete("/api/v1/event-budgets/" + id).with(as(SAM, samHome))).andExpect(status().isOk());
        events(SAM, samHome).andExpect(jsonPath("$.data", empty()));
    }

    @Test
    void itemsAndAmountsAreUnreadableInTheDatabase() throws Exception {
        String id = JsonPath.read(createEvent(SAM, samHome, "Comic Con", 500).andReturn().getResponse().getContentAsString(),
                "$.data.id");
        addPurchase(SAM, samHome, id, "Signed comic", 40).andExpect(status().isCreated());

        Document stored = mongo.getCollection("EventBudgets").find().first();
        assertThat(stored.get("purchases")).isInstanceOf(Binary.class);
        assertThat(stored.get("budget")).isInstanceOf(Binary.class);
        assertThat(stored.toString()).doesNotContain("Signed comic", "Comic Con");
    }

    @Test
    void anotherHouseholdCantSeeOrAddToAnEvent() throws Exception {
        String id = JsonPath.read(createEvent(SAM, samHome, "Comic Con", 500).andReturn().getResponse().getContentAsString(),
                "$.data.id");

        events(KIM, kimHome).andExpect(jsonPath("$.data", empty()));
        addPurchase(KIM, kimHome, id, "Hoodie", 39).andExpect(status().isNotFound());
        mvc.perform(json(put("/api/v1/event-budgets/" + id), "{\"name\":\"Mine\",\"done\":true}").with(as(KIM, kimHome)))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/event-budgets/" + id).with(as(KIM, kimHome)));

        events(SAM, samHome).andExpect(jsonPath("$.data[0].name").value("Comic Con"))
                .andExpect(jsonPath("$.data[0].purchases", empty()));
    }

    @Test
    void purchasesAddedAtOnceAreAllKept() throws Exception {
        String id = JsonPath.read(createEvent(SAM, samHome, "Farmers market", 80).andReturn().getResponse().getContentAsString(),
                "$.data.id");

        try (var pool = Executors.newFixedThreadPool(2)) {
            List<Future<?>> adds = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                String item = "Item " + i;
                adds.add(pool.submit(() -> service.addPurchase(samHome, id, new AddEventPurchaseDto(item, 5.0))));
            }
            for (Future<?> add : adds) {
                add.get();
            }
        }

        events(SAM, samHome).andExpect(jsonPath("$.data[0].purchases", hasSize(2)))
                .andExpect(jsonPath("$.data[0].spent").value(10.0));
    }

    @Test
    void aBudgetAndAPurchaseHaveToBeMoreThanZero() throws Exception {
        createEvent(SAM, samHome, "Free day", 0).andExpect(status().isBadRequest());
        String id = JsonPath.read(createEvent(SAM, samHome, "Fair", 50).andReturn().getResponse().getContentAsString(),
                "$.data.id");
        addPurchase(SAM, samHome, id, "Refund", -5).andExpect(status().isBadRequest());
        addPurchase(SAM, samHome, id, " ", 5).andExpect(status().isBadRequest());
        createEvent(SAM, samHome, "Fair", 20).andExpect(status().isConflict());
    }

    @Test
    void thePurgeDeletesAHouseholdsEvents() throws Exception {
        createEvent(SAM, samHome, "Comic Con", 500);
        createEvent(KIM, kimHome, "Fair", 50);
        events(SAM, samHome);

        assertThat(purger.purge(samHome)).isEqualTo(1);
        events(SAM, samHome).andExpect(jsonPath("$.data", empty()));
        events(KIM, kimHome).andExpect(jsonPath("$.data[*].name", contains("Fair")));
    }

    private ResultActions createEvent(String email, String household, String name, double budget) throws Exception {
        return mvc.perform(json(post("/api/v1/event-budgets"), "{\"name\":\"" + name + "\",\"budget\":" + budget + "}")
                .with(as(email, household)));
    }

    private ResultActions addPurchase(String email, String household, String id, String name, double amount) throws Exception {
        return mvc.perform(json(post("/api/v1/event-budgets/" + id + "/purchases"),
                "{\"name\":\"" + name + "\",\"amount\":" + amount + "}").with(as(email, household)));
    }

    private ResultActions events(String email, String household) throws Exception {
        return mvc.perform(get("/api/v1/event-budgets").with(as(email, household)));
    }

    private static RequestPostProcessor as(String email, String household) {
        var token = jwt().jwt(j -> j.claim("email", email));
        return request -> {
            request.addHeader(HouseholdFilter.HEADER, household);
            return token.postProcessRequest(request);
        };
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body).header("Sec-Fetch-Site", "same-origin");
    }

    private String household(String email) {
        ObjectId id = new ObjectId();
        client.getDatabase("HOUSEHOLD").getCollection("households").insertOne(new Document("_id", id)
                .append("name", "Home")
                .append("members", List.of(new Document("email", email).append("role", "ADMIN")))
                .append("deletedAt", null));
        return id.toHexString();
    }
}
