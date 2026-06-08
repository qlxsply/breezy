package com.corwin.clinic.infrastructure.persistence;

import com.corwin.clinic.domain.model.ItemCategory;
import com.corwin.clinic.domain.model.ItemSku;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/2/8
 */
public interface ItemSkuJpaRepository extends JpaRepository<ItemSku, Long> {
    Optional<ItemSku> findBySpuIdAndDisplayName(Long spuId, String displayName);

    List<ItemSku> findBySpuId(Long spuId);

    @Query("""
            select sku from ItemSku sku
            join ItemSpu spu on sku.spuId = spu.id
            where (:category is null or spu.category = :category)
              and (:nameLike is null or lower(sku.displayName) like lower(concat('%', :nameLike, '%')))
              and (:enabled is null or sku.enabled = :enabled)
            """)
    Page<ItemSku> page(@Param("category") ItemCategory category,
            @Param("nameLike") String nameLike,
            @Param("enabled") Boolean enabled,
            Pageable pageable);
}
