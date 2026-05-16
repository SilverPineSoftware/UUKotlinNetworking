plugins {
    alias(uuBuild.plugins.uu.library.app)
    alias(uuBuild.plugins.kotlin.serialization)
    alias(uuBuild.plugins.kotlin.compose)
}

composeCompiler {
    reportsDestination.set(layout.buildDirectory.dir("compose_compiler"))
    stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("stability_config.conf"))
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.annotation.experimental)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.google.material)
    implementation(uuBuild.kotlinx.serialization.json)
    implementation(libs.uu.core.ktx)
    implementation(project(":library"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.runtime.android)
    implementation(libs.androidx.compose.foundation.layout.android)
    implementation(libs.androidx.compose.material3.android)

    androidTestImplementation(uuBuild.androidx.junit)
    androidTestImplementation(uuBuild.androidx.espresso.core)
}
