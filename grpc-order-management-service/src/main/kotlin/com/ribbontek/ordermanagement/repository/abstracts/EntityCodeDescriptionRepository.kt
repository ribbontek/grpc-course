package com.ribbontek.ordermanagement.repository.abstracts

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.query.Param

@NoRepositoryBean
interface EntityCodeDescriptionRepository<T : AbstractEntityCodeDescription> : JpaRepository<T, Long> {
    @Query("select e from #{#entityName} e where lower(e.code) = lower(:code) and e.deleted = false")
    fun findByCode(
        @Param("code") code: String
    ): T?

    @Query("select e from #{#entityName} e where lower(e.code) = lower(:code)")
    fun findByCodeIncludingDeleted(
        @Param("code") code: String
    ): List<T>
}
