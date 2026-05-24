package kotlinx.serialization.diff

import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule

/**
 * Base implementation of [CompositeEncoder] that handles common boilerplate of
 * serializing primitives, inlines, and nested serializable elements, delegating
 * the stored result to concrete subclasses via [storeNode].
 */
public abstract class BaseCompositeEncoder(
    override val serializersModule: SerializersModule
) : CompositeEncoder {

    public abstract fun storeNode(index: Int, node: SerialNode)

    override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean = true

    override fun encodeBooleanElement(descriptor: SerialDescriptor, index: Int, value: Boolean) {
        storeNode(index, PrimitiveNode(descriptor.getElementDescriptor(index), value))
    }

    override fun encodeByteElement(descriptor: SerialDescriptor, index: Int, value: Byte) {
        storeNode(index, PrimitiveNode(descriptor.getElementDescriptor(index), value))
    }

    override fun encodeCharElement(descriptor: SerialDescriptor, index: Int, value: Char) {
        storeNode(index, PrimitiveNode(descriptor.getElementDescriptor(index), value))
    }

    override fun encodeShortElement(descriptor: SerialDescriptor, index: Int, value: Short) {
        storeNode(index, PrimitiveNode(descriptor.getElementDescriptor(index), value))
    }

    override fun encodeIntElement(descriptor: SerialDescriptor, index: Int, value: Int) {
        storeNode(index, PrimitiveNode(descriptor.getElementDescriptor(index), value))
    }

    override fun encodeLongElement(descriptor: SerialDescriptor, index: Int, value: Long) {
        storeNode(index, PrimitiveNode(descriptor.getElementDescriptor(index), value))
    }

    override fun encodeFloatElement(descriptor: SerialDescriptor, index: Int, value: Float) {
        storeNode(index, PrimitiveNode(descriptor.getElementDescriptor(index), value))
    }

    override fun encodeDoubleElement(descriptor: SerialDescriptor, index: Int, value: Double) {
        storeNode(index, PrimitiveNode(descriptor.getElementDescriptor(index), value))
    }

    override fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String) {
        storeNode(index, PrimitiveNode(descriptor.getElementDescriptor(index), value))
    }

    override fun encodeInlineElement(descriptor: SerialDescriptor, index: Int): Encoder {
        val elementDescriptor = descriptor.getElementDescriptor(index)
        return TreeEncoder(serializersModule, elementDescriptor) { node ->
            storeNode(index, node)
        }
    }

    override fun <T> encodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T
    ) {
        val nestedEncoder = TreeEncoder(serializersModule, serializer.descriptor) { node ->
            storeNode(index, node)
        }
        serializer.serialize(nestedEncoder, value)
    }

    override fun <T : Any> encodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T?
    ) {
        if (value == null) {
            storeNode(index, NullNode(serializer.descriptor))
        } else {
            encodeSerializableElement(descriptor, index, serializer, value)
        }
    }
}

/**
 * Composite encoder for structured objects (classes, objects).
 */
public class StructureCompositeEncoder(
    private val descriptor: SerialDescriptor,
    serializersModule: SerializersModule,
    private val onCompleted: (SerialNode) -> Unit
) : BaseCompositeEncoder(serializersModule) {
    private val fields = mutableMapOf<String, SerialNode>()

    override fun storeNode(index: Int, node: SerialNode) {
        val name = descriptor.getElementName(index)
        fields[name] = node
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        onCompleted(StructureNode(this.descriptor, fields))
    }
}

/**
 * Composite encoder for collections (lists, sets, arrays).
 */
public class CollectionCompositeEncoder(
    private val descriptor: SerialDescriptor,
    serializersModule: SerializersModule,
    private val onCompleted: (SerialNode) -> Unit
) : BaseCompositeEncoder(serializersModule) {
    private val elements = mutableListOf<SerialNode>()

    override fun storeNode(index: Int, node: SerialNode) {
        elements.add(node)
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        onCompleted(CollectionNode(this.descriptor, elements))
    }
}

/**
 * Composite encoder for Maps.
 */
public class MapCompositeEncoder(
    private val descriptor: SerialDescriptor,
    serializersModule: SerializersModule,
    private val onCompleted: (SerialNode) -> Unit
) : BaseCompositeEncoder(serializersModule) {
    private val entries = mutableListOf<Pair<SerialNode, SerialNode>>()
    private var pendingKey: SerialNode? = null

    override fun storeNode(index: Int, node: SerialNode) {
        if (index % 2 == 0) {
            pendingKey = node
        } else {
            val key = pendingKey ?: error("Map value serialized before its key at index $index")
            entries.add(key to node)
            pendingKey = null
        }
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        if (pendingKey != null) {
            error("Map serialized with an odd number of elements (missing last value)")
        }
        onCompleted(MapNode(this.descriptor, entries))
    }
}
