package com.example.ethktprototype.nexus

object ProofBridge {
    init {
        System.loadLibrary("healthyTest") // no 'lib' prefix, no '.so'
    }

    external fun runProof(elfPath: String, input: String): String
}