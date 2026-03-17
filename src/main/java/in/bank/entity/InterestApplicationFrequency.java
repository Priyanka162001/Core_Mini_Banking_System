package in.bank.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum InterestApplicationFrequency {
    MONTHLY,
    QUARTERLY,
    YEARLY;

    // Safe JSON parsing
    @JsonCreator
    public static InterestApplicationFrequency fromValue(String value) {
        try {
            return InterestApplicationFrequency.valueOf(value.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "Invalid interestApplicationFrequencyCode. Allowed values: MONTHLY, QUARTERLY, YEARLY"
            );
        }
    }
}