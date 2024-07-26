import com.google.protobuf.gradle.id

plugins {
    kotlin("jvm") version "1.9.23"
    id("com.google.protobuf") version "0.9.4"
}

dependencies {
    api("io.grpc:grpc-kotlin-stub:1.4.1")
    api("io.grpc:grpc-protobuf:1.62.2")
    api("com.google.protobuf:protobuf-kotlin:3.23.4")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
}

protobuf {
    protoc {
        artifact = when (osdetector.os) {
            "osx" -> "com.google.protobuf:protoc:3.23.4:osx-x86_64"
            "linux" -> "com.google.protobuf:protoc:3.23.4:linux-x86_64"
            else -> "com.google.protobuf:protoc:3.23.4"
        }
    }
    plugins {
        protoc {
            id("grpc") {
                artifact = when (osdetector.os) {
                    "osx" -> "io.grpc:protoc-gen-grpc-java:1.62.2:osx-x86_64"
                    else -> "io.grpc:protoc-gen-grpc-java:1.62.2"
                }
            }
            id("grpckt") {
                artifact = when (osdetector.os) {
                    in listOf("osx", "linux") -> "io.grpc:protoc-gen-grpc-kotlin:1.4.1:jdk8@jar"
                    else -> "io.grpc:protoc-gen-grpc-kotlin:1.4.1"
                }
            }
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                id("grpc")
                id("grpckt")
            }
            task.builtins {
                id("kotlin")
            }
        }
    }
}

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = "21"
        }
    }
}

fun mergeProtoFiles(fileMap: MutableMap<String, MutableList<File>>) {
    val syntaxPackagesRegex = "(?m)^(?=.*syntax|package|option|import)[^;]*;\$".toRegex()
    val enumMessageRegex = "(?m)(?:enum|message)\\s+.*?[\\{](?:\\n|.)*?[\\}]".toRegex()
    val serviceRegex = "(?m)service\\s+\\w+\\s*\\{(?:[^\\{\\}]*\\{(?:[^\\{\\}]*\\{[^\\{\\}]*\\})*[^\\{\\}]*}[^\\{\\}]*)*\\}".toRegex()

    fileMap.forEach { (folderName, files) ->
        File("$buildDir/${folderName}_merged.proto").bufferedWriter().use { mergedWriter ->
            println("Found files: " + files.joinToString { it.name })

            val text = files.joinToString("\n") { it.readText() }

            syntaxPackagesRegex.findAll(text)
                .distinctBy { it.value }
                .filter {
                    !it.value.contains("java_multiple_files")
                        && files.all { file -> !it.value.contains(file.name) }
                }
                .sortedByDescending { it.value }
                .groupBy { it.value.split(" ").first() }
                .forEach { matchResult ->
                    if (matchResult.value.joinToString { it.value }.contains("option java_package")) {
                        mergedWriter.appendLine(matchResult.value.map { it.value }.minBy { it.length })
                        mergedWriter.appendLine()
                    } else {
                        matchResult.value.forEach { mergedWriter.appendLine(it.value) }
                        mergedWriter.appendLine()
                    }
                }
            mergedWriter.appendLine()

            enumMessageRegex.findAll(text).forEach { matchResult ->
                mergedWriter.appendLine(matchResult.value)
                mergedWriter.appendLine()
            }
            mergedWriter.appendLine()

            serviceRegex.findAll(text).forEach { matchResult ->
                mergedWriter.appendLine(matchResult.value)
                mergedWriter.appendLine()
            }
            mergedWriter.appendLine()
        }
        println("Finished creating merged proto file: \"$buildDir/${folderName}_merged.proto\"")
    }
}

tasks.register("mergeProtoFiles") {
    doLast {
        val protoDir = "${projectDir}/src/main/proto"
        val protoFiles = mutableMapOf<String, MutableList<File>>()

        // Recursive function to find .proto files
        fun findProtoFiles(directory: File) {
            directory.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    findProtoFiles(file) // Recursive call for subdirectories
                } else if (file.isFile && file.extension == "proto") {
                    protoFiles.getOrPut(file.parentFile.name) { mutableListOf() }.add(file)
                }
            }
        }

        findProtoFiles(File(protoDir))

        if (protoFiles.isNotEmpty()) {
            mergeProtoFiles(protoFiles)
        } else {
            println("No proto files found in directory: $protoDir")
        }
    }
}

tasks.build { finalizedBy("mergeProtoFiles") }
