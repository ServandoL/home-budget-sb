package com.servando.homebudget;

import com.home.household.HouseholdFilter;
import com.mongodb.client.MongoClient;
import com.servando.homebudget.services.BudgetPurger;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Against a real MongoDB: one set of planning settings per household, encrypted, used by the dashboard. */
@SpringBootTest(properties = {"MONGO_DATABASE=BUDGET", "SERVER_PORT=0", "household.database=HOUSEHOLD",
        "access.team-domain=test.cloudflareaccess.com", "access.aud=test-aud",
        "budget.encryption-key=" + HouseholdScopingTests.KEY})
@AutoConfigureMockMvc
@Testcontainers
class BudgetSettingsTests {

    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:8.0");

    @DynamicPropertySource
    static void mongo(DynamicPropertyRegistry registry) {
        registry.add("MONGO_APP_URL", MONGO::getConnectionString);
    }

    static final String SAM = "sam@example.com";
    static final String ALEX = "alex@example.com";
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
        samHome = household(SAM, ALEX);
        kimHome = household(KIM);
    }

    @Test
    void aHouseholdStartsOnTheDefaults() throws Exception {
        settings(SAM, samHome).andExpect(status().isOk())
                .andExpect(jsonPath("$.defaults").value(true))
                .andExpect(jsonPath("$.payPeriodBuffer").value(100.0))
                .andExpect(jsonPath("$.targetMonthlyTransfer").doesNotExist())
                .andExpect(jsonPath("$.updatedBy").doesNotExist());
    }

    @Test
    void everyoneInTheHouseholdSharesOneSetAndNoOneElseSeesIt() throws Exception {
        save(SAM, samHome, "{\"targetMonthlyTransfer\":1200,\"payPeriodBuffer\":150}").andExpect(status().isOk())
                .andExpect(jsonPath("$.defaults").value(false))
                .andExpect(jsonPath("$.updatedBy").value(SAM));

        settings(ALEX, samHome).andExpect(jsonPath("$.targetMonthlyTransfer").value(1200.0))
                .andExpect(jsonPath("$.payPeriodBuffer").value(150.0));
        settings(KIM, kimHome).andExpect(jsonPath("$.defaults").value(true));

        save(ALEX, samHome, "{\"targetMonthlyTransfer\":null,\"payPeriodBuffer\":80}")
                .andExpect(jsonPath("$.targetMonthlyTransfer").doesNotExist())
                .andExpect(jsonPath("$.updatedBy").value(ALEX));
        assertThat(mongo.getCollection("BudgetSettings").countDocuments()).isEqualTo(1);
    }

    @Test
    void theAmountsAreStoredEncrypted() throws Exception {
        save(SAM, samHome, "{\"targetMonthlyTransfer\":1200,\"payPeriodBuffer\":150}");

        Document stored = mongo.getCollection("BudgetSettings").find().first();
        assertThat(stored.get("targetMonthlyTransfer")).isInstanceOf(Binary.class);
        assertThat(stored.get("payPeriodBuffer")).isInstanceOf(Binary.class);
        assertThat(stored.getObjectId("householdId")).isEqualTo(new ObjectId(samHome));
        // And they read back as numbers on the next request.
        settings(SAM, samHome).andExpect(jsonPath("$.payPeriodBuffer").value(150.0));
    }

    @Test
    void badValuesAreRefused() throws Exception {
        save(SAM, samHome, "{\"payPeriodBuffer\":-5}").andExpect(status().isBadRequest());
        save(SAM, samHome, "{\"targetMonthlyTransfer\":-1,\"payPeriodBuffer\":5}").andExpect(status().isBadRequest());
        save(SAM, samHome, "{\"targetMonthlyTransfer\":100}").andExpect(status().isBadRequest());
        settings(SAM, samHome).andExpect(jsonPath("$.defaults").value(true));
    }

    @Test
    void resettingGoesBackToTheDefaults() throws Exception {
        save(SAM, samHome, "{\"targetMonthlyTransfer\":1200,\"payPeriodBuffer\":150}");
        mvc.perform(delete("/api/v1/settings").header("Sec-Fetch-Site", "same-origin").with(as(SAM, samHome)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaults").value(true));
        assertThat(mongo.getCollection("BudgetSettings").countDocuments()).isZero();
    }

    @Test
    void theDashboardUsesTheSavedSettings() throws Exception {
        // $2,600 every two weeks: most months bring two paydays, so each moves half the target.
        mvc.perform(json(post("/api/v1/incomes"), "{\"name\":\"Pay\",\"netAmount\":2600,\"frequency\":\"biweekly\",\"billingCycle\":\"monthly\"}")
                .with(as(SAM, samHome))).andExpect(status().isCreated());
        double perPayday = 1200 / 2.0;

        dashboard(samHome, "").andExpect(jsonPath("$.targetMonthlyTransfer").isNumber());
        save(SAM, samHome, "{\"targetMonthlyTransfer\":1200,\"payPeriodBuffer\":150}");

        dashboard(samHome, "").andExpect(jsonPath("$.targetMonthlyTransfer").value(1200.0))
                .andExpect(jsonPath("$.recommendedTransfer", closeTo(perPayday + 150, 0.01)));
        // A page from before the settings moved here still sends its browser's values; they're ignored.
        dashboard(samHome, "?targetMonthlyTransfer=500&payPeriodBuffer=0")
                .andExpect(jsonPath("$.targetMonthlyTransfer").value(1200.0))
                .andExpect(jsonPath("$.recommendedTransfer", closeTo(perPayday + 150, 0.01)));
    }

    @Test
    void savingTellsTheHouseholdsOtherDevices() throws Exception {
        MvcResult alexStream = mvc.perform(get("/api/v1/events").with(as(ALEX, samHome)))
                .andExpect(request().asyncStarted()).andReturn();
        MvcResult kimStream = mvc.perform(get("/api/v1/events").with(as(KIM, kimHome)))
                .andExpect(request().asyncStarted()).andReturn();

        save(SAM, samHome, "{\"payPeriodBuffer\":150}");

        assertThat(alexStream.getResponse().getContentAsString())
                .contains("data:{\"resource\":\"settings\",\"action\":\"updated\",\"id\":\"" + samHome + "\"}");
        assertThat(kimStream.getResponse().getContentAsString()).doesNotContain("event:change");
    }

    @Test
    void thePurgeDeletesTheSettings() throws Exception {
        save(SAM, samHome, "{\"payPeriodBuffer\":150}");
        save(KIM, kimHome, "{\"payPeriodBuffer\":50}");

        assertThat(purger.purge(samHome)).isEqualTo(1);
        settings(SAM, samHome).andExpect(jsonPath("$.defaults").value(true));
        settings(KIM, kimHome).andExpect(jsonPath("$.payPeriodBuffer").value(50.0));
    }

    private ResultActions settings(String email, String household) throws Exception {
        return mvc.perform(get("/api/v1/settings").with(as(email, household)));
    }

    private ResultActions save(String email, String household, String body) throws Exception {
        return mvc.perform(json(put("/api/v1/settings"), body).with(as(email, household)));
    }

    private ResultActions dashboard(String household, String query) throws Exception {
        return mvc.perform(get("/api/v1/dashboard" + query).with(as(SAM, household)));
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

    private String household(String... emails) {
        ObjectId id = new ObjectId();
        client.getDatabase("HOUSEHOLD").getCollection("households").insertOne(new Document("_id", id)
                .append("name", "Home")
                .append("members", List.of(emails).stream()
                        .map(email -> new Document("email", email).append("role", "MEMBER")).toList())
                .append("deletedAt", null));
        return id.toHexString();
    }
}
