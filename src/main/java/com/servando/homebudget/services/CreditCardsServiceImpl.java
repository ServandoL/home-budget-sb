package com.servando.homebudget.services;

import com.servando.homebudget.models.database.CreditCardsModel;
import com.servando.homebudget.models.dto.CreateCreditCardsRequestDto;
import com.servando.homebudget.models.dto.GenericResponseDto;
import com.servando.homebudget.models.dto.UpdateCreditCardsRequestDto;
import com.servando.homebudget.repository.CreditCardsRepository;
import com.servando.homebudget.utils.ResolveValueFactory;
import org.springframework.stereotype.Service;

@Service
public class CreditCardsServiceImpl extends BaseCrudServiceImpl<CreditCardsModel, CreditCardsRepository, CreateCreditCardsRequestDto, UpdateCreditCardsRequestDto> {
    CreditCardsServiceImpl(CreditCardsRepository repository) {
        super(repository);
    }

    public GenericResponseDto<CreditCardsModel> createCreditCard(CreateCreditCardsRequestDto request) {
        var toCreate = new CreditCardsModel(
                request.getName(),
                request.getBillingCycle(),
                request.getApr(),
                request.getCurrentBalance(),
                request.getDueDay(),
                request.getMinimumPayment()
        );
        return this.create(request, toCreate);
    }

    public GenericResponseDto<CreditCardsModel> updateCreditCard(String id, UpdateCreditCardsRequestDto request) {
        var other = this.findOtherById(id);
        var toUpdate = new CreditCardsModel(
                ResolveValueFactory.of(request.getName(), other.getName()),
                ResolveValueFactory.of(request.getBillingCycle(), other.getBillingCycle()),
                ResolveValueFactory.of(request.getApr(), other.getApr()),
                ResolveValueFactory.of(request.getCurrentBalance(), other.getCurrentBalance()),
                ResolveValueFactory.of(request.getDueDay(), other.getDueDay()),
                ResolveValueFactory.of(request.getMinimumPayment(), other.getMinimumPayment())
        );
        toUpdate.setCreatedAt(other.getCreatedAt());
        return this.update(id, request, toUpdate);
    }

}
