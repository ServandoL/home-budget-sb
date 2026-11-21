package com.servando.homebudget.encryption;

import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FieldCipherTest {

    private static final String KEY = "ZXnlYD1EWjdZQXlQFHCfLkBP6dBJmAlXrGNLTzdRTGg=";
    private final FieldCipher cipher = FieldCipher.fromBase64(KEY);
    private final ObjectId id = new ObjectId();

    @Test
    void valuesComeBackAsTheyWentIn() {
        assertThat(roundTrip(1450.25)).isEqualTo(1450.25);
        assertThat(roundTrip(42)).isEqualTo(42);
        assertThat(roundTrip("Rent")).isEqualTo("Rent");
    }

    @Test
    void theSameValueEncryptsDifferentlyEachTime() {
        assertThat(cipher.encrypt("Rent", "Bills", id, "name").getData())
                .isNotEqualTo(cipher.encrypt("Rent", "Bills", id, "name").getData());
    }

    @Test
    void aValueMovedToAnotherRecordOrFieldWontDecrypt() {
        Binary amount = cipher.encrypt(1500.0, "Bills", id, "amount");
        assertThatThrownBy(() -> cipher.decrypt(amount, "Bills", new ObjectId(), "amount"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> cipher.decrypt(amount, "Bills", id, "name"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void anotherKeyCantDecrypt() {
        Binary amount = cipher.encrypt(1500.0, "Bills", id, "amount");
        FieldCipher other = FieldCipher.fromBase64("k11JNNgX/ifxfaTVLv3V8m+7n8cAXtC69ABXMn+IjeI=");
        assertThatThrownBy(() -> other.decrypt(amount, "Bills", id, "amount")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void nameIndexMatchesWithinAHouseholdOnly() {
        assertThat(cipher.nameIndex("a", "Rent")).isEqualTo(cipher.nameIndex("a", "Rent"));
        assertThat(cipher.nameIndex("a", "Rent")).isNotEqualTo(cipher.nameIndex("b", "Rent"));
        assertThat(cipher.nameIndex("a", "Rent")).isNotEqualTo(cipher.nameIndex("a", "rent"));
    }

    @Test
    void aMissingOrShortKeyStopsStartup() {
        assertThatThrownBy(() -> FieldCipher.fromBase64("")).hasMessageContaining("BUDGET_ENCRYPTION_KEY");
        assertThatThrownBy(() -> FieldCipher.fromBase64("c2hvcnQ=")).hasMessageContaining("32 bytes");
    }

    private Object roundTrip(Object value) {
        return cipher.decrypt(cipher.encrypt(value, "Bills", id, "amount"), "Bills", id, "amount");
    }
}
