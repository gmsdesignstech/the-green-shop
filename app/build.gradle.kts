import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.net.URI
import java.util.Properties

val localEnvironment = Properties().apply {
  val file = rootProject.file(".env")
  if (file.exists()) file.inputStream().use(::load)
}

fun localConfig(name: String): String =
  providers.gradleProperty(name).orNull
    ?: providers.environmentVariable(name).orNull
    ?: localEnvironment.getProperty(name).orEmpty()

fun quotedBuildConfig(value: String): String =
  "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

fun normalizedApiBaseUrl(rawValue: String, release: Boolean): String {
  val raw = rawValue.trim().trimEnd('/')
  if (raw.isBlank()) {
    if (release) throw GradleException("API_BASE_URL is required for release builds")
    return ""
  }

  val uri = runCatching { URI(raw) }.getOrElse {
    throw GradleException("API_BASE_URL is not a valid URL", it)
  }
  val validScheme = uri.scheme == "https" || (!release && uri.scheme == "http")
  if (!validScheme || uri.host.isNullOrBlank() || uri.rawQuery != null || uri.rawFragment != null) {
    throw GradleException("API_BASE_URL must be ${if (release) "an HTTPS" else "an HTTP(S)"} URL without query or fragment")
  }
  if (release && (uri.host == "localhost" || uri.host == "127.0.0.1" || uri.host == "10.0.2.2")) {
    throw GradleException("API_BASE_URL cannot point to a local host in a release build")
  }

  var path = uri.path.trimEnd('/')
  while (path.endsWith("/api/v1/api/v1")) path = path.removeSuffix("/api/v1")
  if (!path.endsWith("/api/v1")) path += "/api/v1"
  return URI(uri.scheme, uri.userInfo, uri.host, uri.port, path, null, null).toString().trimEnd('/')
}

val productionSupabaseUrl = localConfig("SUPABASE_URL")
val productionSupabaseKey = localConfig("SUPABASE_PUBLISHABLE_KEY")
val mapsApiKey = localConfig("MAPS_API_KEY").trim()
val mapsConfigured = mapsApiKey.isNotBlank() &&
  !mapsApiKey.contains("YOUR_", ignoreCase = true) &&
  !mapsApiKey.contains("MISSING", ignoreCase = true)
val developmentSupabaseUrl = localConfig("DEV_SUPABASE_URL")
val developmentSupabaseKey = localConfig("DEV_SUPABASE_PUBLISHABLE_KEY")
val developmentTestPhone = localConfig("DEV_CUSTOMER_TEST_PHONE_NUMBER").ifBlank { localConfig("DEV_TEST_PHONE_NUMBER") }
val developmentAdminPhone = localConfig("DEV_ADMIN_TEST_PHONE_NUMBER")
val developmentPartnerPhone = localConfig("DEV_DELIVERY_PARTNER_TEST_PHONE_NUMBER")
val localFixedOtpEnabled = localConfig("DEV_FIXED_OTP_ENABLED").equals("true", ignoreCase = true)
val isReleaseInvocation = gradle.startParameter.taskNames.any { it.contains("release", ignoreCase = true) }
val releaseSigningProperties = Properties().apply {
  val file = rootProject.file("key.properties")
  if (file.exists()) file.inputStream().use(::load)
}
fun releaseSigningValue(property: String, environment: String): String =
  releaseSigningProperties.getProperty(property)
    ?: providers.environmentVariable(environment).orNull.orEmpty()
val releaseKeystorePath = releaseSigningValue("storeFile", "KEYSTORE_PATH")
val releaseStorePassword = releaseSigningValue("storePassword", "STORE_PASSWORD")
val releaseKeyAlias = releaseSigningValue("keyAlias", "KEY_ALIAS")
val releaseKeyPassword = releaseSigningValue("keyPassword", "KEY_PASSWORD")
if (isReleaseInvocation) {
  val missing = buildList {
    if (releaseKeystorePath.isBlank()) add("storeFile/KEYSTORE_PATH")
    if (releaseStorePassword.isBlank()) add("storePassword/STORE_PASSWORD")
    if (releaseKeyAlias.isBlank()) add("keyAlias/KEY_ALIAS")
    if (releaseKeyPassword.isBlank()) add("keyPassword/KEY_PASSWORD")
  }
  if (missing.isNotEmpty()) {
    throw GradleException("Release signing is not configured. Missing: ${missing.joinToString()}. Configure ignored key.properties or signing environment variables.")
  }
  if (!mapsConfigured) {
    throw GradleException("MAPS_API_KEY must be a real Android-restricted Google Maps key for release builds")
  }
  val keystore = rootProject.file(releaseKeystorePath)
  if (!keystore.isFile) throw GradleException("Release keystore does not exist: ${keystore.absolutePath}")
}
val releaseApiBaseUrl = providers.gradleProperty("API_BASE_URL").orNull
  ?: providers.environmentVariable("API_BASE_URL").orNull
  ?: localEnvironment.getProperty("API_BASE_URL").orEmpty()
val hasIsolatedDevelopmentAuth =
  developmentSupabaseUrl.startsWith("https://") &&
    developmentSupabaseKey.isNotBlank() &&
    developmentTestPhone.matches(Regex("^\\+[1-9]\\d{7,14}$")) &&
    developmentSupabaseUrl.trimEnd('/') != productionSupabaseUrl.trimEnd('/')

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.greenshop.mkvqlt"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey.ifBlank { "MISSING_MAPS_API_KEY" }
  }

  signingConfigs {
    create("release") {
      if (releaseKeystorePath.isNotBlank()) storeFile = rootProject.file(releaseKeystorePath)
      storePassword = releaseStorePassword
      keyAlias = releaseKeyAlias
      keyPassword = releaseKeyPassword
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
      buildConfigField("String", "AUTH_SUPABASE_URL", quotedBuildConfig(productionSupabaseUrl))
      buildConfigField("String", "AUTH_SUPABASE_PUBLISHABLE_KEY", quotedBuildConfig(productionSupabaseKey))
      buildConfigField("boolean", "DEV_TEST_OTP_ENABLED", "false")
      buildConfigField("String", "DEV_TEST_PHONE_NUMBER", "\"\"")
      buildConfigField("String", "DEV_ADMIN_TEST_PHONE_NUMBER", "\"\"")
      buildConfigField("String", "DEV_DELIVERY_PARTNER_TEST_PHONE_NUMBER", "\"\"")
      buildConfigField("boolean", "DEV_FIXED_OTP_ENABLED", "false")
      buildConfigField("boolean", "MAPS_CONFIGURED", mapsConfigured.toString())
      buildConfigField("String", "API_BASE_URL", quotedBuildConfig(normalizedApiBaseUrl(releaseApiBaseUrl, release = isReleaseInvocation)))
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
      buildConfigField("String", "AUTH_SUPABASE_URL", quotedBuildConfig(if (hasIsolatedDevelopmentAuth) developmentSupabaseUrl else productionSupabaseUrl))
      buildConfigField("String", "AUTH_SUPABASE_PUBLISHABLE_KEY", quotedBuildConfig(if (hasIsolatedDevelopmentAuth) developmentSupabaseKey else productionSupabaseKey))
      buildConfigField("boolean", "DEV_TEST_OTP_ENABLED", hasIsolatedDevelopmentAuth.toString())
      buildConfigField("String", "DEV_TEST_PHONE_NUMBER", quotedBuildConfig(if (hasIsolatedDevelopmentAuth || localFixedOtpEnabled) developmentTestPhone else ""))
      buildConfigField("String", "DEV_ADMIN_TEST_PHONE_NUMBER", quotedBuildConfig(if (localFixedOtpEnabled) developmentAdminPhone else ""))
      buildConfigField("String", "DEV_DELIVERY_PARTNER_TEST_PHONE_NUMBER", quotedBuildConfig(if (localFixedOtpEnabled) developmentPartnerPhone else ""))
      buildConfigField("boolean", "DEV_FIXED_OTP_ENABLED", localFixedOtpEnabled.toString())
      buildConfigField("boolean", "MAPS_CONFIGURED", mapsConfigured.toString())
      buildConfigField("String", "API_BASE_URL", quotedBuildConfig(normalizedApiBaseUrl(localConfig("API_BASE_URL"), release = false)))
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
  ignoreList.add("DEV_.*")
  ignoreList.add("API_BASE_URL")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)

  // Uncomment ALL FOUR of the following dependencies together to use Firebase Auth and Google
  // Sign-In via Credential Manager:
  // implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.firebase.appcheck.debug)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.play.services.location)
  implementation(libs.play.services.maps)
  implementation(libs.maps.compose)
  implementation(libs.retrofit)
  implementation(platform(libs.supabase.bom))
  implementation(libs.supabase.auth)
  implementation(libs.ktor.client.android)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
