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
implementation("com.jessecorbett:kotlinx-serialization-diff:1.0.0")
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

### Readable Diff Statements

For quick debugging or logging, use `diffStatements` to get a human-readable list of changes:

```kotlin
val statements = diffStatements(user1, user2)
// "Property 'age' changed from '30' to '31'"
```

## License

This project is licensed under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt).
