package com.example.ethktprototype.nexus

import android.content.Context
import android.util.Log
import io.ktor.http.*
import io.ktor.server.application.call
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import net.glxn.qrgen.android.QRCode
import java.io.File
import java.net.InetAddress

/*fun startProofServer(context: Context) {
    val nexusBinary = prepareNexusBinary(context)

    embeddedServer(Netty, port = 8081) {
        routing {
            post("/prove") {
                val input = call.receiveText().trim()
                val inputFile = File(context.filesDir, "input.txt")
                inputFile.writeText(input)

                ProcessBuilder(nexusBinary.absolutePath, inputFile.absolutePath)
                    .directory(context.filesDir)
                    .inheritIO()
                    .start()

                val ip = InetAddress.getLocalHost().hostAddress
                val proofUrl = "http://$ip:8080/proof"
                Log.d("ProofServer", "Proof URL: $proofUrl")
                val qr = QRCode.from(proofUrl).withSize(300, 300).stream().toByteArray()

                call.respondBytes(qr, ContentType.Image.PNG)
            }
        }
    }.start(wait = false)
}*/

fun prepareNexusBinary(context: Context): File {
    val binaryName = "nexus-prover"
    val outputFile = File(context.codeCacheDir, binaryName)

    if (!outputFile.exists()) {
        context.assets.open(binaryName).use { input ->
            outputFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        // Make it executable
        outputFile.setExecutable(true)
    }

    return outputFile
}

fun generateProof(input: String, context: Context): String {
    val proverBinary = prepareNexusBinary(context)

    val inputFile = File(context.filesDir, "input.txt")
    inputFile.writeText(input)

    val process = ProcessBuilder(proverBinary.absolutePath, inputFile.absolutePath)
        .directory(context.filesDir)
        .redirectErrorStream(true)
        .start()

    process.waitFor()

    val ip = InetAddress.getLocalHost().hostAddress
    return "http://$ip:8080/proof"
}

fun extractGuestElf(context: Context): File {
    val file = File(context.filesDir, "guest.elf")
    if (!file.exists()) {
        context.assets.open("guest.elf").use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
    }
    val bytes = File(context.filesDir, "guest.elf").readBytes()
    Log.d("ZKP", "Guest ELF size: ${bytes.size}")
    return file
}

