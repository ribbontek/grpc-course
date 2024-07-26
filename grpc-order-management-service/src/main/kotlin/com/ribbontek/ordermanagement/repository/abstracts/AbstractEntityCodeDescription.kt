package com.ribbontek.ordermanagement.repository.abstracts

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass

@MappedSuperclass
abstract class AbstractEntityCodeDescription : AbstractEntityDelete() {
    @Column(nullable = false, length = 50)
    var code: String? = null

    @Column(length = 1000)
    var description: String? = null
}
