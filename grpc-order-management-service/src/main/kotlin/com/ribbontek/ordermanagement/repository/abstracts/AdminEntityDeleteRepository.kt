package com.ribbontek.ordermanagement.repository.abstracts

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.NoRepositoryBean

@NoRepositoryBean
interface AdminEntityDeleteRepository<T : AbstractAdminEntityDelete> : JpaRepository<T, Long> {
    @Query("select e from #{#entityName} e where e.id = :id and e.deleted = false")
    override fun getReferenceById(id: Long): T

    @Query("select e from #{#entityName} e where e.deleted = false")
    override fun findAll(): List<T>

    @Query("select e from #{#entityName} e")
    fun findAllIncludingDeleted(): List<T>
}
