---

## skill: google-places-api-new
agent: DataAgent
created: 2026-03-30
last_used: 2026-03-30
tags: [google-places, api, http, merchant, category-inference]

# Google Places API (New) — Text Search for Merchant Types

## When to use this

When calling Google Places to look up a merchant's business category by name. New Google Cloud API keys (created after mid-2024) only work with the **Places API (New)** — the legacy `maps.googleapis.com/maps/api/place/findplacefromtext` endpoint returns `REQUEST_DENIED` with the message "You're calling a legacy API, which is not enabled for your project."

## What to do

Use `POST https://places.googleapis.com/v1/places:searchText` with the API key in a **header** (not a query parameter) and field selection via `X-Goog-FieldMask`:

```kotlin
val responseText: String = httpClient.post(
    "https://places.googleapis.com/v1/places:searchText"
) {
    header("X-Goog-Api-Key", apiKey)
    header("X-Goog-FieldMask", "places.types")   // cheapest: Basic tier ~$0.017/call
    contentType(ContentType.Application.Json)
    setBody("""{"textQuery":"$safeMerchantName"}""")
}.bodyAsText()
```

Parse the response:
```json
{
  "places": [
    {
      "types": ["cafe", "coffee_shop", "food", "point_of_interest", "establishment"]
    }
  ]
}
```

```kotlin
val root = lenientJson.parseToJsonElement(responseText).jsonObject
val types = root["places"]?.jsonArray
    ?.firstOrNull()?.jsonObject
    ?.get("types")?.jsonArray
    ?.map { it.jsonPrimitive.content }
```

**Cost**: `places.types` is a Basic Data field — ~$0.017 per call. Google gives $200/month free credit (~11,700 free calls/month). For a personal app doing <100 lookups/month, cost is zero.

**Google Cloud setup required**:
1. Create project at console.cloud.google.com
2. Enable billing (required even for free tier)
3. Enable "Places API" (the new one, not "Places API (legacy)")
4. Create API key → restrict to "Places API"

## Example

Input: `merchantName = "Atypical"`

Response:
```json
{"places":[{"types":["coffee_shop","cafe","food_store","store","food","point_of_interest","establishment"]}]}
```

Mapped to app category: **Food** (matched `coffee_shop` → Food rule in `MerchantSearchRepositoryImpl`)

## Pitfalls

- **Legacy endpoint returns `REQUEST_DENIED`**: `GET maps.googleapis.com/maps/api/place/findplacefromtext/json?key=...` — do not use. This is the legacy API disabled for new projects.
- **API key must be in the header, not query param**: new API uses `X-Goog-Api-Key: {key}` header, not `?key=` query string.
- **Response is `places[]` not `candidates[]`**: old API used `candidates`; new API uses `places`.
- **No status field**: the new API doesn't return a `status` field. An empty `places` array means no results — no error handling needed for ZERO_RESULTS.
- **Escape merchant name**: replace `"` with `\"` in the JSON body to avoid malformed requests for merchant names with quotes.

## Related skills

- `ktor-json-parsing-without-serialization-plugin.md` — parse the response without `@Serializable`
