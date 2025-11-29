import groovy.json.JsonOutput
import groovy.json.JsonSlurper

plugins {
    id("net.neoforged.moddev") version "2.0.107"
}

// Toolchain versions
val minecraftVersion: String = "1.21.1"
val neoForgeVersion: String = "21.1.197"
val parchmentVersion: String = "2024.11.17"
val parchmentMinecraftVersion: String = "1.21.1"
val tfcVersion: String = "4.0.1-beta"

// Dependency versions
val patchouliVersion: String = "1.21.1-92-NEOFORGE"

val modId: String = "ez_supervisor"
val modVersion: String = "3.0.1"
val modJavaVersion: String = "21"
val modDataOutput: String = "src/generated/resources"

val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
    val modReplacementProperties = mapOf(
            "modId" to modId,
            "modVersion" to modVersion,
            "minecraftVersionRange" to "[$minecraftVersion]",
            "neoForgeVersionRange" to "[$neoForgeVersion,)",
            "tfcVersionRange" to "[$tfcVersion,)"
    )
    inputs.properties(modReplacementProperties)
    expand(modReplacementProperties)
    from("src/main/templates")
    into(layout.buildDirectory.dir("generated/sources/modMetadata"))
}

neoForge {
    version = neoForgeVersion // this is here because declaring a neoForge version enables 'additionalRuntimeClasspath'
}

base {
    archivesName.set("EntityZoningSupervisor-NeoForge-$minecraftVersion")
    group = "com.eerussianguy.entity_zoning_supervisor"
    version = modVersion
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(modJavaVersion))
}

repositories {
    mavenCentral()
    mavenLocal()
    exclusiveContent {
        forRepository { maven("https://maven.blamejared.com") }
        filter { includeGroup("vazkii.patchouli") }
    }
    exclusiveContent {
        forRepository { maven("https://www.cursemaven.com") }
        filter { includeGroup("curse.maven") }
    }
}

sourceSets {
    main {
        resources {
            srcDir(modDataOutput)
            srcDir(generateModMetadata)
        }
    }
    create("data")
}

neoForge {
    addModdingDependenciesTo(sourceSets["data"])
    validateAccessTransformers = true

    parchment {
        minecraftVersion.set(parchmentMinecraftVersion)
        mappingsVersion.set(parchmentVersion)
    }

    runs {
        configureEach {
            // Only JBR allows enhanced class redefinition, so ignore the option for any other JDKs
            jvmArguments.addAll("-XX:+IgnoreUnrecognizedVMOptions", "-XX:+AllowEnhancedClassRedefinition", "-ea")
        }
        register("client") {
            client()
            gameDirectory = file("run/client")
        }
        register("server") {
            server()
            gameDirectory = file("run/server")
            programArgument("--nogui")
        }
        register("data") {
            data()
            sourceSet = sourceSets["data"]
            programArguments.addAll("--all", "--mod", modId, "--output", file(modDataOutput).absolutePath, "--existing",  file("src/main/resources").absolutePath)
        }
    }

    mods {
        create(modId) {
            sourceSet(sourceSets.main.get())
            sourceSet(sourceSets.test.get())
            sourceSet(sourceSets["data"])
        }
    }

    unitTest {
        enable()
        testedMod = mods[modId];
    }

    ideSyncTask(generateModMetadata)
}

dependencies {
    // Patchouli
    // We need to compile against the full JAR, not just the API, because we do some egregious hacks.
    runtimeOnly("vazkii.patchouli:Patchouli:$patchouliVersion")

    // ModernFix - useful at runtime for significant memory savings in TFC in dev (see i.e. wall block shape caches)
    runtimeOnly(group = "curse.maven", name = "modernfix-790626", version = "6766126")

    // TFC
    implementation(group = "curse.maven", name = "terrafirmacraft-302973", version = "6998336")

    // Data
    "dataImplementation"(sourceSets["main"].output)
}

tasks {
    jar {
        manifest {
            attributes["Implementation-Version"] = project.version
        }
    }

    named("neoForgeIdeSync") {
        dependsOn(generateModMetadata)
    }
}

