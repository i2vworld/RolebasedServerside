package com.world.i2v.i2vauthapi.repository;

import com.world.i2v.i2vauthapi.entity.Territory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TerritoryRepository extends JpaRepository<Territory, Long> {
    List<Territory> findBySquadId(Long squadId);
    List<Territory> findByIsActive(Boolean isActive);
    boolean existsByNameAndSquadId(String name, Long squadId);
}
