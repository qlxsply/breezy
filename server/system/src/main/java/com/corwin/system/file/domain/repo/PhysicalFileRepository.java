package com.corwin.system.file.domain.repo;

import com.corwin.system.file.domain.model.PhysicalFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/**
 * @author Corwin 2026/2/23
 */
public interface PhysicalFileRepository extends JpaRepository<PhysicalFile, String> {

    Optional<PhysicalFile> findByHash(String hash);

    @Modifying
    @Query("update PhysicalFile p set p.refCount = p.refCount + 1 where p.id = ?1")
    int incrementRefCount(String id);

    @Modifying
    @Query("update PhysicalFile p set p.refCount = p.refCount - 1 where p.id = ?1 and p.refCount > 0")
    int decrementRefCount(String id);
}
