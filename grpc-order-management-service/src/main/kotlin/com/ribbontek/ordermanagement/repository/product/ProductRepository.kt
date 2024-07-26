package com.ribbontek.ordermanagement.repository.product

import com.ribbontek.ordermanagement.exception.NotFoundException
import com.ribbontek.ordermanagement.repository.abstracts.AdminEntityDeleteRepository
import com.ribbontek.shared.specdsl.equal
import com.ribbontek.shared.specdsl.get
import com.ribbontek.shared.specdsl.greaterThan
import com.ribbontek.shared.specdsl.greaterThanOrEqualTo
import com.ribbontek.shared.specdsl.isFalse
import com.ribbontek.shared.specdsl.isNotNull
import com.ribbontek.shared.specdsl.isNull
import com.ribbontek.shared.specdsl.join
import com.ribbontek.shared.specdsl.lessThanOrEqualTo
import com.ribbontek.shared.specdsl.like
import com.ribbontek.shared.specdsl.or
import com.ribbontek.shared.specdsl.where
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.JoinType.INNER
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.util.UUID

@Repository
interface ProductRepository : AdminEntityDeleteRepository<ProductEntity>, JpaSpecificationExecutor<ProductEntity> {
    fun findByRequestId(requestId: UUID): ProductEntity?

    @Query(
        """
        select product from ProductEntity product 
        left join fetch product.discount discount
        where product.id in (:ids)
    """
    )
    fun findEagerByIdIn(ids: List<Long>): List<ProductEntity>
}

fun ProductRepository.expectOneByRequestId(requestId: UUID): ProductEntity {
    return findByRequestId(requestId) ?: throw NotFoundException("Could not find product with requestId $requestId")
}

object ProductEntitySpecs {
    fun hasTitle(title: String?): Specification<ProductEntity>? =
        title?.let {
            ProductEntity::title.equal(it)
        }

    fun hasTitleLike(title: String?): Specification<ProductEntity>? =
        title?.let {
            ProductEntity::title.like("%$it%")
        }

    fun hasDescriptionLike(description: String?): Specification<ProductEntity>? =
        description?.let {
            ProductEntity::description.like("%$it%")
        }

    fun hasDeleted(deleted: Boolean?): Specification<ProductEntity>? =
        deleted?.let {
            ProductEntity::deleted.equal(it)
        }

    fun hasStock(hasStock: Boolean?): Specification<ProductEntity>? =
        hasStock?.let {
            if (it) ProductEntity::quantity.greaterThan(0) else ProductEntity::quantity.equal(0)
        }

    fun hasDiscount(hasDiscount: Boolean?): Specification<ProductEntity>? =
        hasDiscount?.let {
            if (it) ProductEntity::discount.isNotNull() else ProductEntity::discount.isNull()
        }

    fun hasPriceRangeStart(priceRangeStart: BigDecimal?): Specification<ProductEntity>? =
        priceRangeStart?.let {
            ProductEntity::price.greaterThanOrEqualTo(priceRangeStart)
        }

    fun hasPriceRangeEnd(priceRangeEnd: BigDecimal?): Specification<ProductEntity>? =
        priceRangeEnd?.let {
            ProductEntity::price.lessThanOrEqualTo(priceRangeEnd)
        }

    fun hasCategoryCode(categoryCode: String?): Specification<ProductEntity>? =
        categoryCode?.let {
            where {
                it.joins.firstOrNull { join -> join.attribute.name == "category" && join.joinType.name == "INNER" }
                    ?.get(CategoryEntity::code)?.let { equal(it, categoryCode) }
                    ?: equal(it.join(ProductEntity::category).get(CategoryEntity::code), categoryCode)
            }
        }

    fun joinCategory(): Specification<ProductEntity> =
        Specification { root, criteriaQuery, criteriaBuilder ->
            if (!criteriaQuery.isCount()) {
                root.fetch<ProductEntity, CategoryEntity>(ProductEntity::category.name, INNER)
            }
            criteriaBuilder.conjunction()
        }

    fun hasKeywordIn(keywords: List<String>?): Specification<ProductEntity>? =
        keywords?.let {
            or(keywords.map(::hasKeyword))
        }

    fun hasKeyword(keyword: String?): Specification<ProductEntity>? =
        keyword?.let {
            or(
                ProductEntity::description.like("%$keyword%"),
                ProductEntity::title.like("%$keyword%")
            )
        }

    fun notDeleted(): Specification<ProductEntity> = ProductEntity::deleted.isFalse()

    private fun CriteriaQuery<*>.isCount(): Boolean =
        resultType == Long::class.javaObjectType || resultType == Long::class.javaPrimitiveType
}
