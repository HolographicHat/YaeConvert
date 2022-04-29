package utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder

val gson: Gson by lazy {
    GsonBuilder().serializeNulls().create()
}

inline fun <reified T: Any> String.toDataClass(provider: Gson = gson): T = provider.fromJson(this, T::class.java)
