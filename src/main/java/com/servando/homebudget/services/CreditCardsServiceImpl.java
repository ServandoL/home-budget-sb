package com.servando.homebudget.services;

import com.servando.homebudget.models.database.CreditCardsModel;
import com.servando.homebudget.models.dto.CreateCreditCardsRequestDto;
import com.servando.homebudget.models.dto.GenericResponseDto;
import com.servando.homebudget.models.dto.UpdateCreditCardsRequestDto;
import com.servando.homebudget.repository.CreditCardsRepository;
import org.springframework.stereotype.Service;

@Service
public class CreditCardsServiceImpl extends BaseCrudServiceImpl<CreditCardsModel, CreditCardsRepository, CreateCreditCardsRequestDto, UpdateCreditCardsRequestDto> {
    CreditCardsServiceImpl(CreditCardsRepository repository) {
        super(repository);
    }

    public GenericResponseDto<String> createCreditCard(CreateCreditCardsRequestDto request) {
        var toCreate = new CreditCardsModel(
                request.getName(),
                request.getApr(),
                request.getCurrentBalance(),
                request.getDueDay(),
                request.getMinimumPayment()
        );
        return this.create(request, toCreate);
    }

    public GenericResponseDto<String> updateCreditCard(String id, UpdateCreditCardsRequestDto request) {
        var other = this.findOtherById(id);
        var toUpdate = new CreditCardsModel(
                request.getName() == null ? other.getName() : request.getName(),
                request.getApr() == null ? other.getApr() : request.getApr(),
                request.getCurrentBalance() == null ? other.getCurrentBalance() : request.getCurrentBalance(),
                request.getDueDay() == null ? other.getDueDay() : request.getDueDay(),
                request.getMinimumPayment() == null ? other.getMinimumPayment() : request.getMinimumPayment()
        );
        return this.update(id, request, toUpdate);
    }

}
