package com.example.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken

/**
 * author: yolanda-zhao
 * description:数据解析相关工具类
 * date: 2019/6/12
 */
object H5JsonDecoderUtils {


    val mGson by lazy {
        GsonBuilder().serializeSpecialFloatingPointValues() // 允许 NaN、Infinity 等浮点值
            .disableHtmlEscaping().create()
    }

    /**
     * 将json字符串数据解析成特定的bean类格式
     */
    fun <T> jsonToBean(json: String, clz: Class<T>): T {
        return mGson.fromJson<T>(json, clz)
    }


    /**
     * 将对象转化为json字符串
     */
    fun toJson(obj: Any): String {
        return mGson.toJson(obj)
    }

    /**
     * 将对象转化为JsonObject
     */
    fun toJsonObj(obj: Any): JsonObject {
        return JsonParser().parse(toJson(obj)).asJsonObject
    }

    /**
     * 将对象转化为JsonObject
     */
    fun strToJsonObj(obj: String): JsonObject {
        return JsonParser().parse(obj).asJsonObject
    }

    inline fun <reified T> safeParseJsonToList(json: String?): List<T> {
        return try {
            json?.takeIf { it.isNotBlank() }?.let {
                val type = object : TypeToken<List<T>>() {}.type
                mGson.fromJson(it, type) ?: emptyList()
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}

fun Any.toJson(): String {
    return H5JsonDecoderUtils.toJson(this)
}