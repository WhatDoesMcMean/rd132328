import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.2"
}

group = "me.kalmemarq"
version = "1.0-SNAPSHOT"

val lwjglVersion = "3.3.4"
val jomlVersion = "1.10.5"
val log4jVersion = "2.24.0"
val jacksonVersion = "2.17.2"
val fastUtilVersion = "8.5.14"
val imguiVersion = "1.87.0"
val lwjglNatives = getNativesLwjgl()
val imguiNatives = getNativesImGui()

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
    arrayOf("lwjgl", "lwjgl-glfw", "lwjgl-jemalloc", "lwjgl-opengl", "lwjgl-stb").forEach {
        implementation("org.lwjgl", it)
        runtimeOnly("org.lwjgl", it, classifier = lwjglNatives)
    }

    implementation("org.apache.logging.log4j", "log4j-api", log4jVersion)
    implementation("org.apache.logging.log4j", "log4j-core", log4jVersion)
    implementation("com.fasterxml.jackson.core", "jackson-core", jacksonVersion)
    implementation("com.fasterxml.jackson.core", "jackson-databind", jacksonVersion)
    implementation("org.joml", "joml", jomlVersion)
    implementation("it.unimi.dsi", "fastutil-core", fastUtilVersion)

    implementation("io.github.spair", "imgui-java-binding", imguiVersion)
    implementation("io.github.spair", "imgui-java-lwjgl3", imguiVersion) {
        exclude("org.lwjgl")
    }
    implementation("io.github.spair", "imgui-java-$imguiNatives", imguiVersion)
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    named<ShadowJar>("shadowJar") {
        minimize()
    }
}

fun getNativesImGui(): String {
    val name = System.getProperty("os.name")!!
    return when {
        arrayOf("Linux", "SunOS", "Unit").any { name.startsWith(it) } -> "natives-linux"
        arrayOf("Mac OS X", "Darwin").any { name.startsWith(it) } -> "natives-macos"
        arrayOf("Windows").any { name.startsWith(it) } -> "natives-windows"
        else ->
            throw Error("Unrecognized or unsupported platform")
    }
}

fun getNativesLwjgl(): String {
    return Pair(
        System.getProperty("os.name")!!,
        System.getProperty("os.arch")!!
    ).let { (name, arch) ->
        when {
            "FreeBSD" == name -> "natives-freebsd"
            arrayOf("Linux", "SunOS", "Unit").any { name.startsWith(it) } ->
                if (arrayOf("arm", "aarch64").any { arch.startsWith(it) })
                    "natives-linux${if (arch.contains("64") || arch.startsWith("armv8")) "-arm64" else "-arm32"}"
                else if (arch.startsWith("ppc"))
                    "natives-linux-ppc64le"
                else if (arch.startsWith("riscv"))
                    "natives-linux-riscv64"
                else
                    "natives-linux"
            arrayOf("Mac OS X", "Darwin").any { name.startsWith(it) } ->
                "natives-macos${if (arch.startsWith("aarch64")) "-arm64" else ""}"
            arrayOf("Windows").any { name.startsWith(it) } ->
                if (arch.contains("64"))
                    "natives-windows${if (arch.startsWith("aarch64")) "-arm64" else ""}"
                else
                    "natives-windows-x86"
            else ->
                throw Error("Unrecognized or unsupported platform. Please set \"lwjglNatives\" manually")
        }
    }
}