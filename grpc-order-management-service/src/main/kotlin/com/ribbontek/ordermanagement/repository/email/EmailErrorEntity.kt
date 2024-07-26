package com.ribbontek.ordermanagement.repository.email

import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Generated
import org.hibernate.generator.EventType

@Entity
@Table(name = "email_error")
@AttributeOverride(name = "id", column = Column(name = "email_error_id"))
class EmailErrorEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Generated(event = [EventType.INSERT])
    @Column(insertable = false, updatable = false, nullable = false)
    val id: Long? = null,
    @Column(length = 1000)
    val errorMessage: String
)
