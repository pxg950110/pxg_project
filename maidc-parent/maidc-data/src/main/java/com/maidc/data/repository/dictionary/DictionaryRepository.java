package com.maidc.data.repository.dictionary;

import com.maidc.common.jpa.base.BaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;

/**
 * 医学字典仓储基接口：固化未删数据的按主键查询与 Specification 条件查询能力，
 * 供五套字典（诊断/药品/检验/检查/收费）仓储继承。
 */
@NoRepositoryBean
public interface DictionaryRepository<E extends BaseEntity> extends JpaRepository<E, Long>, JpaSpecificationExecutor<E> {

    Optional<E> findByIdAndIsDeletedFalse(Long id);
}
