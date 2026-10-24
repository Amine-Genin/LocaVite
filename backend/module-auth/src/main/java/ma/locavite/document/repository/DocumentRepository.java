package ma.locavite.document.repository;

import ma.locavite.document.domain.Document;
import ma.locavite.document.domain.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByUserIdOrderByUploadedAtDesc(Long userId);

    Optional<Document> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndType(Long userId, DocumentType type);
}
