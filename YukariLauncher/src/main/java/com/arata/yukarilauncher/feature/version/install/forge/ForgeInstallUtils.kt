package com.arata.yukarilauncher.feature.version.install.forge

import java.io.File
import java.nio.file.Files
import java.nio.file.Path

fun parseLiteral(
    baseDir: File,
    literal: String,
    vars: Map<String, String> = emptyMap(),
    plainConverter: (String) -> String = { it }
): String? {
    return when {
        literal.startsWith("{") && literal.endsWith("}") -> {
            vars[literal.removeSurrounding("{", "}")]
        }
        literal.startsWith("'") && literal.endsWith("'") -> {
            literal.removeSurrounding("'")
        }
        literal.startsWith("[") && literal.endsWith("]") -> {
            val path = getLibraryPath(literal.removeSurrounding("[", "]"))
            baseDir.toPath().resolve("libraries").resolve(path).toAbsolutePath().toString()
        }
        else -> {
            plainConverter(replaceTokens(vars, literal))
        }
    }
}

fun parseOptions(baseDir: File, args: List<String>, vars: Map<String, String>): Map<String, String> {
    val options = LinkedHashMap<String, String>()
    var optionName: String? = null

    for (arg in args) {
        if (arg.startsWith("--")) {
            optionName?.let { options[it] = "" }
            optionName = arg.removePrefix("--")
        } else {
            if (optionName != null) {
                options[optionName] = parseLiteral(baseDir, arg, vars) ?: arg
                optionName = null
            }
        }
    }

    optionName?.let { options[it] = "" }

    return options
}

fun replaceTokens(tokens: Map<String, String>, value: String): String {
    val buf = StringBuilder()
    var x = 0
    while (x < value.length) {
        val c = value[x]
        if (c == '\\') {
            require(x != value.length - 1) { "Illegal pattern (Bad escape): $value" }
            buf.append(value[++x])
        } else if (c == '{' || c == '\'') {
            val key = StringBuilder()
            var y = x + 1
            while (y <= value.length) {
                require(y != value.length) { "Illegal pattern (Unclosed $c): $value" }
                val d = value[y]
                if (d == '\\') {
                    require(y != value.length - 1) { "Illegal pattern (Bad escape): $value" }
                    key.append(value[++y])
                } else {
                    if (c == '{' && d == '}') {
                        x = y
                        break
                    }
                    if (c == '\'' && d == '\'') {
                        x = y
                        break
                    }
                    key.append(d)
                }
                y++
            }
            if (c == '\'') {
                buf.append(key)
            } else {
                require(tokens.containsKey(key.toString())) { "Illegal pattern: $value Missing Key: $key" }
                buf.append(tokens[key.toString()])
            }
        } else {
            buf.append(c)
        }
        x++
    }
    return buf.toString()
}

fun extractEntryToFile(zipFile: java.util.zip.ZipFile, entryPath: String, outputFile: File) {
    val entry = zipFile.getEntry(entryPath)
        ?: throw IllegalArgumentException("ZIP entry does not exist: $entryPath")
    require(!entry.isDirectory) { "Cannot extract directory to file: ${entry.name}" }

    outputFile.parentFile?.mkdirs()
    zipFile.getInputStream(entry).use { input ->
        outputFile.outputStream().use { output ->
            input.copyTo(output)
        }
    }
}

fun calculateSha1(file: File): String {
    val digest = java.security.MessageDigest.getInstance("SHA-1")
    file.inputStream().use { stream ->
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (stream.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

fun ensureDirectory(file: File): File {
    if (file.isFile) throw java.io.IOException("Target directory is a file: $file")
    if (file.exists()) {
        if (!file.canWrite()) throw java.io.IOException("Target directory not writable: $file")
    } else {
        if (!file.mkdirs()) throw java.io.IOException("Unable to create directory: $file")
    }
    return file
}
