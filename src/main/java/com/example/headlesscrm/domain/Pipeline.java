package com.example.headlesscrm.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pipelines")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pipeline {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID")
    private UUID id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false)
    private String name;

    @Size(max = 500)
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private boolean isDefault = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Organization organization;

    @OneToMany(mappedBy = "pipeline", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<PipelineStage> stages = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public void addStage(PipelineStage stage) {
        stages.add(stage);
        stage.setPipeline(this);
    }

    public void removeStage(PipelineStage stage) {
        stages.remove(stage);
        stage.setPipeline(null);
    }
}
