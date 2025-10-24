package com.teixeirah.withdrawals.domain.wallet.withdraw;

import com.teixeirah.withdrawals.domain.value.objects.Recipient;
import com.teixeirah.withdrawals.domain.value.objects.Account;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RecipientTest {

    @Test
    void shouldCreateRecipientWithValidData() {
        Account account = new Account("987654321", "123456789");
        Recipient recipient = new Recipient("John", "Doe", "123456789", account);

        assertEquals("John", recipient.firstName());
        assertEquals("Doe", recipient.lastName());
        assertEquals("123456789", recipient.nationalId());
        assertEquals("123456789", recipient.account().routingNumber());
        assertEquals("987654321", recipient.account().accountNumber());
    }

    @Test
    void shouldFailToCreateRecipientWithNullFirstName() {
        Account account = new Account("987654321", "123456789");
        assertThrows(IllegalArgumentException.class, () -> new Recipient(null, "Doe", "123456789", account));
    }

    @Test
    void shouldFailToCreateRecipientWithBlankFirstName() {
        Account account = new Account("987654321", "123456789");
        assertThrows(IllegalArgumentException.class, () -> new Recipient("", "Doe", "123456789", account));
    }

    @Test
    void shouldFailToCreateRecipientWithNullLastName() {
        Account account = new Account("987654321", "123456789");
        assertThrows(IllegalArgumentException.class, () -> new Recipient("John", null, "123456789", account));
    }

    @Test
    void shouldFailToCreateRecipientWithBlankLastName() {
        Account account = new Account("987654321", "123456789");
        assertThrows(IllegalArgumentException.class, () -> new Recipient("John", "", "123456789", account));
    }

    @Test
    void shouldFailToCreateRecipientWithNullAccount() {
        assertThrows(IllegalArgumentException.class, () -> new Recipient("John", "Doe", "123456789", null));
    }

    @Test
    void shouldFailToCreateRecipientWithInvalidAccount() {
        assertThrows(IllegalArgumentException.class, () -> new Account("", "123456789"));
        assertThrows(IllegalArgumentException.class, () -> new Account("987654321", ""));
    }

    @Test
    void shouldConsiderTwoRecipientsWithSameDataAsEqual() {
        Account account1 = new Account("987654321", "123456789");
        Account account2 = new Account("987654321", "123456789");
        Recipient r1 = new Recipient("John", "Doe", "123456789", account1);
        Recipient r2 = new Recipient("John", "Doe", "123456789", account2);

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void shouldConsiderTwoRecipientsWithDifferentDataAsNotEqual() {
        Account account1 = new Account("987654321", "123456789");
        Account account2 = new Account("987654322", "123456789");
        Recipient r1 = new Recipient("John", "Doe", "123456789", account1);
        Recipient r2 = new Recipient("Jane", "Doe", "123456789", account2);

        assertNotEquals(r1, r2);
    }
}
