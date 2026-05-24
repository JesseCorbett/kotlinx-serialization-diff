# kotlinx-serialization-diff

![Maven Central Version](https://img.shields.io/maven-central/v/com.jessecorbett/kotlinx-serialization-diff)

A diffing library for Kotlin/Multiplatform which uses `kotlinx.serialization` for performant, type-safe diffing without reflection.

[Dokka Documentation](https://jessecorbett.github.io/kotlinx-serialization-diff/)

## Features

- **Multiplatform**: Supports JVM, JS, Wasm, Android, iOS, and Native targets.
- **No Reflection**: Leverages `kotlinx.serialization`'s compile-time generated serializers.
- **Flexible**: Supports deep diffing of nested objects, lists, and maps.
- **Customizable**: Choose between index-based diffing or Longest Common Subsequence (LCS) for list collections.

## Getting Started

Add the dependency to your `build.gradle.kts` (check Maven Central for the latest version):

```kotlin
implementation("com.jessecorbett:kotlinx-serialization-diff:1.1.0")
```

### Basic Usage

Use the `diff` function to get a list of changes:

```kotlin
import kotlinx.serialization.diff.diff
import kotlinx.serialization.Serializable

@Serializable
data class User(val name: String, val age: Int)

val user1 = User("Alice", 30)
val user2 = User("Alice", 31)

val changes = diff(user1, user2)
// Changes: [PropertyValueChanged(path=[Property(name=age)], left=30, right=31, ...)]
```

### Human-Readable Statements

For logging or UI display, use `diffStatements` to get a pre-formatted multi-line string:

```kotlin
import kotlinx.serialization.diff.diffStatements

val statements = diffStatements(user1, user2)
println(statements)
// Output: Property 'age' changed from '30' to '31'
```

Alternatively, call `.format()` on any `List<Diff>`:

```kotlin
import kotlinx.serialization.diff.format

val output = diff(user1, user2).format()
```

## Configuration

You can customize the diffing behavior by passing a `DiffConfig`.

### List Diffing Strategies

`kotlinx-serialization-diff` supports two strategies for diffing lists and collections:

1. **`INDEX_BY_INDEX` (Default)**: Compares elements at the same index. Best for fixed-position data.
2. **`LCS`**: Uses the Longest Common Subsequence (Myers' diff algorithm) to identify insertions and deletions. Best for lists where items might be shifted.

```kotlin
import kotlinx.serialization.diff.DiffConfig
import kotlinx.serialization.diff.ListDiffStrategy

val config = DiffConfig(listStrategy = ListDiffStrategy.LCS)
val changes = diff(oldList, newList, config)
```

### Custom Serializers Modules

If your models use contextual or polymorphic serialization that requires a custom `SerializersModule`, you can provide it in the configuration:

```kotlin
import kotlinx.serialization.modules.SerializersModule

val myModule = SerializersModule {
    contextual(MyCustomSerializer)
}

val config = DiffConfig(serializersModule = myModule)
val changes = diff(obj1, obj2, config)
```

## Types of Diffs

The library identifies several types of changes:

- **`PropertyValueChanged`**: A primitive value (String, Int, Boolean, etc.) or null changed.
- **`FieldAdded` / `FieldRemoved`**: A property was added or removed (e.g., when diffing polymorphic types).
- **`ElementAdded` / `ElementRemoved`**: An element was added to or removed from a List or Set.
- **`MapEntryAdded` / `MapEntryRemoved`**: A key-value pair was added to or removed from a Map.
- **`TypeMismatch`**: The structure changed significantly (e.g., a field changed from a primitive to a nested object).

## License

This project is licensed under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt).
