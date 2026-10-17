plugins {
 id("com.android.application")
 id("org.jetbrains.kotlin.android")
}
android {
 namespace = "com.megal.gastrack"
 compileSdk = 34
 defaultConfig {
  applicationId = "com.megal.gastrack"
  minSdk = 24
  targetSdk = 34
  versionCode = 2
  versionName = "2.0-turso"
 }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
 buildTypes { getByName("debug") { isMinifyEnabled = false } }
}
dependencies {
 implementation("androidx.core:core-ktx:1.12.0")
 implementation("com.google.android.gms:play-services-location:21.0.1")
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
