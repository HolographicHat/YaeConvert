package data

import com.google.gson.annotations.SerializedName

data class FullPackageInfo(
    @SerializedName("c")
    val cn: PackageInfo,
    @SerializedName("o")
    val os: PackageInfo
)

data class PackageInfo(
    @SerializedName("e")
    val name: String,
    @SerializedName("d")
    val scatteredUrl: String
)
