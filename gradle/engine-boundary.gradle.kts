// Fails `check` if `fr.acinq.*` is referenced outside the ACINQ adapter package,
// keeping the engine confined behind the KeyStore port.

tasks.register("verifyEngineBoundarySource") {
    group = "verification"
    description = "Fails if fr.acinq.* is used outside the ACINQ adapter package."
    // Plain File/String captured at configuration time (config-cache safe).
    val srcRoot = layout.projectDirectory.dir("src").asFile
    val rootPath = projectDir.absolutePath
    val adapterPath = "com/bitcoin/wallet/kmp/onchain/engine/acinq"
    inputs.dir(srcRoot).withPropertyName("sources").withPathSensitivity(PathSensitivity.RELATIVE)
    doLast {
        // Catches imports and inline fully-qualified use; `//` comments stripped
        // so the namespace can be named in prose without tripping the guard.
        val leakRegex = Regex("""\bfr\.acinq\.""")
        val offenders = srcRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.path.replace('\\', '/').contains(adapterPath) }
            .filter { file -> file.readLines().any { leakRegex.containsMatchIn(it.substringBefore("//")) } }
            .map { it.absolutePath.removePrefix("$rootPath/") }
            .toList()
        if (offenders.isNotEmpty()) {
            throw GradleException(
                "Engine boundary violated — `fr.acinq.*` used outside the adapter " +
                "package ($adapterPath):\n" +
                offenders.joinToString("\n") { "  - $it" } +
                "\nMove engine usage into the adapter and depend on the KeyStore port instead."
            )
        }
    }
}

tasks.named("check").configure {
    dependsOn("verifyEngineBoundarySource")
}
