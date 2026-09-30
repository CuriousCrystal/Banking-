package com.amc.bfsi.repository;

import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Base class of every repository (see JpaConfig).
 *
 * SimpleJpaRepository.findById uses em.find(), and Hibernate's tenant filter does NOT apply to em.find():
 * a bank could read - or, through update/delete, change - another bank's row just by guessing its id.
 * A query is filtered, so findById (and deleteById, which calls it) goes through one.
 */
public class TenantSafeRepository<T, ID> extends SimpleJpaRepository<T, ID> {

    private final EntityManager em;
    private final JpaEntityInformation<T, ?> entityInformation;

    public TenantSafeRepository(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.em = entityManager;
        this.entityInformation = entityInformation;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<T> findById(ID id) {
        String jpql = "select e from " + entityInformation.getEntityName()
                + " e where e." + entityInformation.getRequiredIdAttribute().getName() + " = :id";
        return em.createQuery(jpql, entityInformation.getJavaType())
                .setParameter("id", id)
                .getResultStream()
                .findFirst();
    }
}
