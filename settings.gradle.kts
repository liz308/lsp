rootProject.name = "lsp-android"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

include(":android-app")
include(":lsp-core")
include(":lsp-client")
include(":lsp-server")
include(":protocol")
include(":common")
include(":editor")
include(":syntax-highlighting")
include(":code-completion")
include(":diagnostics")
include(":file-watcher")
include(":project-manager")
