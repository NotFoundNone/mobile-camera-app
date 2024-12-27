plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.kapt) apply false
}

// Добавление репозиториев
repositories {
    google() // Для библиотек AndroidX и CameraX
    mavenCentral() // Для других Java/Kotlin библиотек
}

// Общие настройки для всех проектов
subprojects {
    repositories {
        google()
        mavenCentral()
    }
}

// Опционально: настройка кэша для Gradle
gradle.projectsEvaluated {
    tasks.withType<org.gradle.api.tasks.compile.JavaCompile> {
        options.isIncremental = true
    }
}
