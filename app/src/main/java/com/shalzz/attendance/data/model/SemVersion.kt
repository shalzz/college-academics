package com.shalzz.attendance.data.model

/**
 * @author shalzz
 */
data class SemVersion (
    val version: String,
    var major: Int = 0,
    var minor: Int = 0,
    var patch: Int = 0
) : Comparable<SemVersion>
{
    init {
        val values = version.removePrefix("v").split(".")
        major = values[0].toInt()
        minor = values[1].toInt()
        patch = values[2].toInt()
    }

    override fun compareTo(other:SemVersion) =
        compareValuesBy(this, other, SemVersion::major, SemVersion::minor, SemVersion::patch)
}