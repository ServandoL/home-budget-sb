package com.servando.homebudget.services;

import com.servando.homebudget.models.database.BillsModel;
import com.servando.homebudget.models.database.IncomesModel;
import com.servando.homebudget.models.dto.*;
import com.servando.homebudget.repository.BillsRepository;
import com.servando.homebudget.repository.IncomesRepository;
import org.springframework.stereotype.Service;

@Service
public class IncomeServiceImpl extends BaseCrudServiceImpl<IncomesModel, IncomesRepository, CreateIncomeRequestDto, UpdateIncomeRequestDto> {
    IncomeServiceImpl(IncomesRepository repository) {
        super(repository);
    }

    public GenericResponseDto<String> createIncome(CreateIncomeRequestDto request) {
        var toCreate = new IncomesModel(
                request.getName(),
                request.getBillingCycle(),
                request.getFrequency(),
                request.getNetAmount()
        );
        return this.create(request, toCreate);
    }

    public GenericResponseDto<String> updateIncome(String id, UpdateIncomeRequestDto request) {
        var other = findOtherById(id);
        var toUpdate = new IncomesModel(
                request.getName() == null ? other.getName() : request.getName(),
                request.getBillingCycle() == null ? other.getBillingCycle() : request.getBillingCycle(),
                request.getFrequency() == null ? other.getFrequency() : request.getFrequency(),
                request.getNetAmount() == null ? other.getNetAmount() : request.getNetAmount()
                );
        return this.update(id, request, toUpdate);
    }
}
