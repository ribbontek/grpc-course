package com.ribbontek.ordermanagement.repository.product

import com.ribbontek.ordermanagement.exception.NotFoundException
import com.ribbontek.ordermanagement.repository.abstracts.EntityCodeDescriptionRepository
import com.ribbontek.shared.specdsl.like
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface CategoryRepository : EntityCodeDescriptionRepository<CategoryEntity>, JpaSpecificationExecutor<CategoryEntity>

fun CategoryRepository.expectOneByCode(code: String): CategoryEntity {
    return findByCode(code) ?: throw NotFoundException("Could not find Category with code: $code")
}

object CategoryEntitySpecs {
    fun hasCodeLike(title: String?): Specification<CategoryEntity>? =
        title?.let {
            CategoryEntity::code.like(it)
        }

    fun hasDescriptionLike(description: String?): Specification<CategoryEntity>? =
        description?.let {
            CategoryEntity::description.like(it)
        }
}
