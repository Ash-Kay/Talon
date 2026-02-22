package io.ashkay.talon.agent

enum class LogStatus {
  ONGOING,
  DONE,
  ERROR,
  INFO,
}

data class LogEntry(val message: String, val status: LogStatus = LogStatus.INFO)
