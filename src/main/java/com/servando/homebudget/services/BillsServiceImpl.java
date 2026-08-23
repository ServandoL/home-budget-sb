package com.servando.homebudget.services;

import com.servando.homebudget.exceptions.RecordNotFoundException;
import com.servando.homebudget.models.database.BillsModel;
import com.servando.homebudget.models.dto.CreateBillsRequestDto;
import com.servando.homebudget.models.dto.GenericResponseDto;
import com.servando.homebudget.models.dto.UpdateBillsRequestDto;
import com.servando.homebudget.repository.BillsRepository;
import org.springframework.stereotype.Service;

@Service
public class BillsServiceImpl extends BaseCrudServiceImpl<BillsModel, BillsRepository, CreateBillsRequestDto, UpdateBillsRequestDto> {
    BillsServiceImpl(BillsRepository repository) {
        super(repository);
    }

    public GenericResponseDto<String> createBill(CreateBillsRequestDto request) {
        var toCreate = new BillsModel(
                request.getName(),
                request.getAmount(),
                request.getCategory(),
                request.getBillingCycle(),
                request.getDueDay()
        );
        return this.create(request, toCreate);
    }

    public GenericResponseDto<String> updateBill(String id, UpdateBillsRequestDto request) {
        var other = findOtherById(id);
        var toUpdate = new BillsModel(
                request.getName() == null ? other.getName() : request.getName(),
                request.getAmount() == null ? other.getAmount() : request.getAmount(),
                request.getCategory() == null ? other.getCategory() : request.getCategory(),
                request.getBillingCycle() == null ? other.getBillingCycle() : request.getBillingCycle(),
                request.getDueDay() == null ? other.getDueDay() : request.getDueDay()
                );
        return this.update(id, request, toUpdate);
    }
}
