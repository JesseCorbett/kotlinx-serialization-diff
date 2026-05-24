package kotlinx.serialization.diff

import kotlinx.serialization.serializer

/**
 * Convenience entry point to diff two instances of a serializable class.
 * Uses the compile-time serializer resolved via [serializer].
 */
public inline fun <reified T> diff(
    left: T,
    right: T,
    config: DiffConfig = DiffConfig()
): List<Diff> {
    val serializer = serializer<T>()
    val format = TreeFormat(config.serializersModule)
    val leftTree = format.toTree(serializer, left)
    val rightTree = format.toTree(serializer, right)

    val diffs = mutableListOf<Diff>()
    diffRecursive(leftTree, rightTree, emptyList(), config, diffs)
    return diffs
}

/**
 * Convenience entry point to diff two instances of a serializable class
 * and return the formatted diff statements as a single multi-line string.
 */
public inline fun <reified T> diffStatements(
    left: T,
    right: T,
    config: DiffConfig = DiffConfig()
): String {
    return diff(left, right, config).format()
}
