package kotlinx.serialization.diff

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Serializable
data class SimpleData(
    val b: Boolean,
    val i: Int,
    val s: String,
    val d: Double?
)

@Serializable
data class Address(val city: String, val street: String)

@Serializable
data class User(val name: String, val address: Address)

@Serializable
data class RoleList(val roles: List<String>)

@Serializable
data class ComplexItemList(val items: List<Address>)

@Serializable
data class Preferences(val tags: Map<String, String>)

@Serializable
data class ComplexMap(val data: Map<Address, String>)

enum class Status { ACTIVE, INACTIVE, PENDING }

@Serializable
data class Job(val title: String, val status: Status)

@Serializable
@JvmInline
value class UserId(val value: String)

@Serializable
data class Account(val id: UserId, val balance: Double)

@Serializable
sealed interface Response {
    @Serializable
    data class Success(val data: String) : Response

    @Serializable
    data class Failure(val error: String) : Response
}

@Serializable
data class Container(val response: Response)

class DiffUtilityTest {

    @Test
    fun testBasicPrimitivesAndNulls() {
        val left = SimpleData(b = true, i = 42, s = "hello", d = null)
        val right = SimpleData(b = false, i = 42, s = "world", d = 3.14)

        val diffs = diff(left, right)
        assertEquals(3, diffs.size)

        val bDiff = diffs.filterIsInstance<PropertyValueChanged>().first { it.path.first().toString() == "b" }
        assertEquals(true, bDiff.left)
        assertEquals(false, bDiff.right)

        val sDiff = diffs.filterIsInstance<PropertyValueChanged>().first { it.path.first().toString() == "s" }
        assertEquals("hello", sDiff.left)
        assertEquals("world", sDiff.right)

        val dDiff = diffs.filterIsInstance<PropertyValueChanged>().first { it.path.first().toString() == "d" }
        assertEquals(null, dDiff.left)
        assertEquals(3.14, dDiff.right)

        val expectedStatements = """
            Property 'b' changed from 'true' to 'false'
            Property 's' changed from 'hello' to 'world'
            Property 'd' changed from null to '3.14'
        """.trimIndent()
        assertEquals(expectedStatements, diffs.format())
    }

    @Test
    fun testNestedStructures() {
        val left = User("Alice", Address("New York", "5th Ave"))
        val right = User("Alice", Address("Boston", "Beacon St"))

        val diffs = diff(left, right)
        assertEquals(2, diffs.size)

        val statements = diffs.format().split("\n").sorted()
        assertEquals("Property 'address.city' changed from 'New York' to 'Boston'", statements[0])
        assertEquals("Property 'address.street' changed from '5th Ave' to 'Beacon St'", statements[1])
    }

    @Test
    fun testListIndexByIndexStrategy() {
        val left = RoleList(listOf("Admin", "User", "Guest"))
        val right = RoleList(listOf("Admin", "Moderator", "Guest", "Super"))

        val diffs = diff(left, right, DiffConfig(listStrategy = ListDiffStrategy.INDEX_BY_INDEX))
        // INDEX_BY_INDEX will compare element-by-element:
        // index 0: "Admin" == "Admin" (no diff)
        // index 1: "User" -> "Moderator" (diff)
        // index 2: "Guest" == "Guest" (no diff)
        // index 3: added "Super" (diff)
        assertEquals(2, diffs.size)

        val modDiff = diffs.filterIsInstance<PropertyValueChanged>().first()
        assertEquals("roles[1]", formatPath(modDiff.path))
        assertEquals("User", modDiff.left)
        assertEquals("Moderator", modDiff.right)

        val addDiff = diffs.filterIsInstance<ElementAdded>().first()
        assertEquals("roles", formatPath(addDiff.path))
        assertEquals(3, addDiff.index)
        assertEquals("Super", (addDiff.value as PrimitiveNode).value)
    }

    @Test
    fun testListLcsStrategy() {
        val left = RoleList(listOf("Admin", "User", "Guest"))
        val right = RoleList(listOf("Admin", "Moderator", "Guest"))

        val diffs = diff(left, right, DiffConfig(listStrategy = ListDiffStrategy.LCS))
        // LCS will recognize that "User" was deleted and "Moderator" was inserted:
        // Match "Admin"
        // Delete "User" at index 1
        // Insert "Moderator" at index 1
        // Match "Guest"
        assertEquals(2, diffs.size)

        val delDiff = diffs.filterIsInstance<ElementRemoved>().first()
        assertEquals("roles", formatPath(delDiff.path))
        assertEquals(1, delDiff.index)
        assertEquals("User", (delDiff.value as PrimitiveNode).value)

        val addDiff = diffs.filterIsInstance<ElementAdded>().first()
        assertEquals("roles", formatPath(addDiff.path))
        assertEquals(1, addDiff.index)
        assertEquals("Moderator", (addDiff.value as PrimitiveNode).value)
    }

    @Test
    fun testMaps() {
        val left = Preferences(mapOf("theme" to "dark", "lang" to "en"))
        val right = Preferences(mapOf("theme" to "light", "lang" to "en", "notify" to "true"))

        val diffs = diff(left, right)
        assertEquals(2, diffs.size)

        val themeDiff = diffs.filterIsInstance<PropertyValueChanged>().first()
        assertEquals("tags{theme}", formatPath(themeDiff.path))
        assertEquals("dark", themeDiff.left)
        assertEquals("light", themeDiff.right)

        val addDiff = diffs.filterIsInstance<MapEntryAdded>().first()
        assertEquals("tags", formatPath(addDiff.path))
        assertEquals("notify", (addDiff.key as PrimitiveNode).value)
        assertEquals("true", (addDiff.value as PrimitiveNode).value)
    }

    @Test
    fun testComplexMapKeysAndValues() {
        val left = ComplexMap(mapOf(Address("NYC", "1st") to "Main"))
        val right = ComplexMap(mapOf(Address("NYC", "1st") to "Office", Address("LA", "2nd") to "Branch"))

        val diffs = diff(left, right)
        assertEquals(2, diffs.size)

        val valDiff = diffs.filterIsInstance<PropertyValueChanged>().first()
        assertTrue(formatPath(valDiff.path).startsWith("data{"))
        assertEquals("Main", valDiff.left)
        assertEquals("Office", valDiff.right)

        val addDiff = diffs.filterIsInstance<MapEntryAdded>().first()
        assertEquals("data", formatPath(addDiff.path))
        assertEquals("LA", ((addDiff.key as StructureNode).fields["city"] as PrimitiveNode).value)
        assertEquals("Branch", (addDiff.value as PrimitiveNode).value)
    }

    @Test
    fun testEnums() {
        val left = Job("Developer", Status.ACTIVE)
        val right = Job("Developer", Status.INACTIVE)

        val diffs = diff(left, right)
        assertEquals(1, diffs.size)

        val enumDiff = diffs.filterIsInstance<PropertyValueChanged>().first()
        assertEquals("status", formatPath(enumDiff.path))
        assertEquals("ACTIVE", enumDiff.left)
        assertEquals("INACTIVE", enumDiff.right)
    }

    @Test
    fun testInlineValueClasses() {
        val left = Account(UserId("123"), 100.0)
        val right = Account(UserId("456"), 100.0)

        val diffs = diff(left, right)
        assertEquals(1, diffs.size)

        val idDiff = diffs.filterIsInstance<PropertyValueChanged>().first()
        assertEquals("id", formatPath(idDiff.path))
        assertEquals("123", idDiff.left)
        assertEquals("456", idDiff.right)
    }

    @Test
    fun testPolymorphicSealedClasses() {
        val left = Container(Response.Success("Yay!"))
        val right = Container(Response.Failure("Oops!"))

        val diffs = diff(left, right)
        assertEquals(2, diffs.size)

        val typePropDiff = diffs.filterIsInstance<PropertyValueChanged>().first()
        assertEquals("response.type", formatPath(typePropDiff.path))
        assertTrue(typePropDiff.left.toString().contains("Success"))
        assertTrue(typePropDiff.right.toString().contains("Failure"))

        val typeValueDiff = diffs.filterIsInstance<TypeMismatch>().first()
        assertEquals("response.value", formatPath(typeValueDiff.path))
        assertTrue(typeValueDiff.leftType.contains("Success"))
        assertTrue(typeValueDiff.rightType.contains("Failure"))
    }
}
