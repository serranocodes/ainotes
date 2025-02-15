plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    // Removed the Compose plugin alias because it's not needed.
    alias(libs.plugins.google.gms.google.services) apply false
    alias(libs.plugins.hilt) apply false
}
