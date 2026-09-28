package com.k8s.costoptimizer.infrastructure.persistence.adapter;

import com.k8s.costoptimizer.domain.model.Recommendation;
import com.k8s.costoptimizer.domain.model.RecommendationStatus;
import com.k8s.costoptimizer.domain.repository.RecommendationRepository;
import com.k8s.costoptimizer.infrastructure.persistence.entity.RecommendationEntity;
import com.k8s.costoptimizer.infrastructure.persistence.mapper.RecommendationMapper;
import com.k8s.costoptimizer.infrastructure.persistence.repository.SpringDataRecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Persistence adapter that acts as a bridge between the clean domain repository port
 * and Spring Data JPA infrastructure.
 */
@Component
@RequiredArgsConstructor
public class RecommendationRepositoryAdapter implements RecommendationRepository {

    private final SpringDataRecommendationRepository jpaRepo;
    private final RecommendationMapper mapper;

    @Override
    public Recommendation save(Recommendation recommendation) {
        RecommendationEntity entity = mapper.toEntity(recommendation);
        RecommendationEntity saved = jpaRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<Recommendation> saveAll(List<Recommendation> recommendations) {
        List<RecommendationEntity> entities = mapper.toEntityList(recommendations);
        List<RecommendationEntity> saved = jpaRepo.saveAll(entities);
        return mapper.toDomainList(saved);
    }

    @Override
    public Optional<Recommendation> findById(Long id) {
        return jpaRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Recommendation> findAll() {
        return mapper.toDomainList(jpaRepo.findAll());
    }

    @Override
    public List<Recommendation> findByStatus(RecommendationStatus status) {
        return mapper.toDomainList(jpaRepo.findByStatus(status));
    }

    @Override
    public List<Recommendation> findByNamespace(String namespace) {
        return mapper.toDomainList(jpaRepo.findByNamespace(namespace));
    }

    @Override
    public void deleteById(Long id) {
        jpaRepo.deleteById(id);
    }

    @Override
    public void deleteAll() {
        jpaRepo.deleteAll();
    }
}
