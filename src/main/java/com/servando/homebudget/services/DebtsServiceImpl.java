package com.servando.homebudget.services;

import com.servando.homebudget.models.database.DebtsModel;
import com.servando.homebudget.models.dto.CreateDebtDto;
import com.servando.homebudget.models.dto.GenericResponseDto;
import com.servando.homebudget.models.dto.UpdateDebtDto;
import com.servando.homebudget.repository.DebtsRepository;
import com.servando.homebudget.utils.ResolveValueFactory;
import org.springframework.stereotype.Service;

@Service
public class DebtsServiceImpl extends BaseCrudServiceImpl<DebtsModel, DebtsRepository, CreateDebtDto, UpdateDebtDto> {
    DebtsServiceImpl(DebtsRepository debtsRepository) {
        super(debtsRepository);
    }

    public GenericResponseDto<String> createDebt(CreateDebtDto request) {
        var toCreate = new DebtsModel(request.getName(), request.getBillingCycle());
        toCreate.setAmount(request.getAmount());
        toCreate.setAmountPaid(request.getAmountPaid());
        toCreate.setDatePaid(request.getDatePaid());
        toCreate.setDebtStarted(request.getDebtStarted());
        toCreate.setDescription(request.getDescription());
        toCreate.setFrom(request.getFrom());
        return this.create(request, toCreate);
    }

    public GenericResponseDto<String> updateDebt(String id, UpdateDebtDto request) {
        var other = findOtherById(id);
        var toUpdate = new DebtsModel(
                ResolveValueFactory.of(request.getName(), other.getName()),
                ResolveValueFactory.of(request.getBillingCycle(), other.getBillingCycle())
        );
        toUpdate.setFrom(ResolveValueFactory.of(request.getFrom(), other.getFrom()));
        toUpdate.setDescription(ResolveValueFactory.of(request.getDescription(), other.getDescription()));
        toUpdate.setDebtStarted(ResolveValueFactory.of(request.getDebtStarted(), other.getDebtStarted()));
        toUpdate.setDatePaid(ResolveValueFactory.of(request.getDatePaid(), other.getDatePaid()));
        toUpdate.setAmountPaid(ResolveValueFactory.of(request.getAmountPaid(), other.getAmountPaid()));
        toUpdate.setAmount(ResolveValueFactory.of(request.getAmount(), other.getAmount()));
        return this.update(id, request, toUpdate);
    }
}
