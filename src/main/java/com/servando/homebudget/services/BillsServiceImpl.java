package com.servando.homebudget.services;

import com.servando.homebudget.encryption.FieldCipher;
import com.servando.homebudget.models.database.BillsModel;
import com.servando.homebudget.models.dto.CreateBillsRequestDto;
import com.servando.homebudget.models.dto.GenericResponseDto;
import com.servando.homebudget.models.dto.UpdateBillsRequestDto;
import com.servando.homebudget.repository.BillsRepository;
import com.servando.homebudget.utils.ResolveValueFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BillsServiceImpl extends BaseCrudServiceImpl<BillsModel, BillsRepository, CreateBillsRequestDto, UpdateBillsRequestDto> {
    BillsServiceImpl(BillsRepository repository, FieldCipher cipher) {
        super(repository, cipher);
    }


    @Cacheable(cacheNames = "bills", key = "#householdId")
    public GenericResponseDto<List<BillsModel>> findAll(String householdId) {
        return super.getAll(householdId, largestFirst(BillsModel::getAmount));
    }

    @CacheEvict(cacheNames = "bills", key = "#householdId")
    public GenericResponseDto<BillsModel> createBill(String householdId, CreateBillsRequestDto request) {
        var toCreate = new BillsModel(
                request.getName(),
                request.getAmount(),
                request.getCategory(),
                request.getBillingCycle(),
                request.getDueDay()
        );
        return this.create(householdId, request, toCreate);
    }

    @CacheEvict(cacheNames = "bills", key = "#householdId")
    public GenericResponseDto<BillsModel> updateBill(String householdId, String id, UpdateBillsRequestDto request) {
        var other = findOtherById(householdId, id);
        var toUpdate = new BillsModel(
                ResolveValueFactory.of(request.getName(), other.getName()),
                ResolveValueFactory.of(request.getAmount(), other.getAmount()),
                ResolveValueFactory.of(request.getCategory(), other.getCategory()),
                ResolveValueFactory.of(request.getBillingCycle(), other.getBillingCycle()),
                ResolveValueFactory.of(request.getDueDay(), other.getDueDay())
        );
        toUpdate.setCreatedAt(other.getCreatedAt());
        return this.update(householdId, id, request, toUpdate);
    }

    @Override
    @CacheEvict(cacheNames = "bills", key = "#householdId")
    public GenericResponseDto<String> delete(String householdId, String id) {
        return super.delete(householdId, id);
    }
}
