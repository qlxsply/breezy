package com.corwin.clinic.infrastructure.persistence;

import com.corwin.clinic.domain.model.ItemCategory;
import com.corwin.clinic.domain.model.ItemSpu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * @author Corwin 2026/2/8
 */
public interface ItemSpuJpaRepository extends JpaRepository<ItemSpu, Long> {
    Optional<ItemSpu> findByCategoryAndName(ItemCategory category, String name);
}
