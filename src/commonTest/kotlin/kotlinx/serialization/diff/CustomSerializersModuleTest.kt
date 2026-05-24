package kotlinx.serialization.diff

import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlin.test.Test
import kotlin.test.assertEquals

class MyCustomType(val value: String)

object MyCustomTypeSerializer : KSerializer<MyCustomType> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("MyCustomType", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: MyCustomType) = encoder.encodeString(value.value)
    override fun deserialize(decoder: Decoder): MyCustomType = MyCustomType(decoder.decodeString())
}

@Serializable
data class DataWithContextual(
    @Contextual val custom: MyCustomType
)

class CustomSerializersModuleTest {

    @Test
    fun testCustomSerializersModule() {
        val left = DataWithContextual(MyCustomType("old"))
        val right = DataWithContextual(MyCustomType("new"))

        // This should fail because MyCustomType is contextual and no module is provided
        kotlin.test.assertFails {
            diff(left, right)
        }

        val module = SerializersModule {
            contextual(MyCustomTypeSerializer)
        }

        val config = DiffConfig(serializersModule = module)
        val diffs = diff(left, right, config)

        assertEquals(1, diffs.size)
        val diff = diffs.filterIsInstance<PropertyValueChanged>().first()
        assertEquals("custom", formatPath(diff.path))
        assertEquals("old", diff.left)
        assertEquals("new", diff.right)
    }
}
