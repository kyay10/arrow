@file:Suppress("DSL_SCOPE_VIOLATION")

import kotlinx.benchmark.gradle.KotlinJvmBenchmarkTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id(libs.plugins.kotlin.multiplatform.get().pluginId)
  alias(libs.plugins.arrowGradleConfig.kotlin)
  alias(libs.plugins.arrowGradleConfig.publish)
  alias(libs.plugins.kotest.multiplatform)
  id("org.jetbrains.kotlinx.benchmark") version "0.4.7"
  id("org.jetbrains.kotlin.plugin.allopen") version "1.8.10"
}

apply(from = property("TEST_COVERAGE"))
apply(from = property("ANIMALSNIFFER_MPP"))

val enableCompatibilityMetadataVariant =
  providers.gradleProperty("kotlin.mpp.enableCompatibilityMetadataVariant")
    .orNull?.toBoolean() == true

if (enableCompatibilityMetadataVariant) {
  tasks.withType<Test>().configureEach {
    exclude("**/*")
  }
}

kotlin {
  targets {
    jvm {
      compilations {
        val main by getting
        val benchmarks by creating {
          associateWith(main)
        }

        benchmark.targets.add(
          KotlinJvmBenchmarkTarget(
            benchmark,
            benchmarks.defaultSourceSet.name,
            benchmarks,
          )
        )
      }
    }
  }
  sourceSets {
    commonMain {
      dependencies {
        api(projects.arrowAtomic)
        api(projects.arrowAnnotations)
        api(libs.kotlin.stdlibCommon)
        implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.4")
      }
    }
    if (!enableCompatibilityMetadataVariant) {
      commonTest {
        dependencies {
          implementation(projects.arrowFxCoroutines)
          implementation(libs.kotest.frameworkEngine)
          implementation(libs.kotest.assertionsCore)
          implementation(libs.kotest.property)
        }
      }
      jvmTest {
        dependencies {
          runtimeOnly(libs.kotest.runnerJUnit5)
        }
      }
    }

    jvmMain {
      dependencies {
        implementation(libs.kotlin.stdlibJDK8)
      }
    }

    jsMain {
      dependencies {
        implementation(libs.kotlin.stdlibJS)
      }
    }

    val jvmMain by getting
    val jvmBenchmarks by getting {
      dependsOn(jvmMain)
      dependencies {
        implementation("org.openjdk.jmh:jmh-core:1.21")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
      }
    }
  }
}

// enables context receivers for Jvm Tests
tasks.named<KotlinCompile>("compileTestKotlinJvm") {
  kotlinOptions.freeCompilerArgs += "-Xcontext-receivers"
}

// enables context receivers for Jvm Benchmarks
tasks.named<KotlinCompile>("compileBenchmarksKotlinJvm") {
  kotlinOptions.freeCompilerArgs += "-Xcontext-receivers"
}

allOpen {
  annotation("org.openjdk.jmh.annotations.State")
}
