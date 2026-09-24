package com.maidc.data.repository;

import com.maidc.data.entity.KnowledgeItemEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface KnowledgeItemRepository extends JpaRepository<KnowledgeItemEntity, Long> {

    Page<KnowledgeItemEntity> findByIsDeletedFalse(Pageable pageable);

    Page<KnowledgeItemEntity> findByCategoryIdAndIsDeletedFalse(Long categoryId, Pageable pageable);

    Page<KnowledgeItemEntity> findByCategoryIdAndItemTypeAndIsDeletedFalse(Long categoryId, String itemType, Pageable pageable);

    Page<KnowledgeItemEntity> findByItemTypeAndIsDeletedFalse(String itemType, Pageable pageable);

    @Query("SELECT ki FROM KnowledgeItemEntity ki WHERE ki.isDeleted = false " +
            "AND ki.categoryId = :categoryId " +
            "AND ki.title LIKE CONCAT('%', :keyword, '%')")
    Page<KnowledgeItemEntity> searchByCategoryAndKeyword(@Param("categoryId") Long categoryId,
                                                          @Param("keyword") String keyword,
                                                          Pageable pageable);

    @Query("SELECT ki FROM KnowledgeItemEntity ki WHERE ki.isDeleted = false " +
            "AND ki.itemType = :itemType " +
            "AND ki.title LIKE CONCAT('%', :keyword, '%')")
    Page<KnowledgeItemEntity> searchByTypeAndKeyword(@Param("itemType") String itemType,
                                                      @Param("keyword") String keyword,
                                                      Pageable pageable);

    @Query("SELECT ki FROM KnowledgeItemEntity ki WHERE ki.isDeleted = false " +
            "AND ki.categoryId = :categoryId " +
            "AND ki.itemType = :itemType " +
            "AND ki.title LIKE CONCAT('%', :keyword, '%')")
    Page<KnowledgeItemEntity> searchByCategoryAndTypeAndKeyword(@Param("categoryId") Long categoryId,
                                                                 @Param("itemType") String itemType,
                                                                 @Param("keyword") String keyword,
                                                                 Pageable pageable);

    @Query("SELECT ki FROM KnowledgeItemEntity ki WHERE ki.isDeleted = false " +
            "AND ki.title LIKE CONCAT('%', :keyword, '%')")
    Page<KnowledgeItemEntity> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
