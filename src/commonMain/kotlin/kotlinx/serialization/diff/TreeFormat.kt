package kotlinx.serialization.diff

import kotlinx.serialization.SerialFormat
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

/**
 * Custom [SerialFormat] that serializes Kotlin objects into in-memory walkable ASTs ([SerialNode]).
 */
public class TreeFormat(
    override val serializersModule: SerializersModule = EmptySerializersModule()
) : SerialFormat {

    /**
     * Serializes the given [value] into a [SerialNode] tree representation.
     */
    public fun <T> toTree(serializer: SerializationStrategy<T>, value: T): SerialNode {
        val encoder = TreeEncoder(serializersModule, serializer.descriptor)
        serializer.serialize(encoder, value)
        return encoder.root ?: NullNode(serializer.descriptor)
    }
}

/**
 * Main [Encoder] implementation that builds a [SerialNode] representation of the serialized value.
 */
public class TreeEncoder(
    override val serializersModule: SerializersModule,
    private val descriptor: SerialDescriptor,
    private val onCompleted: (SerialNode) -> Unit = {}
) : Encoder {

    public var root: SerialNode? = null
        private set

    private fun setRoot(node: SerialNode) {
        root = node
        onCompleted(node)
    }

    override fun encodeNull() {
        setRoot(NullNode(descriptor))
    }

    override fun encodeNotNullMark() {
        // No-op for our custom representation
    }

    override fun encodeBoolean(value: Boolean) {
        setRoot(PrimitiveNode(descriptor, value))
    }

    override fun encodeByte(value: Byte) {
        setRoot(PrimitiveNode(descriptor, value))
    }

    override fun encodeChar(value: Char) {
        setRoot(PrimitiveNode(descriptor, value))
    }

    override fun encodeShort(value: Short) {
        setRoot(PrimitiveNode(descriptor, value))
    }

    override fun encodeInt(value: Int) {
        setRoot(PrimitiveNode(descriptor, value))
    }

    override fun encodeLong(value: Long) {
        setRoot(PrimitiveNode(descriptor, value))
    }

    override fun encodeFloat(value: Float) {
        setRoot(PrimitiveNode(descriptor, value))
    }

    override fun encodeDouble(value: Double) {
        setRoot(PrimitiveNode(descriptor, value))
    }

    override fun encodeString(value: String) {
        setRoot(PrimitiveNode(descriptor, value))
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        val enumValueName = enumDescriptor.getElementName(index)
        setRoot(PrimitiveNode(enumDescriptor, enumValueName))
    }

    override fun encodeInline(descriptor: SerialDescriptor): Encoder {
        return TreeEncoder(serializersModule, descriptor) { node ->
            setRoot(node)
        }
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        return when (descriptor.kind) {
            is StructureKind.MAP -> MapCompositeEncoder(descriptor, serializersModule) { node ->
                setRoot(node)
            }
            is StructureKind.LIST -> CollectionCompositeEncoder(descriptor, serializersModule) { node ->
                setRoot(node)
            }
            else -> StructureCompositeEncoder(descriptor, serializersModule) { node ->
                setRoot(node)
            }
        }
    }

    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        return beginStructure(descriptor)
    }
}
