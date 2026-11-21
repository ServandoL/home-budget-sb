package com.servando.homebudget;

import com.home.household.HouseholdFilter;
import com.mongodb.client.MongoClient;
import com.servando.homebudget.models.database.BillsModel;
import com.servando.homebudget.services.BudgetPurger;
import com.jayway.jsonpath.JsonPath;
import org.bson.Document;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Against a real MongoDB: what one household saves, another can't see, change or delete, and
 * the cache and the dashboard never mix them.
 */
@SpringBootTest(properties = {"MONGO_DATABASE=BUDGET", "SERVER_PORT=0", "household.database=HOUSEHOLD",
        "access.team-domain=test.cloudflareaccess.com", "access.aud=test-aud"})
@AutoConfigureMockMvc
@Testcontainers
class HouseholdScopingTests {

    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:8.0");

    // MongoConfiguration builds its client from these, not from a service connection.
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
    void eachHouseholdSeesOnlyItsOwnBills() throws Exception {
        createBill(SAM, samHome, "Rent", 1500).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.householdId").doesNotExist());
        // Sam's list is cached now; with one cache key for everyone, Kim would get it.
        bills(SAM, samHome).andExpect(jsonPath("$.data[*].name", contains("Rent")));
        bills(KIM, kimHome).andExpect(jsonPath("$.data", empty()));

        createBill(KIM, kimHome, "Power", 90).andExpect(status().isCreated());
        bills(KIM, kimHome).andExpect(jsonPath("$.data[*].name", contains("Power")));
        bills(SAM, samHome).andExpect(jsonPath("$.data[*].name", contains("Rent")));

        Document stored = mongo.getCollection("Bills").find(new Document("name", "Rent")).first();
        assertThat(stored.getObjectId("householdId")).isEqualTo(new ObjectId(samHome));
    }

    @Test
    void namesAreUniquePerHousehold() throws Exception {
        createBill(SAM, samHome, "Rent", 1500).andExpect(status().isCreated());
        createBill(KIM, kimHome, "Rent", 900).andExpect(status().isCreated());
        createBill(SAM, samHome, "Rent", 1).andExpect(status().isConflict());

        List<String> indexes = mongo.indexOps(BillsModel.class).getIndexInfo().stream().map(i -> i.getName()).toList();
        assertThat(indexes).contains("householdId_name_unique");
    }

    @Test
    void anotherHouseholdsBillCantBeChangedOrDeleted() throws Exception {
        String rent = JsonPath.read(createBill(SAM, samHome, "Rent", 1500).andReturn().getResponse().getContentAsString(),
                "$.data.id");

        // A complete, valid update, so only the household decides the outcome.
        mvc.perform(json(put("/api/v1/bills/" + rent), "{\"name\":\"Rent\",\"amount\":1}").with(as(KIM, kimHome)))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/bills/" + rent).with(as(KIM, kimHome)));

        bills(SAM, samHome).andExpect(jsonPath("$.data[0].amount").value(1500.0));
    }

    @Test
    void theDashboardTotalsOnlyTheHouseholdsRecords() throws Exception {
        createBill(SAM, samHome, "Rent", 1500);
        createBill(KIM, kimHome, "Rent", 900);

        mvc.perform(get("/api/v1/dashboard").with(as(SAM, samHome)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalObligations").value(1500.0));
    }

    @Test
    void someoneOutsideTheHouseholdIsRefused() throws Exception {
        bills(KIM, samHome).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/bills").with(jwt().jwt(j -> j.claim("email", SAM)))).andExpect(status().is(428));
    }

    @Test
    void thePurgeDeletesOneHouseholdsRecordsAndItsCachedList() throws Exception {
        createBill(SAM, samHome, "Rent", 1500);
        createBill(KIM, kimHome, "Power", 90);
        bills(SAM, samHome);

        assertThat(purger.purge(samHome)).isEqualTo(1);
        assertThat(purger.purge(samHome)).isZero();

        assertThat(caches.getCache("bills").get(samHome)).isNull();
        bills(SAM, samHome).andExpect(jsonPath("$.data", empty()));
        bills(KIM, kimHome).andExpect(jsonPath("$.data[*].name", contains("Power")));
    }

    @Test
    void everyWriteTellsTheHouseholdsOtherDevices() throws Exception {
        MvcResult samsStream = mvc.perform(get("/api/v1/events").with(as(SAM, samHome)))
                .andExpect(request().asyncStarted()).andReturn();
        MvcResult kimsStream = mvc.perform(get("/api/v1/events").with(as(KIM, kimHome)))
                .andExpect(request().asyncStarted()).andReturn();

        String rent = JsonPath.read(createBill(SAM, samHome, "Rent", 1500).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.data.id");
        mvc.perform(json(put("/api/v1/bills/" + rent), "{\"name\":\"Rent\",\"amount\":1600}").with(as(SAM, samHome)))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/v1/bills/" + rent).with(as(SAM, samHome))).andExpect(status().isOk());

        assertThat(samsStream.getResponse().getContentAsString())
                .contains("data:{\"resource\":\"bills\",\"action\":\"created\",\"id\":\"" + rent + "\"}")
                .contains("data:{\"resource\":\"bills\",\"action\":\"updated\",\"id\":\"" + rent + "\"}")
                .contains("data:{\"resource\":\"bills\",\"action\":\"deleted\",\"id\":\"" + rent + "\"}");
        assertThat(kimsStream.getResponse().getContentAsString()).doesNotContain("event:change");
    }

    private ResultActions createBill(String email, String household, String name, double amount) throws Exception {
        return mvc.perform(json(post("/api/v1/bills"),
                "{\"name\":\"" + name + "\",\"amount\":" + amount + ",\"category\":\"rent\",\"billingCycle\":\"monthly\"}")
                .with(as(email, household)));
    }

    private ResultActions bills(String email, String household) throws Exception {
        return mvc.perform(get("/api/v1/bills").with(as(email, household)));
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
