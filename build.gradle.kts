plugins {
    id("com.android.application") version "9.4.1" apply false
    // Not applied: AGP 9 compiles Kotlin itself. Declared only to pin the Kotlin version.
    id("org.jetbrains.kotlin.android") version "2.4.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
}
