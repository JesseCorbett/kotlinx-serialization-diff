package kotlinx.serialization.diff

/**
 * Checks if two [SerialNode] AST trees are structurally equal (deep equality).
 */
public fun areNodesEqual(a: SerialNode, b: SerialNode): Boolean {
    if (a::class != b::class) return false
    return when (a) {
        is NullNode -> b is NullNode
        is PrimitiveNode -> b is PrimitiveNode && a.value == b.value
        is StructureNode -> {
            b as StructureNode
            a.fields.keys == b.fields.keys && a.fields.all { (k, v) ->
                areNodesEqual(v, b.fields[k]!!)
            }
        }
        is CollectionNode -> {
            b as CollectionNode
            a.elements.size == b.elements.size && a.elements.zip(b.elements).all { (x, y) ->
                areNodesEqual(x, y)
            }
        }
        is MapNode -> {
            b as MapNode
            if (a.entries.size != b.entries.size) return false
            // Compare maps structurally regardless of order
            a.entries.all { (ak, av) ->
                b.entries.any { (bk, bv) -> areNodesEqual(ak, bk) && areNodesEqual(av, bv) }
            }
        }
    }
}

/**
 * Recursively diffs two [SerialNode] AST trees and populates [diffs].
 */
public fun diffRecursive(
    left: SerialNode,
    right: SerialNode,
    path: List<PathSegment>,
    config: DiffConfig,
    diffs: MutableList<Diff>
) {
    // 1. Check for NullNode comparisons (changes to/from null)
    if (left is NullNode || right is NullNode) {
        if (left is NullNode && right is NullNode) {
            return // Both are null, no difference
        }
        // One is null, the other is not.
        // If the other is primitive, it's a PropertyValueChanged.
        // Otherwise, it's a TypeMismatch (structure changed to null).
        when (left) {
            is NullNode if right is PrimitiveNode -> {
                diffs.add(PropertyValueChanged(path, null, right.value, "kotlin.Nothing?", right.descriptor.serialName))
            }
            is PrimitiveNode -> {
                diffs.add(PropertyValueChanged(path, left.value, null, left.descriptor.serialName, "kotlin.Nothing?"))
            }
            else -> {
                diffs.add(
                    TypeMismatch(
                        path,
                        leftType = left.descriptor.serialName,
                        rightType = right.descriptor.serialName,
                        leftValue = left.toDisplayString(),
                        rightValue = right.toDisplayString()
                    )
                )
            }
        }
        return
    }

    // 2. Both are non-null. Check for class/type mismatch.
    if (left::class != right::class || left.descriptor.serialName != right.descriptor.serialName) {
        // Different node types or class type names (e.g., Success vs. Failure)
        diffs.add(
            TypeMismatch(
                path,
                leftType = left.descriptor.serialName,
                rightType = right.descriptor.serialName,
                leftValue = left.toDisplayString(),
                rightValue = right.toDisplayString()
            )
        )
        return
    }

    // 3. Delegate based on a concrete node type
    when (left) {
        is PrimitiveNode -> {
            right as PrimitiveNode
            if (left.value != right.value) {
                diffs.add(
                    PropertyValueChanged(
                        path,
                        left.value,
                        right.value,
                        left.descriptor.serialName,
                        right.descriptor.serialName
                    )
                )
            }
        }

        is StructureNode -> {
            right as StructureNode
            diffStructures(left, right, path, config, diffs)
        }

        is CollectionNode -> {
            right as CollectionNode
            diffCollections(left, right, path, config, diffs)
        }

        is MapNode -> {
            right as MapNode
            diffMaps(left, right, path, config, diffs)
        }
    }
}

private fun diffStructures(
    left: StructureNode,
    right: StructureNode,
    path: List<PathSegment>,
    config: DiffConfig,
    diffs: MutableList<Diff>
) {
    val leftFields = left.fields
    val rightFields = right.fields
    val allKeys = leftFields.keys + rightFields.keys

    for (key in allKeys) {
        val inLeft = key in leftFields
        val inRight = key in rightFields

        if (inLeft && !inRight) {
            diffs.add(FieldRemoved(path, key, leftFields[key]!!))
        } else if (!inLeft && inRight) {
            diffs.add(FieldAdded(path, key, rightFields[key]!!))
        } else {
            // In both, recurse
            diffRecursive(leftFields[key]!!, rightFields[key]!!, path + PathSegment.Property(key), config, diffs)
        }
    }
}

private fun diffCollections(
    left: CollectionNode,
    right: CollectionNode,
    path: List<PathSegment>,
    config: DiffConfig,
    diffs: MutableList<Diff>
) {
    if (config.listStrategy == ListDiffStrategy.INDEX_BY_INDEX) {
        val minSize = minOf(left.elements.size, right.elements.size)
        for (i in 0 until minSize) {
            diffRecursive(left.elements[i], right.elements[i], path + PathSegment.Index(i), config, diffs)
        }
        if (left.elements.size > right.elements.size) {
            for (i in minSize until left.elements.size) {
                diffs.add(ElementRemoved(path, i, left.elements[i]))
            }
        } else if (right.elements.size > left.elements.size) {
            for (i in minSize until right.elements.size) {
                diffs.add(ElementAdded(path, i, right.elements[i]))
            }
        }
    } else {
        // LCS / Myers'-style diffing
        diffCollectionsLcs(left, right, path, diffs)
    }
}

private sealed interface ListOp {
    data class Match(val leftIdx: Int, val rightIdx: Int) : ListOp
    data class Insert(val rightIdx: Int, val value: SerialNode) : ListOp
    data class Delete(val leftIdx: Int, val value: SerialNode) : ListOp
}

private fun diffCollectionsLcs(
    left: CollectionNode,
    right: CollectionNode,
    path: List<PathSegment>,
    diffs: MutableList<Diff>
) {
    val leftElems = left.elements
    val rightElems = right.elements
    val m = leftElems.size
    val n = rightElems.size

    val dp = Array(m + 1) { IntArray(n + 1) }
    for (i in 1..m) {
        for (j in 1..n) {
            if (areNodesEqual(leftElems[i - 1], rightElems[j - 1])) {
                dp[i][j] = dp[i - 1][j - 1] + 1
            } else {
                dp[i][j] = maxOf(dp[i - 1][j], dp[i][j - 1])
            }
        }
    }

    var i = m
    var j = n
    val ops = mutableListOf<ListOp>()

    while (i > 0 || j > 0) {
        if (i > 0 && j > 0 && areNodesEqual(leftElems[i - 1], rightElems[j - 1])) {
            ops.add(ListOp.Match(i - 1, j - 1))
            i--
            j--
        } else if (j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j])) {
            ops.add(ListOp.Insert(j - 1, rightElems[j - 1]))
            j--
        } else {
            ops.add(ListOp.Delete(i - 1, leftElems[i - 1]))
            i--
        }
    }

    ops.reverse()

    // For user reporting, we can output deletions and additions in order
    for (op in ops) {
        when (op) {
            is ListOp.Match -> {
                // Do nothing for matches
            }
            is ListOp.Delete -> {
                diffs.add(ElementRemoved(path, op.leftIdx, op.value))
            }
            is ListOp.Insert -> {
                diffs.add(ElementAdded(path, op.rightIdx, op.value))
            }
        }
    }
}

private fun diffMaps(
    left: MapNode,
    right: MapNode,
    path: List<PathSegment>,
    config: DiffConfig,
    diffs: MutableList<Diff>
) {
    val matchedRightIndices = mutableSetOf<Int>()

    for ((leftKey, leftValue) in left.entries) {
        val rightEntryIndex = right.entries.indexOfFirst { (rk, _) -> areNodesEqual(leftKey, rk) }
        if (rightEntryIndex != -1) {
            matchedRightIndices.add(rightEntryIndex)
            val (_, rightValue) = right.entries[rightEntryIndex]
            val keyStr = leftKey.toDisplayString()
            diffRecursive(leftValue, rightValue, path + PathSegment.MapKey(keyStr), config, diffs)
        } else {
            diffs.add(MapEntryRemoved(path, leftKey, leftValue))
        }
    }

    for (index in right.entries.indices) {
        if (index !in matchedRightIndices) {
            val (rightKey, rightValue) = right.entries[index]
            diffs.add(MapEntryAdded(path, rightKey, rightValue))
        }
    }
}
