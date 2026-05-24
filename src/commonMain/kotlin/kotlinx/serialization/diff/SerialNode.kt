package kotlinx.serialization.diff

import kotlinx.serialization.descriptors.SerialDescriptor

/**
 * Represents a walkable node in the serialized AST of a Kotlin class.
 */
public sealed interface SerialNode {
    public val descriptor: SerialDescriptor

    /**
     * Helper to retrieve a displayable string representation of this node.
     */
    public fun toDisplayString(): String
}

/**
 * Represents a primitive value (e.g., Int, Double, String, Char, Boolean, Enum value name, etc.)
 */
public data class PrimitiveNode(
    override val descriptor: SerialDescriptor,
    val value: Any?
) : SerialNode {
    override fun toDisplayString(): String = value?.toString() ?: "null"
}

/**
 * Represents an explicitly serialized null value.
 */
public data class NullNode(
    override val descriptor: SerialDescriptor
) : SerialNode {
    override fun toDisplayString(): String = "null"
}

/**
 * Represents a structured object (e.g., a standard Kotlin class, object, etc.)
 * mapping property names to their serialized values.
 */
public data class StructureNode(
    override val descriptor: SerialDescriptor,
    val fields: Map<String, SerialNode>
) : SerialNode {
    override fun toDisplayString(): String = descriptor.serialName
}

/**
 * Represents a collection of elements (e.g., List, Set, Array).
 */
public data class CollectionNode(
    override val descriptor: SerialDescriptor,
    val elements: List<SerialNode>
) : SerialNode {
    override fun toDisplayString(): String = "List(size=${elements.size})"
}

/**
 * Represents a Map, preserving ordered key-value node pairs.
 */
public data class MapNode(
    override val descriptor: SerialDescriptor,
    val entries: List<Pair<SerialNode, SerialNode>>
) : SerialNode {
    override fun toDisplayString(): String = "Map(size=${entries.size})"
}
