pluginManagement {

    val configureUuKotlinBuildRepo: MavenArtifactRepository.() -> Unit = {
        name = "UUKotlinBuildGitHubPackages"
        url = uri(System.getenv("UU_KOTLIN_BUILD_URL"))
        credentials {
            username = providers.gradleProperty("gpr.user").orNull
                ?: System.getenv("GITHUB_ACTOR")
            // CI: pass secrets.RELEASE_PAT as env GPR_TOKEN (do not rely on default GITHUB_TOKEN for another repo's Packages).
            password = providers.gradleProperty("gpr.token").orNull
                ?: System.getenv("GPR_TOKEN")
                        ?: System.getenv("GITHUB_TOKEN")
        }
    }

    settings.extra["uuKotlinBuildConfiguration"] = configureUuKotlinBuildRepo


    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven(configureUuKotlinBuildRepo)
    }
}

dependencyResolutionManagement {
    @Suppress("UNCHECKED_CAST")
    val configureUuKotlinBuildRepo = settings.extra["uuKotlinBuildConfiguration"] as MavenArtifactRepository.() -> Unit

    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(uri(System.getenv("MAVEN_CENTRAL_SNAPSHOT_URL")))
        maven(configureUuKotlinBuildRepo)
        mavenLocal()
    }
    versionCatalogs {
    register("uuBuild") {
        val uuBuildVersion = providers.gradleProperty("uu_build").orNull
            ?: error("Set `uu_build=<version>` in gradle.properties.")
        from("com.silverpine.uu:uu-kotlin-build-catalog:$uuBuildVersion")
    }
}
}

rootProject.name = "UUKotlinNetworking"

include(":library")
include(":app")
