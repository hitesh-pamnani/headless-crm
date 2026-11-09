package com.example.headlesscrm.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pipeline_stages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PipelineStage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID")
    private UUID id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int displayOrder;

    @Builder.Default
    private int probability = 0;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private StageCategory category = StageCategory.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pipeline_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Pipeline pipeline;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
