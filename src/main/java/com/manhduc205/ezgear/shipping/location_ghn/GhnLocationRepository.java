package com.manhduc205.ezgear.shipping.location_ghn;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GhnLocationRepository extends JpaRepository<GhnLocation, String> {
    List<GhnLocation> findByType(GhnLocation.Type type);
    List<GhnLocation> findByParentId(String parentId);
    Optional<GhnLocation> findByTypeAndId( GhnLocation.Type type,String id);
    List<GhnLocation> findAllByType(GhnLocation.Type type);
}

