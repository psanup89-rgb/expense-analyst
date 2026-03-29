---

## skill: ktor-json-parsing-without-serialization-plugin
agent: DataAgent
created: 2026-03-30
last_used: 2026-03-30
tags: [ktor, json, serialization, android, data-module]

# Ktor JSON Parsing Without the Serialization Compiler Plugin

## When to use this

When adding a new HTTP response type in the `:data` module and you need to deserialize JSON from a Ktor `body()` call. The `kotlinx.serialization` **compiler plugin is NOT applied** to this project's `:data` module — there is no `kotlin("plugin.serialization")` in `data/build.gradle.kts` or the root. Using `@Serializable` data classes with Ktor's content negotiation will throw a runtime `SerializationException` even though the build succeeds.

## What to do

Instead of using Ktor's content negotiation deserializer (`body<MyClass>()`), fetch raw text and parse with `JsonElement` tree API:

```kotlin
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// Step 1: Get raw response text
val responseText: String = httpClient.get(url).bodyAsText()

// Step 2: Parse to JsonElement (no compiler plugin required)
val root = Json { ignoreUnknownKeys = true; isLenient = true }
    .parseToJsonElement(responseText).jsonObject

// Step 3: Navigate the tree
val status = root["status"]?.jsonPrimitive?.content
val items = root["items"]?.jsonArray
    ?.mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.content }
```

Do NOT define `@Serializable` data classes for the response. Do NOT use `body<MyClass>()`.

For existing simple cases like `CurrencyApiService` that use `val response: ExchangeRateResponse = httpClient.get(url).body()` — these work only because `ExchangeRateResponse` uses primitive types and `Map<String, Double>` which have built-in serializers. Do not rely on this for any new class with nested structures.

## Example

```kotlin
// GooglePlacesApiService — fetches place types for a merchant name
val responseText: String = httpClient.post("https://places.googleapis.com/v1/places:searchText") {
    header("X-Goog-Api-Key", apiKey)
    header("X-Goog-FieldMask", "places.types")
    contentType(ContentType.Application.Json)
    setBody("""{"textQuery":"$safeName"}""")
}.bodyAsText()

val root = lenientJson.parseToJsonElement(responseText).jsonObject
val types = root["places"]?.jsonArray
    ?.firstOrNull()?.jsonObject
    ?.get("types")?.jsonArray
    ?.map { it.jsonPrimitive.content }
```

## Pitfalls

- **`body<T>()` compiles fine but crashes at runtime** with:
  `SerializationException: Serializer for class 'X' is not found. Please ensure that class is marked as '@Serializable' and that the serialization compiler plugin is applied.`
  The build succeeds because the annotation is present; the failure is a runtime lookup for the KSP-generated serializer class that was never generated.

- **`val response: MyClass = body()` has the same problem** — Kotlin infers the same reified type parameter both ways; it's the same underlying call.

- **`ignoreUnknownKeys = true` is essential** on the `Json` instance — Google APIs return many extra fields that would cause parsing failures otherwise.

## Related skills

- `build-full-clean.md` — always run `clean assembleDebug` after adding new files
