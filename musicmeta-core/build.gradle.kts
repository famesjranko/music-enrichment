plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    `maven-publish`
}

group = "com.landofoz"
version = "0.1.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<Test> {
    // Forward system properties to test JVM (for E2E test gating)
    systemProperty("include.e2e", System.getProperty("include.e2e") ?: "false")

    // Forward API keys (system property > environment variable > empty)
    val apiKeys = mapOf(
        "lastfm.apikey" to "LASTFM_API_KEY",
        "fanarttv.apikey" to "FANARTTV_API_KEY",
        "discogs.token" to "DISCOGS_TOKEN",
    )
    apiKeys.forEach { (prop, env) ->
        systemProperty(prop, System.getProperty(prop) ?: System.getenv(env) ?: "")
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "musicmeta-core"
        }
    }
}

dependencies {
    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // JSON parsing
    implementation(libs.json)

    // Serialization (for cache layer consumers)
    api(libs.kotlinx.serialization.json)

    // Testing
    testImplementation(libs.bundles.testing)
}
