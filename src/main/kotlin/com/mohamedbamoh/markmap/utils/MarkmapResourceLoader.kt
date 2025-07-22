package com.mohamedbamoh.markmap.utils

import java.io.IOException

class MarkmapResourceLoader {

    fun loadResource(resourcePath: String): String {
        val stream = javaClass.getResourceAsStream(resourcePath)
            ?: throw IOException("Resource not found: $resourcePath")

        return stream.bufferedReader().readText()
    }
}