import java.util.Locale

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.4.0"
}

if (!file(".git").exists()) {
    val errorText = """
        
        =====================[ ERROR ]=====================
         The FoliaSIMD project directory is not a properly cloned Git repository.
         
         In order to build FoliaSIMD from source you must clone
         the FoliaSIMD repository using Git, not download a code
         zip from GitHub.
         
         See https://github.com/PaperMC/Paper/blob/master/CONTRIBUTING.md
         for further information on building and modifying Paper and Forks.
        ===================================================
    """.trimIndent()
    error(errorText)
}

rootProject.name = "FoliaSIMD"

for (name in listOf("Folia-API", "Folia-Server")) {
    val projName = name.lowercase(Locale.ENGLISH)
    include(projName)
    findProject(":$projName")!!.projectDir = file(name)
}
