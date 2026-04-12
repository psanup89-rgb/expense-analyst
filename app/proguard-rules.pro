# ============================================================
# Expense Analyst — ProGuard / R8 rules
# ============================================================

# --------------- General Kotlin / JVM ---------------

# Keep Kotlin metadata so reflection-based libs (Hilt, Room) work correctly.
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes Signature

# Kotlin companion objects and object declarations
-keepnames class kotlin.** { *; }
-keepclassmembers class ** {
    @kotlin.jvm.JvmField *;
    @kotlin.jvm.JvmStatic *;
}

# Enum classes — required for Room's string-mapped enum columns and Gson/JSON parsing
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# --------------- Kotlin Coroutines ---------------

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# --------------- kotlinx.datetime ---------------

-keep class kotlinx.datetime.** { *; }
-dontwarn kotlinx.datetime.**

# --------------- Hilt / Dagger ---------------

# Hilt-generated components are accessed by name — keep all generated classes.
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ActivityComponentManager { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-keep @dagger.Module class * { *; }
-keepclasseswithmembers class * {
    @dagger.Provides *;
    @dagger.Binds *;
}
-dontwarn dagger.hilt.**
-dontwarn dagger.internal.**

# --------------- Room ---------------

# Room entities are instantiated via reflection by the generated DAO code.
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.TypeConverter class * { *; }
-keep class androidx.room.** { *; }

# App-specific entities and DAOs
-keep class com.expenseanalyst.data.local.entity.** { *; }
-keep class com.expenseanalyst.data.local.dao.** { *; }
-keep class com.expenseanalyst.data.local.ExpenseAnalystDatabase { *; }

# --------------- Domain models ---------------

# Domain data classes are used by Room mappers — keep all fields and constructors.
-keep class com.expenseanalyst.domain.model.** { *; }

# --------------- Ktor (Android / OkHttp engine) ---------------

-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# OkHttp (Ktor's Android engine dependency)
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# --------------- Jetpack Compose ---------------

# Compose is fully R8-compatible; no custom rules needed.
# Keep composable lambda classes so stack traces remain readable.
-keepattributes LineNumberTable,SourceFile

# --------------- DataStore ---------------

-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# --------------- Miscellaneous suppression ---------------

# Suppress warnings from libraries that reference missing classes at build time.
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn java.lang.invoke.**
-dontwarn javax.annotation.**
