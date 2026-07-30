package com.corwin.system.file.domain.repo;

import com.corwin.system.file.domain.model.PhysicalFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/**
 * Repository interface for {@link PhysicalFile} entity.
 * Provides data access for physical file records, including hash-based lookup
 * and reference count management.
 *
 * @author Corwin 2026/2/23
 */
public interface PhysicalFileRepository extends JpaRepository<PhysicalFile, String> {

    /**
     * Finds a physical file by its content hash.
     */
    Optional<PhysicalFile> findByHash(String hash);

    /**
     * Atomically increments the reference count of a physical file.
     *
     * @return the number of affected rows
     */
    @Modifying
    @Query("update PhysicalFile p set p.refCount = p.refCount + 1 where p.id = ?1")
    int incrementRefCount(String id);

    /**
     * Atomically decrements the reference count of a physical file (minimum 0).
     *
     * @return the number of affected rows
     */
    @Modifying
    @Query("update PhysicalFile p set p.refCount = p.refCount - 1 where p.id = ?1 and p.refCount > 0")
    int decrementRefCount(String id);
}
