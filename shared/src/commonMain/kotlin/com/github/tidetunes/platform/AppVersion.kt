package com.github.tidetunes.platform

expect fun getAppVersion(): String
expect fun getAppBuildInfo(): String

fun getAppGitCommitSha(): String = GeneratedBuildInfo.gitCommitSha
