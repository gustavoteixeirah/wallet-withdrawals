package com.teixeirah.withdrawals.domain.value.objects;

public record Recipient(
        String firstName,
        String lastName,
        String nationalId,
        Account account
) {
    public Recipient {
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("Recipient's first name is required.");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("Recipient's last name is required.");
        }
        if (nationalId == null || nationalId.isBlank()) {
            throw new IllegalArgumentException("Recipient's national identification is required.");
        }
        if (account == null) {
            throw new IllegalArgumentException("Recipient's account is required.");
        }
    }
}