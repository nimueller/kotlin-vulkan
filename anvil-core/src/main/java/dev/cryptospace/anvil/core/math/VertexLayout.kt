package dev.cryptospace.anvil.core.math

import kotlin.reflect.KProperty1

interface VertexLayout<V : Vertex> {

    val byteSize: Int

    fun offset(property: KProperty1<V, *>): Int

    fun getAttributeDescriptions(binding: Int): List<AttributeDescription>
}
