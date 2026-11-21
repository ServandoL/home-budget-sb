package com.servando.homebudget.encryption;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Stored encrypted with AES-256-GCM: MongoDB holds an unreadable binary, and the API reads it back
 * as the original value. Mongo can't filter or sort on it, so sort such fields in Java.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Encrypted {
}
