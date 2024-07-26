package com.ribbontek.ordermanagement.grpc.model.validator

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER)
@Constraint(validatedBy = [NotBlankOrEmptyValidator::class])
annotation class NotBlankOrEmpty(
    val message: String = "must not be blank or empty",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class NotBlankOrEmptyValidator : ConstraintValidator<NotBlankOrEmpty, String?> {
    override fun isValid(
        value: String?,
        context: ConstraintValidatorContext
    ): Boolean {
        return value?.isNotBlank() ?: true
    }
}
