package in.bank.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "idempotency_keys")
@Data
public class IdempotencyKey {

    @Id
    @Column(name = "idempotency_key")  // ← For H2 tests  // ← Added backticks to escape the reserved word
    private String key;

    private String response; // store txnId

    private Integer statusCode;

    private LocalDateTime createdAt;
}