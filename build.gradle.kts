plugins {
    java
    id("io.papermc.paperweight.userdev") version "1.7.1"
    id("com.gradleup.shadow") version "8.3.5"
}

group = "world.elyona"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    paperweight.paperDevBundle("1.21.1-R0.1-SNAPSHOT")
    compileOnly("net.luckperms:api:5.4")
    // HikariCP (SQLite/MySQL 接続プール)
    implementation("com.zaxxer:HikariCP:5.1.0")
    // SQLite JDBC
    implementation("org.xerial:sqlite-jdbc:3.45.3.0")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {
    // HikariCP・sqlite-jdbc(implementation依存)をshadowJarで同梱してから難読化する
    assemble {
        dependsOn(reobfJar)
    }
    reobfJar {
        inputJar.set(shadowJar.flatMap { it.archiveFile })
    }
    shadowJar {
        archiveClassifier.set("shadow")
    }
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }
}
