package data

import com.google.gson.annotations.SerializedName
import java.nio.file.Path

data class ResVersionItem(
    @SerializedName("remoteName")
    val name: String,
    @SerializedName("md5")
    val md5: String,
    @SerializedName("fileSize")
    val size: Long
) {
    fun toPath(root: Path): Path = root.resolve(name)
}
