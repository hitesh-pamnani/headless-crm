package com.example.headlesscrm.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "deals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Deal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID")
    private UUID id;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false)
    private String title;

    @Builder.Default
    @Column(precision = 15, scale = 2)
    private BigDecimal value = BigDecimal.ZERO;

    @Builder.Default
    @Column(length = 3)
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private DealStatus status = DealStatus.OPEN;

    private LocalDate expectedCloseDate;

    private LocalDateTime actualCloseDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pipeline_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Pipeline pipeline;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private PipelineStage stage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Contact contact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User owner;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_fields", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> customFields = new HashMap<>();

    @Builder.Default
    @Column(nullable = false)
    private boolean deleted = false;

    private LocalDateTime deletedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
