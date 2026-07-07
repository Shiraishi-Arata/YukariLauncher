package com.arata.yukarilauncher.feature.version.install.forge

import com.google.gson.annotations.SerializedName

data class LibraryComponents(
    val groupId: String,
    val artifactId: String,
    val version: String,
    val classifier: String? = null,
    val extension: String = "jar"
) {
    val descriptor: String by lazy {
        "$groupId:$artifactId:$version".let {
            if (classifier.isNullOrEmpty()) it
            else "$it:$classifier"
        }.let {
            if (extension != "jar") "$it@$extension"
            else it
        }
    }
}

fun fromDescriptor(descriptor: String): LibraryComponents {
    val arr = descriptor.split(":", limit = 4).toMutableList()
    if (arr.size != 3 && arr.size != 4) {
        throw IllegalArgumentException("Artifact name is malformed: $descriptor")
    }

    var extension: String? = null
    val last = arr.size - 1
    val splitted = arr[last].split("@")
    when (splitted.size) {
        2 -> {
            arr[last] = splitted[0]
            extension = splitted[1]
        }
        in 3..Int.MAX_VALUE -> throw IllegalArgumentException("Artifact name is malformed: $descriptor")
    }

    return LibraryComponents(
        arr[0].replace('\\', '/'),
        arr[1],
        arr[2],
        if (arr.size >= 4) arr[3] else "",
        extension ?: "jar"
    )
}

fun LibraryComponents.toPath(): String {
    val rawName = "$artifactId-$version".let { raw ->
        if (classifier.isNullOrEmpty()) raw
        else "$raw-$classifier"
    }
    val fileName = "$rawName.$extension"
    return "${groupId.replace('.', '/')}/$artifactId/$version/$fileName"
}

fun getLibraryPath(descriptor: String): String {
    return fromDescriptor(descriptor).toPath()
}

class ForgeLikeInstallProcessor(
    @SerializedName("sides")
    private val sides: List<String>?,
    @SerializedName("jar")
    private val jar: String,
    @SerializedName("classpath")
    private val classpath: List<String>?,
    @SerializedName("args")
    private val args: List<String>?,
    @SerializedName("outputs")
    private val outputs: Map<String, String>?
) {
    fun isSide(side: String): Boolean {
        return sides == null || sides.contains(side)
    }

    fun getJar(): LibraryComponents {
        return fromDescriptor(this.jar)
    }

    fun getClasspath(): List<LibraryComponents> {
        return classpath?.map { fromDescriptor(it) } ?: emptyList()
    }

    fun getArgs(): List<String> {
        return args ?: emptyList()
    }

    fun getOutputs(): Map<String, String> {
        return outputs ?: emptyMap()
    }
}
