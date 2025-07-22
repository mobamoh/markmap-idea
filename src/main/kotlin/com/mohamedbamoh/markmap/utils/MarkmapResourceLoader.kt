package com.mohamedbamoh.markmap.utils

import java.io.IOException

class MarkmapResourceLoader {

    fun loadResource(resourcePath: String): String {
        val possiblePaths = listOf(
            resourcePath,
            "/markmap/${resourcePath.substring(1)}"
        )

        for (path in possiblePaths) {
            try {
                val stream = javaClass.getResourceAsStream(path)
                if (stream != null) {
                    return stream.bufferedReader().readText()
                }
            } catch (e: IOException) {
                // Continue to next path
            }
        }

        throw IOException("Resource not found: $resourcePath (tried paths: $possiblePaths)")
    }
}