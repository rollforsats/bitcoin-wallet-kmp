import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.nativeCoroutines)
    alias(libs.plugins.mavenPublish)
}

group = "com.bitcoin.wallet.kmp"
version = "0.1.0"

kotlin {
    android {
        namespace = "com.bitcoin.wallet.kmp"
        compileSdk = 36
        minSdk = 24
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    jvm()

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        all {
            languageSettings.optIn("kotlin.experimental.ExperimentalObjCName")
        }
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            api(libs.bitcoin.kmp)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.android)
            implementation(libs.secp256k1.jni.android)
        }
        jvmMain.dependencies {
            implementation(libs.secp256k1.jni.jvm)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

publishing {
    publications {
        // KMP plugin creates publications automatically; configure POM metadata here.
        withType<MavenPublication>().configureEach {
            pom {
                name.set("bitcoin-wallet-kmp")
                description.set("Bitcoin wallet core (KMP)")
            }
        }
    }
}
