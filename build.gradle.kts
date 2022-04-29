import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm") version "1.6.20"
    id("org.graalvm.buildtools.native") version "0.9.4"
}

repositories {
    mavenCentral()
}

nativeBuild {
    verbose.set(true)
    imageName.set("YaeConvert")
    jvmArgs.add("-Dfile.encoding=utf-8")
    buildArgs.addAll(
        "--allow-incomplete-classpath",
        "-H:+ReportExceptionStackTraces",
        "-H:+ReportUnsupportedElementsAtRuntime",
        "--report-unsupported-elements-at-runtime",
        "--initialize-at-build-time=kotlinx,kotlin",
        "-H:ReflectionConfigurationFiles=../../../ref.json"
    )
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(11))
    })
}

dependencies {
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.6.1")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

application {
    mainClass.set("MainKt")
    applicationDefaultJvmArgs += "-Dfile.encoding=utf-8"
}

tasks.withType<Jar> {
    manifest {
        attributes("Main-Class" to "MainKt")
    }
    from(configurations.runtimeClasspath.get().map { zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
