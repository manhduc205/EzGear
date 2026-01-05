package com.manhduc205.ezgear.elasticsearch.repositories;

import com.manhduc205.ezgear.elasticsearch.documents.ProductDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, Long> {
    Page<ProductDocument> findByNameContaining(String name, Pageable pageable);
}