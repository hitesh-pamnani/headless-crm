package com.example.headlesscrm.repository;

import com.example.headlesscrm.domain.Pipeline;
import com.example.headlesscrm.domain.PipelineStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PipelineStageRepository extends JpaRepository<PipelineStage, UUID> {
    Optional<PipelineStage> findByIdAndPipeline(UUID id, Pipeline pipeline);
    List<PipelineStage> findAllByPipelineOrderByDisplayOrderAsc(Pipeline pipeline);
}
