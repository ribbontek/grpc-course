package com.ribbontek.ordermanagement.context

/**
 * The no-arg plugin uses this annotation to create default empty constructors in Java
 * for classes that the annotation is annotated against.
 *
 * For this app, the RibbontekGrpcService annotation uses this to dynamically create empty instances to retrieve Service Definitions
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class NoArg
