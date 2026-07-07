// Fails `check` if `fr.acinq.*` is referenced outside the ACINQ adapter package,
// keeping the engine confined behind the KeyStore port.

tasks.register("verifyEngineBoundarySource") {
    group = "verification"
    description = "Fails if fr.acinq.* is used outside the ACINQ adapter package."
    // Plain File/String captured at configuration time (config-cache safe).
    val srcRoot = layout.projectDirectory.dir("src").asFile
    val rootPath = projectDir.absolutePath
    val adapterPath = "com/bitcoin/wallet/kmp/onchain/engine/acinq"
    // Marker output so the task can be UP-TO-DATE when `sources` are unchanged;
    // without an output, the declared inputs give Gradle nothing to cache against.
    val marker = layout.buildDirectory.file("engine-boundary/verified.marker")
    inputs.dir(srcRoot).withPropertyName("sources").withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.file(marker).withPropertyName("marker")
    doLast {
        // Strip comments before scanning so the namespace can be named in prose
        // without tripping the guard: block comments (incl. KDoc `/** … */`) are
        // removed whole-file, then `//` line comments per line. Catches imports
        // and inline fully-qualified use alike.
        val leakRegex = Regex("""\bfr\.acinq\.""")
        val blockCommentRegex = Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL)
        val offenders = srcRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.path.replace('\\', '/').contains(adapterPath) }
            .filter { file ->
                val stripped = file.readText()
                    .replace(blockCommentRegex, "")
                    .lineSequence()
                    .joinToString("\n") { it.substringBefore("//") }
                leakRegex.containsMatchIn(stripped)
            }
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
        // Record success so Gradle can skip the scan until sources change.
        marker.get().asFile.apply { parentFile.mkdirs(); writeText("ok") }
    }
}

tasks.named("check").configure {
    dependsOn("verifyEngineBoundarySource")
}
