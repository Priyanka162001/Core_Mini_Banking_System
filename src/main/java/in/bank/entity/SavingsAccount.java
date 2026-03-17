package in.bank.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "savings_accounts")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Auto generated account number
    @Column(name = "account_number", nullable = false, unique = true)
    private String accountNumber;

    // Linked user
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    // Linked savings product
    @ManyToOne
    @JoinColumn(name = "savings_product_id", nullable = false)
    private SavingsProduct savingsProduct;

    // Current balance
    @Column(name = "current_balance_amount", nullable = false)
    private BigDecimal currentBalanceAmount;

    // Lifecycle status
    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false)
    private AccountLifecycleStatus accountStatus;

    // ================= AUDIT FIELDS =================

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}