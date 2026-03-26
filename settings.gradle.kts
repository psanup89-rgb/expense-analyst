pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ExpenseAnalyst"

include(":app")
include(":core")
include(":domain")
include(":data")
include(":feature:expenses")
include(":feature:emi")
include(":feature:notification")
include(":feature:settings")
include(":feature:onboarding")
