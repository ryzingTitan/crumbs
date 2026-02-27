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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            authentication { create<BasicAuthentication>("basic") }
            credentials {
                username = "mapbox"
                // sk.* token read from local.properties (gitignored); never committed
                password = providers.fileContents(
                    layout.settingsDirectory.file("local.properties")
                ).asText.orElse("").map { text ->
                    text.lines()
                        .firstOrNull { it.startsWith("MAPBOX_DOWNLOAD_TOKEN=") }
                        ?.removePrefix("MAPBOX_DOWNLOAD_TOKEN=")
                        .orEmpty()
                }.get()
            }
        }
    }
}

rootProject.name = "Crumbs"
include(":app")
include(":wear")
