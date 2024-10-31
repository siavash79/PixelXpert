import java.io.FileInputStream
import java.util.Properties

plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.android)
}

android {

	namespace = "sh.siava.pixelxpert"
	compileSdk = 34

	defaultConfig {
		applicationId = "sh.siava.pixelxpert"
		minSdk = 33
		targetSdk = 34
		versionCode = 401
		versionName = "canary-11"
		setProperty("archivesBaseName", "PixelXpert.apk")
		ndk {
			//noinspection ChromeOsAbiSupport
			abiFilters.add("arm64-v8a")
		}
	}

	val keystorePropertiesFile = rootProject.file("ReleaseKey.properties")
	var releaseSigning = signingConfigs.getByName("debug")

	try {
		val keystoreProperties = Properties()
		FileInputStream(keystorePropertiesFile).use { inputStream ->
			keystoreProperties.load(inputStream)
		}

		releaseSigning = signingConfigs.create("release") {
			keyAlias = keystoreProperties.getProperty("keyAlias")
			keyPassword = keystoreProperties.getProperty("keyPassword")
			storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
			storePassword = keystoreProperties.getProperty("storePassword")
		}
	} catch (ignored: Exception) {
	}

	buildTypes {
		release {
			isMinifyEnabled = true
			isShrinkResources = true
			proguardFiles("proguard-android-optimize.txt", "proguard.pro", "proguard-rules.pro")
			signingConfig = releaseSigning
		}
		debug {
			isDebuggable = true
			isMinifyEnabled = false
			isShrinkResources = false
			signingConfig = releaseSigning
		}
	}

	applicationVariants.all {
		val variant = this
		variant.outputs
			.map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
			.forEach { output ->
				val outputFileName = "PixelXpert.apk"
				output.outputFileName = outputFileName
			}
	}

	buildFeatures{
		viewBinding = true
		buildConfig = true
		aidl = true
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_1_9
		targetCompatibility = JavaVersion.VERSION_1_9
	}
	kotlinOptions {
		jvmTarget = "9"
	}
	packaging {
		jniLibs.excludes += setOf(
			"**/libpytorch_jni_lite.so"
		)

		jniLibs.useLegacyPackaging = true
	}
}

dependencies {

	compileOnly(files("lib/api-82.jar"))
	compileOnly(files("lib/api-82-sources.jar"))

	implementation(project(":Submodules:RangeSliderPreference"))

	implementation (libs.androidx.constraintlayout)
	implementation (libs.navigation.fragment.ktx)
	implementation (libs.navigation.ui.ktx)
	implementation (libs.navigation.fragment.ktx)
	implementation (libs.navigation.ui.ktx)
	implementation (libs.androidx.appcompat)
	implementation (libs.androidx.annotation)
	implementation (libs.androidx.preference.ktx)
	implementation (libs.androidx.recyclerview)
	implementation (libs.android.material)
	implementation (libs.androidx.ui.geometry)
	//noinspection KtxExtensionAvailable
	implementation (libs.androidx.activity)
	implementation (libs.androidx.work.runtime)
	implementation (libs.androidx.concurrent.futures)

	// The core module that provides APIs to a shell
	implementation (libs.libsuCore)
	// Optional: APIs for creating root services. Depends on ":core"
	implementation (libs.libsuService)
	// Optional: Provides remote file system support
	implementation (libs.libsuNIO)

	implementation (libs.remotepreferences)
	// Remote Preferences for Xposed Module prefs
	implementation (libs.colorpicker) //Color Picker Component for UI
	implementation (libs.persian.date.time) //Persian Calendar

	implementation (libs.markdown) //Markdown reader

	// Search Preference
	implementation (libs.androidx.cardview)
	implementation (libs.apache.commons.text)

	implementation (libs.androidx.swiperefreshlayout)

	// Class initializer
	// https://mvnrepository.com/artifact/org.objenesis/objenesis
	implementation (libs.objenesis)

	implementation (libs.ntpClient) //NTP Client

	//Google Subject Segmentation - MLKit
	implementation (libs.play.services.mlkit.subject.segmentation)
	implementation (libs.play.services.base)

  // Collapsing Toolbar with subtitle
	implementation (libs.collapsingtoolbarlayout.subtitle)

	// Splash screen
	implementation (libs.androidx.core.splashscreen)

	implementation (libs.prdownloader)

	implementation (libs.pytorch.android.lite)
	implementation (libs.pytorch.android.torchvision.lite)
	implementation ("com.google.code.gson:gson:2.11.0")
}
