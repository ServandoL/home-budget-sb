package com.servando.homebudget;

import com.home.household.HouseholdFilter;
import com.mongodb.client.MongoClient;
import com.servando.homebudget.configuration.MongoConfiguration;
import com.servando.homebudget.encryption.EncryptionMigration;
import com.servando.homebudget.encryption.FieldCipher;
import com.servando.homebudget.models.database.BillsModel;
import org.bson.types.Binary;
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
        "access.team-domain=test.cloudflareaccess.com", "access.aud=test-aud",
        "budget.encryption-key=" + HouseholdScopingTests.KEY})
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

    static final String KEY = "ZXnlYD1EWjdZQXlQFHCfLkBP6dBJmAlXrGNLTzdRTGg=";
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

        Document stored = mongo.getCollection("Bills").find().first();
        assertThat(stored.getObjectId("householdId")).isEqualTo(new ObjectId(samHome));
    }

    @Test
    void namesAreUniquePerHousehold() throws Exception {
        createBill(SAM, samHome, "Rent", 1500).andExpect(status().isCreated());
        createBill(KIM, kimHome, "Rent", 900).andExpect(status().isCreated());
        createBill(SAM, samHome, "Rent", 1).andExpect(status().isConflict());

        List<String> indexes = mongo.indexOps(BillsModel.class).getIndexInfo().stream().map(i -> i.getName()).toList();
        assertThat(indexes).contains("householdId_nameIndex_unique").doesNotContain("householdId_name_unique");
    }

    @Test
    void namesAndAmountsAreUnreadableInTheDatabase() throws Exception {
        createBill(SAM, samHome, "Rent", 1500).andExpect(status().isCreated());
        createBill(SAM, samHome, "Power", 90).andExpect(status().isCreated());
        createBill(SAM, samHome, "Mortgage", 2100).andExpect(status().isCreated());

        Document stored = mongo.getCollection("Bills").find(new Document("householdId", new ObjectId(samHome))).first();
        assertThat(stored.get("name")).isInstanceOf(Binary.class);
        assertThat(((Binary) stored.get("name")).getType()).isEqualTo(FieldCipher.SUBTYPE);
        assertThat(stored.get("amount")).isInstanceOf(Binary.class);
        assertThat(stored.getString("billingCycle")).isNotNull(); // not sensitive, so still queryable
        String everything = mongo.getCollection("Bills").find().into(new java.util.ArrayList<>()).toString();
        assertThat(everything).doesNotContain("Rent", "Power", "Mortgage", "1500", "2100");

        // MongoDB can't sort ciphertext, so the API sorts in Java: largest first, as before.
        bills(SAM, samHome).andExpect(jsonPath("$.data[*].name", contains("Mortgage", "Rent", "Power")))
                .andExpect(jsonPath("$.data[0].amount").value(2100.0))
                .andExpect(jsonPath("$.data[0].nameIndex").doesNotExist());
    }

    @Test
    void recordsFromBeforeEncryptionAreEncryptedAtStartup() throws Exception {
        String id = new ObjectId().toHexString();
        mongo.getCollection("Bills").insertOne(new Document("_id", new ObjectId(id))
                .append("householdId", new ObjectId(samHome)).append("name", "Rent").append("amount", 1500.0)
                .append("category", "RENT").append("billingCycle", "MONTHLY"));

        assertThat(EncryptionMigration.run(mongo, MongoConfiguration.MODELS)).isEqualTo(1);
        assertThat(EncryptionMigration.run(mongo, MongoConfiguration.MODELS)).isZero();

        Document stored = mongo.getCollection("Bills").find(new Document("_id", new ObjectId(id))).first();
        assertThat(stored.get("name")).isInstanceOf(Binary.class);
        assertThat(stored.get("amount")).isInstanceOf(Binary.class);
        bills(SAM, samHome).andExpect(jsonPath("$.data[*].name", contains("Rent")))
                .andExpect(jsonPath("$.data[0].amount").value(1500.0));
        // It has a nameIndex now, so the name is still taken.
        createBill(SAM, samHome, "Rent", 1).andExpect(status().isConflict());
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
    void anIncomeNeedsAPayFrequencyAndCanBeMonthly() throws Exception {
        mvc.perform(json(post("/api/v1/incomes"), "{\"name\":\"Salary\",\"netAmount\":4000,\"billingCycle\":\"monthly\"}")
                .with(as(SAM, samHome))).andExpect(status().isBadRequest());
        mvc.perform(json(post("/api/v1/incomes"),
                        "{\"name\":\"Salary\",\"netAmount\":4000,\"frequency\":\"monthly\",\"billingCycle\":\"monthly\"}")
                .with(as(SAM, samHome)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.frequency").value("monthly"));
        mvc.perform(get("/api/v1/dashboard").with(as(SAM, samHome)))
                .andExpect(jsonPath("$.monthlyIncome").value(4000.0))
                .andExpect(jsonPath("$.frequency").value("monthly"));
    }

    @Test
    void anIncomeHasToPaySomething() throws Exception {
        mvc.perform(json(post("/api/v1/incomes"),
                        "{\"name\":\"Side gig\",\"netAmount\":0,\"frequency\":\"weekly\",\"billingCycle\":\"monthly\"}")
                .with(as(SAM, samHome))).andExpect(status().isBadRequest());
        mvc.perform(json(post("/api/v1/incomes"),
                        "{\"name\":\"Refund\",\"netAmount\":-200,\"frequency\":\"monthly\",\"billingCycle\":\"monthly\"}")
                .with(as(SAM, samHome))).andExpect(status().isBadRequest());
        String pay = JsonPath.read(mvc.perform(json(post("/api/v1/incomes"),
                        "{\"name\":\"Paycheck\",\"netAmount\":2450,\"frequency\":\"biweekly\",\"billingCycle\":\"monthly\"}")
                .with(as(SAM, samHome))).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.data.id");

        mvc.perform(json(put("/api/v1/incomes/" + pay), "{\"name\":\"Paycheck\",\"netAmount\":0}")
                .with(as(SAM, samHome))).andExpect(status().isBadRequest());
    }

    @Test
    void onlyHouseRepairsAndDebtsCanBeOptional() throws Exception {
        // The app only offers monthly, semiannual and annual for these; an optional cost isn't a bill.
        mvc.perform(json(post("/api/v1/bills"),
                        "{\"name\":\"Gym\",\"amount\":40,\"category\":\"other\",\"billingCycle\":\"optional\"}")
                .with(as(SAM, samHome))).andExpect(status().isBadRequest());
        mvc.perform(json(post("/api/v1/subscriptions"),
                        "{\"name\":\"Hulu\",\"amount\":17.99,\"priorityLevel\":\"optional\",\"billingCycle\":\"optional\"}")
                .with(as(SAM, samHome))).andExpect(status().isBadRequest());
        mvc.perform(json(post("/api/v1/credit-cards"),
                        "{\"name\":\"Visa\",\"currentBalance\":500,\"billingCycle\":\"optional\"}")
                .with(as(SAM, samHome))).andExpect(status().isBadRequest());

        // An optional subscription is a priority, which says nothing about the amount.
        String hulu = JsonPath.read(mvc.perform(json(post("/api/v1/subscriptions"),
                        "{\"name\":\"Hulu\",\"amount\":17.99,\"priorityLevel\":\"optional\",\"billingCycle\":\"monthly\"}")
                .with(as(SAM, samHome))).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.data.id");
        mvc.perform(json(put("/api/v1/subscriptions/" + hulu), "{\"name\":\"Hulu\",\"billingCycle\":\"optional\"}")
                .with(as(SAM, samHome))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/dashboard").with(as(SAM, samHome)))
                .andExpect(jsonPath("$.subscriptionsMonthly").value(17.99));
    }

    @Test
    void aNewDebtCountsTowardTheDashboardStraightAway() throws Exception {
        mvc.perform(json(post("/api/v1/debts"), "{\"name\":\"Car\",\"billingCycle\":\"monthly\",\"amount\":1000,"
                        + "\"amountPaid\":250,\"from\":\"Bank\",\"description\":\"Loan\",\"debtStarted\":\"2026-01-01\"}")
                        .with(as(SAM, samHome)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.amountOwed").value(750.0));
        mvc.perform(post("/api/v1/credit-cards").contentType(MediaType.APPLICATION_JSON)
                        .header("Sec-Fetch-Site", "same-origin")
                        .content("{\"name\":\"Visa\",\"billingCycle\":\"monthly\",\"currentBalance\":500}")
                        .with(as(SAM, samHome)))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/dashboard").with(as(SAM, samHome)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDebts").value(750.0))
                .andExpect(jsonPath("$.creditCardBalances").value(500.0))
                .andExpect(jsonPath("$.totalOwed").value(1250.0))
                .andExpect(jsonPath("$.creditCardsMonthly").value(0.0));
    }

    @Test
    void editingARepairKeepsWhenItWasCreated() throws Exception {
        String repair = JsonPath.read(mvc.perform(json(post("/api/v1/house-repairs"),
                        "{\"name\":\"Roof\",\"description\":\"Leak\",\"priority\":1,\"cost\":800,"
                                + "\"status\":\"quoted\",\"category\":\"Exterior\"}").with(as(SAM, samHome)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.data.id");
        String created = JsonPath.read(mvc.perform(get("/api/v1/house-repairs").with(as(SAM, samHome)))
                .andReturn().getResponse().getContentAsString(), "$.data[0].createdAt");

        Thread.sleep(5);
        mvc.perform(json(put("/api/v1/house-repairs/" + repair), "{\"name\":\"Roof\",\"status\":\"complete\"}")
                .with(as(SAM, samHome))).andExpect(status().isOk());

        mvc.perform(get("/api/v1/house-repairs").with(as(SAM, samHome)))
                .andExpect(jsonPath("$.data[0].status").value("complete"))
                .andExpect(jsonPath("$.data[0].createdAt").value(created));
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
