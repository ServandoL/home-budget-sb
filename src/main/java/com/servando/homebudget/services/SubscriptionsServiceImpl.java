package com.servando.homebudget.services;

import com.servando.homebudget.models.database.IncomesModel;
import com.servando.homebudget.models.database.SubscriptionsModel;
import com.servando.homebudget.models.dto.*;
import com.servando.homebudget.repository.IncomesRepository;
import com.servando.homebudget.repository.SubscriptionsRepository;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionsServiceImpl extends BaseCrudServiceImpl<SubscriptionsModel, SubscriptionsRepository, CreateSubscriptionsRequestDto, UpdateSubscriptionsRequestDto> {
    SubscriptionsServiceImpl(SubscriptionsRepository repository) {
        super(repository);
    }

    public GenericResponseDto<String> createIncome(CreateSubscriptionsRequestDto request) {
        var toCreate = new SubscriptionsModel(
                request.getName(),
                request.getBillingCycle(),
                request.getAmount(),
                request.getBillingDay()
        );
        return this.create(request, toCreate);
    }

    public GenericResponseDto<String> updateIncome(String id, UpdateSubscriptionsRequestDto request) {
        var other = findOtherById(id);
        var toUpdate = new SubscriptionsModel(
                request.getName() == null ? other.getName() : request.getName(),
                request.getBillingCycle() == null ? other.getBillingCycle() : request.getBillingCycle(),
                request.getAmount() == null ? other.getAmount() : request.getAmount(),
                request.getBillingDay() == null ? other.getBillingDay() : request.getBillingDay()
        );
        return this.update(id, request, toUpdate);
    }
}
