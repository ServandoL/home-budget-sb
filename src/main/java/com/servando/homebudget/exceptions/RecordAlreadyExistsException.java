package com.servando.homebudget.exceptions;

import lombok.Getter;

@Getter
public class RecordAlreadyExistsException extends RuntimeException {
    private final String name;

    public RecordAlreadyExistsException(String name) {
        super("Record with name '" + name + "' already exists");
        this.name = name;
    }
}
