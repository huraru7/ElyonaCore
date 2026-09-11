plugins {
    java
    id("io.papermc.paperweight.userdev") version "1.7.1"
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
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(group = "org.bukkit", module = "bukkit")
    }
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
    assemble {
        dependsOn(reobfJar)
    }
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }
}
