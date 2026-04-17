package in.bank.entity;

public enum PostingResult {
    POSTED,      // successfully inserted
    DUPLICATE,   // already exists for this period
    SKIPPED      // zero interest calculated
}