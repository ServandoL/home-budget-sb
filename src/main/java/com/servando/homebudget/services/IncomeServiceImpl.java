package com.servando.homebudget.services;

import com.servando.homebudget.models.database.IncomesModel;
import com.servando.homebudget.models.dto.*;
import com.servando.homebudget.repository.IncomesRepository;
import com.servando.homebudget.utils.ResolveValueFactory;
import org.springframework.stereotype.Service;

@Service
public class IncomeServiceImpl extends BaseCrudServiceImpl<IncomesModel, IncomesRepository, CreateIncomeRequestDto, UpdateIncomeRequestDto> {
    IncomeServiceImpl(IncomesRepository repository) {
        super(repository);
    }

    public GenericResponseDto<IncomesModel> createIncome(CreateIncomeRequestDto request) {
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
                ResolveValueFactory.of(request.getName(), other.getName()),
                ResolveValueFactory.of(request.getBillingCycle(), other.getBillingCycle()),
                ResolveValueFactory.of(request.getFrequency(), other.getFrequency()),
                ResolveValueFactory.of(request.getNetAmount(), other.getNetAmount())
        );
        toUpdate.setCreatedAt(other.getCreatedAt());
        return this.update(id, request, toUpdate);
    }
}
