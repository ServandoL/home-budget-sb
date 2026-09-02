package com.servando.homebudget.services;

import com.servando.homebudget.models.database.BillsModel;
import com.servando.homebudget.models.dto.CreateBillsRequestDto;
import com.servando.homebudget.models.dto.GenericResponseDto;
import com.servando.homebudget.models.dto.UpdateBillsRequestDto;
import com.servando.homebudget.repository.BillsRepository;
import com.servando.homebudget.utils.ResolveValueFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BillsServiceImpl extends BaseCrudServiceImpl<BillsModel, BillsRepository, CreateBillsRequestDto, UpdateBillsRequestDto> {
    BillsServiceImpl(BillsRepository repository) {
        super(repository);
    }


    @Cacheable(cacheNames = "bills", key = "'all'")
    public GenericResponseDto<List<BillsModel>> findAll() {
        var sort = Sort.by(Sort.Direction.DESC, "amount");
        return super.getAll(sort);
    }

    @CacheEvict(cacheNames = "bills", key = "'all'")
    public GenericResponseDto<BillsModel> createBill(CreateBillsRequestDto request) {
        var toCreate = new BillsModel(
                request.getName(),
                request.getAmount(),
                request.getCategory(),
                request.getBillingCycle(),
                request.getDueDay()
        );
        return this.create(request, toCreate);
    }

    @CacheEvict(cacheNames = "bills", key = "'all'")
    public GenericResponseDto<BillsModel> updateBill(String id, UpdateBillsRequestDto request) {
        var other = findOtherById(id);
        var toUpdate = new BillsModel(
                ResolveValueFactory.of(request.getName(), other.getName()),
                ResolveValueFactory.of(request.getAmount(), other.getAmount()),
                ResolveValueFactory.of(request.getCategory(), other.getCategory()),
                ResolveValueFactory.of(request.getBillingCycle(), other.getBillingCycle()),
                ResolveValueFactory.of(request.getDueDay(), other.getDueDay())
        );
        toUpdate.setCreatedAt(other.getCreatedAt());
        return this.update(id, request, toUpdate);
    }

    @Override
    @CacheEvict(cacheNames = "bills", key = "'all'")
    public GenericResponseDto<String> delete(String id) {
        return super.delete(id);
    }
}
