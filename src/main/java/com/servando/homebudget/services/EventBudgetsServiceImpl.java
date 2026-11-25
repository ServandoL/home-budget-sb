package com.servando.homebudget.services;

import com.servando.homebudget.encryption.FieldCipher;
import com.servando.homebudget.models.database.EventBudgetModel;
import com.servando.homebudget.models.database.EventPurchase;
import com.servando.homebudget.models.dto.AddEventPurchaseDto;
import com.servando.homebudget.models.dto.CreateEventBudgetDto;
import com.servando.homebudget.models.dto.GenericResponseDto;
import com.servando.homebudget.models.dto.UpdateEventBudgetDto;
import com.servando.homebudget.repository.EventBudgetsRepository;
import com.servando.homebudget.utils.ResolveValueFactory;
import org.bson.types.ObjectId;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Service
public class EventBudgetsServiceImpl extends BaseCrudServiceImpl<EventBudgetModel, EventBudgetsRepository, CreateEventBudgetDto, UpdateEventBudgetDto> {
    /** Plenty for one outing, and keeps one event's encrypted list small. */
    static final int MAX_PURCHASES = 500;
    private static final int ATTEMPTS = 3;

    protected EventBudgetsServiceImpl(EventBudgetsRepository repository, FieldCipher cipher) {
        super(repository, cipher);
    }

    /** Newest first; the UI splits them into in progress and done. */
    @Cacheable(cacheNames = "event-budgets", key = "#householdId")
    public GenericResponseDto<List<EventBudgetModel>> findAll(String householdId) {
        return this.getAll(householdId, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @CacheEvict(cacheNames = "event-budgets", key = "#householdId")
    public GenericResponseDto<EventBudgetModel> createEvent(String householdId, CreateEventBudgetDto request) {
        var toCreate = new EventBudgetModel(request.getName(), Instant.now());
        toCreate.setBudget(request.getBudget());
        toCreate.setDate(blankToNull(request.getDate()));
        return this.create(householdId, request, toCreate);
    }

    /** A rename is checked against the household's other names, like every record. */
    @CacheEvict(cacheNames = "event-budgets", key = "#householdId")
    public GenericResponseDto<EventBudgetModel> updateEvent(String householdId, String id, UpdateEventBudgetDto request) {
        var event = findOtherById(householdId, id);
        event.setName(ResolveValueFactory.of(request.getName(), event.getName()));
        event.setBudget(ResolveValueFactory.of(request.getBudget(), event.getBudget()));
        if (request.getDate() != null) {
            event.setDate(blankToNull(request.getDate()));
        }
        if (Boolean.TRUE.equals(request.getDone()) && event.getDoneAt() == null) {
            event.setDoneAt(Instant.now());
        } else if (Boolean.FALSE.equals(request.getDone())) {
            event.setDoneAt(null);
        }
        event.setUpdatedAt(Instant.now());
        return this.update(householdId, id, request, event);
    }

    @CacheEvict(cacheNames = "event-budgets", key = "#householdId")
    public GenericResponseDto<EventBudgetModel> addPurchase(String householdId, String id, AddEventPurchaseDto request) {
        var purchase = new EventPurchase(new ObjectId().toHexString(), request.name().strip(),
                Math.round(request.amount() * 100) / 100.0, Instant.now());
        return modify(householdId, id, event -> {
            if (event.getPurchases().size() >= MAX_PURCHASES) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "An event holds up to " + MAX_PURCHASES + " purchases.");
            }
            event.getPurchases().addFirst(purchase);
        }, "Purchase added");
    }

    @CacheEvict(cacheNames = "event-budgets", key = "#householdId")
    public GenericResponseDto<EventBudgetModel> removePurchase(String householdId, String id, String purchaseId) {
        return modify(householdId, id, event -> event.getPurchases().removeIf(p -> p.id().equals(purchaseId)),
                "Purchase removed");
    }

    @Override
    @CacheEvict(cacheNames = "event-budgets", key = "#householdId")
    public GenericResponseDto<String> delete(String householdId, String id) {
        return super.delete(householdId, id);
    }

    /** Reads the event, changes it and saves it, again on the newer copy if someone saved in between. */
    private GenericResponseDto<EventBudgetModel> modify(String householdId, String id, Consumer<EventBudgetModel> change,
                                                        String message) {
        for (int attempt = 1; ; attempt++) {
            var event = findOtherById(householdId, id);
            if (event.getPurchases() == null) {
                event.setPurchases(new ArrayList<>());
            } else {
                event.setPurchases(new ArrayList<>(event.getPurchases()));
            }
            change.accept(event);
            event.setUpdatedAt(Instant.now());
            try {
                return new GenericResponseDto<>(true, message, repository.save(event));
            } catch (OptimisticLockingFailureException e) {
                if (attempt == ATTEMPTS) {
                    throw e;
                }
            }
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
