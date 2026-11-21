package com.servando.homebudget.encryption;

import org.bson.Document;
import org.bson.types.Binary;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterLoadEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeSaveEvent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Encrypts each record's {@link Encrypted} fields as it's written and decrypts them as it's read,
 * for every entity, the budget records and the household's settings alike,
 * so the rest of the API only ever sees plain values. Works on the BSON document between the
 * entity and MongoDB, which covers every repository save and find.
 *
 * <p>Before encrypting the name it stores {@code nameIndex}, the keyed hash the unique-name index
 * and the name lookups use. A value that isn't encrypted yet reads as it is, which is how records
 * from before encryption still load until the startup migration rewrites them.
 */
@Component
public class EncryptingMongoListener extends AbstractMongoEventListener<Object> {

    public static final String NAME_INDEX = "nameIndex";

    private final FieldCipher cipher;
    private final Map<Class<?>, List<String>> encryptedFields = new ConcurrentHashMap<>();

    public EncryptingMongoListener(FieldCipher cipher) {
        this.cipher = cipher;
    }

    @Override
    public void onBeforeSave(BeforeSaveEvent<Object> event) {
        Document document = event.getDocument();
        if (document == null) {
            return;
        }
        String collection = event.getCollectionName();
        Object id = document.get("_id");
        Object householdId = document.get("householdId");
        Object name = document.get("name");
        if (householdId != null && name instanceof String plainName) {
            document.put(NAME_INDEX, new Binary(cipher.nameIndex(householdId.toString(), plainName)));
        }
        for (String field : fieldsOf(event.getSource().getClass())) {
            Object value = document.get(field);
            if (value != null && !FieldCipher.isEncrypted(value)) {
                document.put(field, cipher.encrypt(value, collection, id, field));
            }
        }
    }

    @Override
    public void onAfterLoad(AfterLoadEvent<Object> event) {
        Document document = event.getDocument();
        if (document != null) {
            cipher.decryptDocument(document, event.getCollectionName());
        }
    }

    /** The stored names of a model's encrypted fields, its superclasses' included. */
    List<String> fieldsOf(Class<?> type) {
        return encryptedFields.computeIfAbsent(type, t -> {
            List<String> names = new ArrayList<>();
            for (Class<?> c = t; c != null && c != Object.class; c = c.getSuperclass()) {
                for (java.lang.reflect.Field field : c.getDeclaredFields()) {
                    if (field.isAnnotationPresent(Encrypted.class)) {
                        Field mapped = field.getAnnotation(Field.class);
                        names.add(mapped != null && !mapped.value().isEmpty() ? mapped.value() : field.getName());
                    }
                }
            }
            return List.copyOf(names);
        });
    }
}
