package com.servando.homebudget.encryption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class EncryptionConfiguration {

    /** Fails startup without a valid key, rather than writing anything in plain text. */
    @Bean
    FieldCipher fieldCipher(@Value("${budget.encryption-key:}") String key) {
        return FieldCipher.fromBase64(key);
    }
}
