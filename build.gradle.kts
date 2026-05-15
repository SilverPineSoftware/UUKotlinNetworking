extra["uu_namespace"] = "com.silverpine.uu.networking"
extra["uu_publish_artifact_id"] = "uu-networking-ktx"
extra["uu_publish_description"] = "Useful Utilities Networking"
extra["uu_scm_module_name"] = "UUKotlinNetworking"

extra["uu_min_sdk"] = 26
extra["uu_target_sdk"] = 36
extra["uu_java_version"] = 17

plugins {
    alias(uuBuild.plugins.android.application) apply false
    alias(uuBuild.plugins.android.library) apply false
    alias(uuBuild.plugins.kotlin.android) apply false
    alias(uuBuild.plugins.nexus.publish)
    alias(uuBuild.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose) apply false
    alias(uuBuild.plugins.uu.library) apply false
    alias(uuBuild.plugins.uu.library.app) apply false
    alias(uuBuild.plugins.uu.android.test) apply false
    alias(uuBuild.plugins.uu.publish)
}
