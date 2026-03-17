package in.bank.entity;

import in.bank.entity.GenderType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_profile_id")
    private Long id;

    @OneToOne
    @JoinColumn(name = "customer_id", nullable = false, unique = true)
    private AppUser customer;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    private GenderType gender;

    @Column(name = "permanent_address_line")
    private String permanentAddressLine;

    @Column(name = "current_address_line")
    private String currentAddressLine;

    private String city;

    private String state;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}