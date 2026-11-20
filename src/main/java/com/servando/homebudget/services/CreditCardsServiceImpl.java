package com.servando.homebudget.services;

import com.servando.homebudget.models.database.CreditCardsModel;
import com.servando.homebudget.models.dto.CreateCreditCardsRequestDto;
import com.servando.homebudget.models.dto.GenericResponseDto;
import com.servando.homebudget.models.dto.UpdateCreditCardsRequestDto;
import com.servando.homebudget.repository.CreditCardsRepository;
import com.servando.homebudget.utils.ResolveValueFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CreditCardsServiceImpl extends BaseCrudServiceImpl<CreditCardsModel, CreditCardsRepository, CreateCreditCardsRequestDto, UpdateCreditCardsRequestDto> {
    CreditCardsServiceImpl(CreditCardsRepository repository) {
        super(repository);
    }

    @Cacheable(cacheNames = "credit-cards", key = "#householdId")
    public GenericResponseDto<List<CreditCardsModel>> findAll(String householdId) {
        var sort = Sort.by(Sort.Direction.DESC, "currentBalance");
        return this.getAll(householdId, sort);
    }

    @CacheEvict(cacheNames = "credit-cards", key = "#householdId")
    public GenericResponseDto<CreditCardsModel> createCreditCard(String householdId, CreateCreditCardsRequestDto request) {
        var toCreate = new CreditCardsModel(
                request.getName(),
                request.getBillingCycle(),
                request.getApr(),
                request.getCurrentBalance(),
                request.getDueDay(),
                request.getMinimumPayment()
        );
        return this.create(householdId, request, toCreate);
    }

    @CacheEvict(cacheNames = "credit-cards", key = "#householdId")
    public GenericResponseDto<CreditCardsModel> updateCreditCard(String householdId, String id, UpdateCreditCardsRequestDto request) {
        var other = this.findOtherById(householdId, id);
        var toUpdate = new CreditCardsModel(
                ResolveValueFactory.of(request.getName(), other.getName()),
                ResolveValueFactory.of(request.getBillingCycle(), other.getBillingCycle()),
                ResolveValueFactory.of(request.getApr(), other.getApr()),
                ResolveValueFactory.of(request.getCurrentBalance(), other.getCurrentBalance()),
                ResolveValueFactory.of(request.getDueDay(), other.getDueDay()),
                ResolveValueFactory.of(request.getMinimumPayment(), other.getMinimumPayment())
        );
        toUpdate.setCreatedAt(other.getCreatedAt());
        return this.update(householdId, id, request, toUpdate);
    }

    @Override
    @CacheEvict(cacheNames = "credit-cards", key = "#householdId")
    public GenericResponseDto<String> delete(String householdId, String id) {
        return super.delete(householdId, id);
    }
}
