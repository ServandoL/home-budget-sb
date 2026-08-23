package com.servando.homebudget.exceptions;

import lombok.Getter;

@Getter
public class RecordNotFoundException extends RuntimeException {
    private final String id;

    public RecordNotFoundException(String id) {
        super("Record with id '" + id + "' was not found");
        this.id = id;
    }
}
