package utils

@Suppress("EnumEntryName")
enum class Algorithm(
    val s: String,
    val length: Int
) {
     MD2("MD2", 32),
     MD5("MD5", 32),
     Sha1("SHA-1", 40),
     Sha224("SHA-224", 56),
     Sha256("SHA-256", 64),
     Sha384("SHA-384", 96),
     Sha512("SHA-512", 128),
    `Sha3-224`("SHA3-224", 56),
    `Sha3-384`("SHA3-384", 96),
    `Sha3-512`("SHA3-512", 128)
}
