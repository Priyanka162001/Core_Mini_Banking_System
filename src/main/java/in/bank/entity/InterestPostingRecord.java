package in.bank.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "interest_posting_records",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_interest_account_period",
            columnNames = {"savings_account_id", "posting_month", "posting_year"}
        )
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterestPostingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "savings_account_id", nullable = false)
    private SavingsAccount savingsAccount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal interestAmount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceBefore;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal annualInterestRate;

    @Column(nullable = false)
    private Integer postingMonth;

    @Column(nullable = false)
    private Integer postingYear;

    @Column(nullable = false)
    private String status;

    // ── Audit fields ─────────────────────────────────────────────────────────

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;           // ✅ NEW

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private Long createdBy;                    // ✅ Long to match your AuditorAware

    @LastModifiedBy
    @Column(name = "updated_by")
    private Long updatedBy;                    // ✅ NEW
}