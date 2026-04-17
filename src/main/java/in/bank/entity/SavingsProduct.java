package in.bank.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "savings_products")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_code", nullable = false, unique = true, length = 50)
    private String productCode;

    @Column(name = "product_name", nullable = false, unique = true, length = 100)
    private String productName;

    @Column(name = "interest_rate_percent", nullable = false)
    private BigDecimal interestRatePercent;

    @Column(name = "minimum_opening_balance_amount", nullable = false)
    private BigDecimal minimumOpeningBalanceAmount;

    @Column(name = "minimum_maintaining_balance_amount", nullable = false)
    private BigDecimal minimumMaintainingBalanceAmount;

    // ENUM stored as VARCHAR
    @Enumerated(EnumType.STRING)
    @Column(name = "interest_application_frequency_code", nullable = false)
    private InterestApplicationFrequency interestApplicationFrequencyCode;

    // ENUM stored as VARCHAR
    @Enumerated(EnumType.STRING)
    @Column(name = "product_status", nullable = false)
    private ProductStatus productStatus;

    @Column(name = "effective_from_date", nullable = false)
    private LocalDate effectiveFromDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "min_age", nullable = false)
    private Integer minAge;

    @Column(name = "max_age", nullable = false)
    private Integer maxAge;

    // ================= AUDIT FIELDS =================

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private Long  updatedBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}