// ---------------------------------------------------------------------------
// Engine-boundary guard — applied into :shared via `apply(from = ...)`.
// Wired into `check`, so it runs locally and in CI.
//
// The ACINQ engine (`fr.acinq.*`) is an implementation detail confined to the
// adapter package. This module is single-module by design, so the compiler
// cannot physically prevent an errant `import fr.acinq.*` in domain/wallet code
// — these tasks are that missing wall. They fail the build on two leak modes:
//   1. Source leak:   `import fr.acinq.` anywhere outside the adapter package.
//   2. Re-export leak: the `fr.acinq` engine declared `api(...)` (re-exported)
//                      instead of `implementation(...)`.
// ---------------------------------------------------------------------------

val verifyEngineBoundarySource = tasks.register("verifyEngineBoundarySource") {
    group = "verification"
    description = "Fails if fr.acinq.* is imported outside the ACINQ adapter package."
    // Capture plain File/String at configuration time — no project/script
    // references leak into the task action (config-cache safe).
    val srcRoot = layout.projectDirectory.dir("src").asFile
    val rootPath = projectDir.absolutePath
    val adapterPath = "com/bitcoin/wallet/kmp/onchain/engine/acinq"
    inputs.dir(srcRoot).withPropertyName("sources").withPathSensitivity(PathSensitivity.RELATIVE)
    doLast {
        val importRegex = Regex("""^\s*import\s+fr\.acinq\.""")
        val offenders = srcRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.path.replace('\\', '/').contains(adapterPath) }
            .filter { file -> file.readLines().any { importRegex.containsMatchIn(it) } }
            .map { it.absolutePath.removePrefix("$rootPath/") }
            .toList()
        if (offenders.isNotEmpty()) {
            throw GradleException(
                "Engine boundary violated — `fr.acinq.*` imported outside the adapter " +
                "package ($adapterPath):\n" +
                offenders.joinToString("\n") { "  - $it" } +
                "\nMove engine usage into the adapter and depend on the KeyStore port instead."
            )
        }
    }
}

val verifyEngineBoundaryApi = tasks.register("verifyEngineBoundaryApi") {
    group = "verification"
    description = "Fails if the ACINQ engine dependency is declared api() (re-exported) instead of implementation()."
    // On KMP there is no per-configuration signal that cleanly separates `api`
    // from `implementation` for a commonMain dependency — Kotlin/Native and
    // metadata apiElements carry `implementation` klib deps too (needed for
    // cross-module compilation). So we assert the RULE at its source: the ACINQ
    // engine must be declared `implementation(...)`, never `api(...)`, in the
    // module build script. Capture the file at configuration time (config-cache safe).
    val buildScript = buildFile
    inputs.file(buildScript).withPropertyName("buildScript")
    doLast {
        val engineApiRegex = Regex("""^\s*api\s*\(\s*libs\.bitcoin\.kmp\s*\)""")
        val offending = buildScript.readLines()
            .withIndex()
            .filter { (_, line) -> engineApiRegex.containsMatchIn(line) }
            .map { (i, line) -> "  ${buildScript.name}:${i + 1}: ${line.trim()}" }
        if (offending.isNotEmpty()) {
            throw GradleException(
                "Engine boundary violated — the ACINQ engine is declared `api(...)`, " +
                "which re-exports `fr.acinq.*` to consumers:\n" +
                offending.joinToString("\n") +
                "\nChange it to `implementation(libs.bitcoin.kmp)` so ACINQ stays confined."
            )
        }
    }
}

tasks.named("check").configure {
    dependsOn(verifyEngineBoundarySource, verifyEngineBoundaryApi)
}
