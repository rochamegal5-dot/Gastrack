
plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
 namespace = "com.megal.gastrack"
 compileSdk = 34
 defaultConfig { applicationId = "com.megal.gastrack"; minSdk = 24; targetSdk = 34; versionCode = 8; versionName = "1.0.8" }
 buildTypes { release { isMinifyEnabled = false } }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
}
dependencies { implementation("androidx.core:core-ktx:1.12.0"); implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"); implementation("androidx.work:work-runtime-ktx:2.9.0") }
