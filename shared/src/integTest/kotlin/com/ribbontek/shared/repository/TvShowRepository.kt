package com.ribbontek.shared.repository

import com.ribbontek.shared.specdsl.equal
import com.ribbontek.shared.specdsl.`in`
import com.ribbontek.shared.specdsl.like
import com.ribbontek.shared.specdsl.or
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TvShowRepository : CrudRepository<TvShow, Int>, JpaSpecificationExecutor<TvShow>

@Entity
@Table(name = "tv_show")
data class TvShow(
    @Id
    @GeneratedValue
    val id: Int = 0,
    val name: String = "",
    val synopsis: String = "",
    val availableOnNetflix: Boolean = false,
    val releaseDate: String? = null,
    @ManyToMany(cascade = [CascadeType.ALL], fetch = LAZY)
    val starRatings: Set<StarRating> = emptySet()
)

@Entity
@Table(name = "star_rating")
data class StarRating(
    @Id
    @GeneratedValue
    val id: Int = 0,
    val stars: Int = 0
)

// Convenience functions (using the DSL) that make assembling queries more readable and allows for dynamic queries.
// Note: these functions return null for a null input. This means that when included in
// and() or or() they will be ignored as if they weren't supplied.

fun hasName(name: String?): Specification<TvShow>? =
    name?.let {
        TvShow::name.equal(it)
    }

fun availableOnNetflix(available: Boolean?): Specification<TvShow>? =
    available?.let {
        TvShow::availableOnNetflix.equal(it)
    }

fun hasReleaseDateIn(releaseDates: List<String>?): Specification<TvShow>? =
    releaseDates?.let {
        TvShow::releaseDate.`in`(releaseDates)
    }

fun hasKeywordIn(keywords: List<String>?): Specification<TvShow>? =
    keywords?.let {
        or(keywords.map(::hasKeyword))
    }

fun hasKeyword(keyword: String?): Specification<TvShow>? =
    keyword?.let {
        TvShow::synopsis.like("%$keyword%")
    }
