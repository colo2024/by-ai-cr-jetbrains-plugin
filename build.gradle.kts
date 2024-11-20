import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "1.9.25"
  id("org.jetbrains.intellij") version "1.17.4"
}

group = "org.example"
version = "1.0.1"

repositories {
  mavenCentral()
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
//  version.set("2023.2.6")
//  type.set("IC") // Target IDE Platform
  localPath.set("/Applications/IntelliJ IDEA.app/Contents")
//  localPath.set("/Applications/PyCharm CE.app/Contents")
  //localPath.set("/Applications/Android Studio.app/Contents")
//  localPath.set("/Applications/CLion.app/Contents")
//  localPath.set("/Applications/GoLand.app/Contents")
//  localPath.set("/Applications/WebStorm.app/Contents")

//  plugins.set(listOf(/* Plugin Dependencies */))
   plugins.set(
        when {
            // PyCharm
            File(localPath.get()).absolutePath.contains("PyCharm") -> listOf(
                "PythonCore",
//                "org.jetbrains.plugins.go"
            )
            // IntelliJ IDEA
            File(localPath.get()).absolutePath.contains("IntelliJ") -> listOf(
                "com.intellij.java",
            )
            // GoLand
            File(localPath.get()).absolutePath.contains("GoLand") -> listOf(
                "org.jetbrains.plugins.go"
            )
            // Clion
            File(localPath.get()).absolutePath.contains("CLion") -> listOf(
              "com.intellij.cidr.base",        // C/C++ 基础支持
              "com.intellij.cidr.lang",        // C/C++ 语言支持
              "com.intellij.clion",            // CLion 特定功能
            )
            // Android Studio
            File(localPath.get()).absolutePath.contains("Android") -> listOf(
              "com.intellij.java"
            )
            // 默认配置
            else -> listOf(
            )
        }
    )
//  plugins.set(listOf("Git4Idea"))
}

tasks {
  // Set the JVM compatibility versions
  withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
  }
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
  }

  patchPluginXml {
    sinceBuild.set("232")
    untilBuild.set("242.*")
  }

  signPlugin {
    certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
    privateKey.set(System.getenv("PRIVATE_KEY"))
    password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
  }

  publishPlugin {
    token.set(System.getenv("PUBLISH_TOKEN"))
  }

  jar {
    archiveBaseName.set("by-ai-cr-plugin")
    archiveVersion.set(version.toString())
//    archiveClassifier.set("beta")
  }
}
