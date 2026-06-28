plugins {
    // trick: for the same plugin versions in all sub-modules
    alias(libs.plugins.androidMultiplatformLibrary).apply(false)
    alias(libs.plugins.kotlinMultiplatform).apply(false)
    alias(libs.plugins.kotlinSerialization).apply(false)
    alias(libs.plugins.nativeCoroutines).apply(false)
}
