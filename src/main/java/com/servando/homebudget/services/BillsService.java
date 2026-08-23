package com.servando.homebudget.services;

import com.servando.homebudget.exceptions.RecordAlreadyExistsException;
import com.servando.homebudget.exceptions.RecordNotFoundException;
import com.servando.homebudget.models.database.BillsModel;
import com.servando.homebudget.models.dto.CreateBillsRequestDto;
import com.servando.homebudget.models.dto.GenericResponseDto;
import com.servando.homebudget.models.dto.UpdateBillsRequestDto;
import com.servando.homebudget.repository.BillsRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.bson.types.ObjectId;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class BillsService {
    private final BillsRepository billsRepository;

    BillsService(BillsRepository billsRepository) {
        this.billsRepository = billsRepository;
    }

    public GenericResponseDto<List<BillsModel>> getBills() {
        var results = billsRepository.findAll();
        return new GenericResponseDto<>(
                true,
                "Successfully retrieved bills",
                results
        );
    }

    public GenericResponseDto<String> createBill(@NotNull CreateBillsRequestDto request) {
        billsRepository.findByName(request.name()).ifPresent(other -> {
            throw new RecordAlreadyExistsException(request.name());
        });
        var now = Instant.now();
        var toCreate = new BillsModel(new ObjectId().toHexString(), request.name(), request.amount(), request.dueDay(), request.category(), now, now);
        var result = billsRepository.save(toCreate);
        return new GenericResponseDto<>(true, "Record created", result.id());
    }

    public GenericResponseDto<String> updateBill(@NotBlank String id, @NotNull UpdateBillsRequestDto toUpdate) {
        var existing = billsRepository.findById(id)
                .orElseThrow(() -> new RecordNotFoundException(id));

        // Optional: prevent duplicate names if name can change
        if (!existing.name().equals(toUpdate.name())) {
            billsRepository.findByName(toUpdate.name())
                    .ifPresent(other -> {
                        if (!other.id().equals(id)) {
                            throw new RecordAlreadyExistsException(toUpdate.name());
                        }
                    });
        }

        var updated = new BillsModel(
                existing.id(),
                toUpdate.name() != null ? toUpdate.name() : existing.name(),
                toUpdate.amount() != null ? toUpdate.amount() : existing.amount(),
                toUpdate.dueDay() != 0 ? toUpdate.dueDay() : existing.dueDay(),
                toUpdate.category() != null ? toUpdate.category() : existing.category(),
                existing.createdAt() != null ? existing.createdAt() : Instant.now(),
                Instant.now()
        );

        billsRepository.save(updated);

        return new GenericResponseDto<>(true, "Record updated", updated.id());
    }

    public GenericResponseDto<String> deleteBill(@NotBlank String id) {
        billsRepository.deleteById(id);
        return new GenericResponseDto<>(true, "Record deleted", id);
    }
}
