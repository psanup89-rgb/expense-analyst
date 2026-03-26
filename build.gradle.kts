// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
}

// KSP reliability fix for multi-module Hilt projects.
//
// Problem: Android Studio incremental builds (and even "Clean + Assemble") leave stale
// KSP-generated code in build/generated/ksp/. When a new agent session adds @Inject
// classes or new domain interfaces, Hilt sees BOTH old and new generated files in the
// same directory and fails with PROCESSING_ERROR.
//
// Fix: tasks.configureEach (lazy, catches all tasks including lazily-registered ones)
// marks every ksp*Kotlin task as never-up-to-date AND deletes its output dir before
// running. This is a targeted clean for generated code only (~50ms extra per build).
// Combined with ksp.incremental=false in gradle.properties, KSP always starts fresh.
subprojects {
    tasks.configureEach {
        if (name.startsWith("ksp") && name.endsWith("Kotlin")) {
            outputs.upToDateWhen { false }
            doFirst {
                outputs.files.forEach { f -> if (f.exists()) f.deleteRecursively() }
            }
        }
    }
}
