package kotlinx.serialization.diff

import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

/**
 * Strategy to use when diffing lists / collections.
 */
public enum class ListDiffStrategy {
    /**
     * Diffs lists element-by-element by index. Useful when the order is fixed
     * or when you want to see detailed property-level modifications for elements at the same position.
     */
    INDEX_BY_INDEX,

    /**
     * Diffs lists using Longest Common Subsequence (LCS) / Myers' diff algorithm.
     * Identifies items that were inserted or deleted to minimize changes.
     */
    LCS
}

/**
 * Configuration options for the diffing process.
 */
public data class DiffConfig(
    val listStrategy: ListDiffStrategy = ListDiffStrategy.INDEX_BY_INDEX,
    val serializersModule: SerializersModule = EmptySerializersModule()
)

/**
 * Represents a single path segment in the object tree.
 */
public sealed interface PathSegment {
    /**
     * Represents a property on a class/object.
     */
    public data class Property(val name: String) : PathSegment {
        override fun toString(): String = name
    }

    /**
     * Represents an index within a list or array.
     */
    public data class Index(val index: Int) : PathSegment {
        override fun toString(): String = "[$index]"
    }

    /**
     * Represents a key within a Map.
     */
    public data class MapKey(val keyString: String) : PathSegment {
        override fun toString(): String = "{$keyString}"
    }
}

/**
 * Helper function to format a path of [PathSegment]s into a standard reference string.
 * Example: `user.roles[0].name` or `attributes{theme}`.
 */
public fun formatPath(path: List<PathSegment>): String {
    if (path.isEmpty()) return "<root>"
    val sb = StringBuilder()
    for (i in path.indices) {
        when (val seg = path[i]) {
            is PathSegment.Property -> {
                if (sb.isNotEmpty()) sb.append(".")
                sb.append(seg.name)
            }
            is PathSegment.Index -> {
                sb.append("[").append(seg.index).append("]")
            }
            is PathSegment.MapKey -> {
                sb.append("{").append(seg.keyString).append("}")
            }
        }
    }
    return sb.toString()
}

/**
 * Represents a change identified between two objects.
 */
public sealed interface Diff {
    public val path: List<PathSegment>

    /**
     * Formats this diff into a clear, human-readable sentence.
     */
    public fun toStatement(): String
}

/**
 * Indicates that a property value changed between primitives (or nulls).
 */
public data class PropertyValueChanged(
    override val path: List<PathSegment>,
    val left: Any?,
    val right: Any?,
    val leftType: String,
    val rightType: String
) : Diff {
    override fun toStatement(): String {
        val pathStr = formatPath(path)
        val leftVal = if (left == null) "null" else "'$left'"
        val rightVal = if (right == null) "null" else "'$right'"
        return "Property '$pathStr' changed from $leftVal to $rightVal"
    }
}

/**
 * Indicates that the structure types did not match (e.g., left is a class, right is a list).
 */
public data class TypeMismatch(
    override val path: List<PathSegment>,
    val leftType: String,
    val rightType: String,
    val leftValue: String,
    val rightValue: String
) : Diff {
    override fun toStatement(): String {
        val pathStr = formatPath(path)
        return "Type mismatch at '$pathStr': left is '$leftType' ($leftValue), right is '$rightType' ($rightValue)"
    }
}

/**
 * Indicates that a property was added to a class structure in the right instance.
 */
public data class FieldAdded(
    override val path: List<PathSegment>,
    val name: String,
    val value: SerialNode
) : Diff {
    override fun toStatement(): String {
        val pathStr = formatPath(path)
        val fullPath = if (pathStr == "<root>") name else "$pathStr.$name"
        return "Property '$fullPath' was added with value: ${value.toDisplayString()}"
    }
}

/**
 * Indicates that a property was removed from a class structure in the right instance.
 */
public data class FieldRemoved(
    override val path: List<PathSegment>,
    val name: String,
    val value: SerialNode
) : Diff {
    override fun toStatement(): String {
        val pathStr = formatPath(path)
        val fullPath = if (pathStr == "<root>") name else "$pathStr.$name"
        return "Property '$fullPath' was removed (old value: ${value.toDisplayString()})"
    }
}

/**
 * Indicates that an element was added at a specific index in a collection.
 */
public data class ElementAdded(
    override val path: List<PathSegment>,
    val index: Int,
    val value: SerialNode
) : Diff {
    override fun toStatement(): String {
        val pathStr = formatPath(path)
        return "Element at index $index was added to '$pathStr': ${value.toDisplayString()}"
    }
}

/**
 * Indicates that an element was removed at a specific index in a collection.
 */
public data class ElementRemoved(
    override val path: List<PathSegment>,
    val index: Int,
    val value: SerialNode
) : Diff {
    override fun toStatement(): String {
        val pathStr = formatPath(path)
        return "Element at index $index was removed from '$pathStr' (old value: ${value.toDisplayString()})"
    }
}

/**
 * Indicates that an entry was added to a Map.
 */
public data class MapEntryAdded(
    override val path: List<PathSegment>,
    val key: SerialNode,
    val value: SerialNode
) : Diff {
    override fun toStatement(): String {
        val pathStr = formatPath(path)
        return "Map entry with key '${key.toDisplayString()}' was added to '$pathStr' with value: ${value.toDisplayString()}"
    }
}

/**
 * Indicates that an entry was removed from a Map.
 */
public data class MapEntryRemoved(
    override val path: List<PathSegment>,
    val key: SerialNode,
    val value: SerialNode
) : Diff {
    override fun toStatement(): String {
        val pathStr = formatPath(path)
        return "Map entry with key '${key.toDisplayString()}' was removed from '$pathStr' (old value: ${value.toDisplayString()})"
    }
}

/**
 * Extension function to format a list of [Diff]s into a single multi-line string.
 */
public fun List<Diff>.format(): String {
    return joinToString("\n") { it.toStatement() }
}
