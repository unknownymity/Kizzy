/*
 *
 *  ******************************************************************
 *  *  * Copyright (C) 2022
 *  *  * RpcImage.kt is part of Kizzy
 *  *  *  and can not be copied and/or distributed without the express
 *  *  * permission of yzziK(Vaibhav)
 *  *  *****************************************************************
 *
 *
 */

package com.my.kizzy.data.rpc

import android.content.Context
import android.graphics.Bitmap
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.my.kizzy.domain.repository.KizzyRepository
import com.my.kizzy.data.preference.Prefs
import com.my.kizzy.data.utils.getAppInfo
import com.my.kizzy.data.utils.toBitmap
import com.my.kizzy.data.utils.toFile


sealed class RpcImage {
    abstract suspend fun resolveImage(repository: KizzyRepository): String?

    class DiscordImage(val image: String) : RpcImage() {
        override suspend fun resolveImage(repository: KizzyRepository): String {
            return "mp:${image}"
        }
    }

    class ExternalImage(val image: String) : RpcImage() {
        override suspend fun resolveImage(repository: KizzyRepository): String? {
            return repository.getImage(image)
        }
    }

    class ApplicationIcon(private val packageName: String, private val context: Context) : RpcImage() {
        val data = Prefs[Prefs.SAVED_IMAGES, "{}"]
        private val savedImages: HashMap<String, String> = Gson().fromJson(data,
            object : TypeToken<HashMap<String, String>>() {}.type)

        override suspend fun resolveImage(repository: KizzyRepository): String? {
            return if (savedImages.containsKey(packageName))
                savedImages[packageName]
            else
                retrieveImageFromApi(packageName, context, repository)
        }

        private suspend fun retrieveImageFromApi(
            packageName: String,
            context: Context,
            repository: KizzyRepository,
        ): String? {
            val applicationInfo = context.getAppInfo(packageName)
            val bitmap = applicationInfo.toBitmap(context)
            val response = repository.uploadImage(bitmap.toFile(context, "image"))
            response?.let {
                savedImages[packageName] = it
                Prefs[Prefs.SAVED_IMAGES] = Gson().toJson(savedImages)
            }
            return response
        }
    }

    class BitmapImage(
        private val context: Context,
        private val bitmap: Bitmap?,
        private val packageName: String,
        val title: String,
    ) : RpcImage() {
        override suspend fun resolveImage(repository: KizzyRepository): String? {
            val data = Prefs[Prefs.SAVED_ARTWORK, "{}"]
            val schema = "${this.packageName}:${this.title}"
            val savedImages = Gson().fromJson<HashMap<String, String>>(data,
                object : TypeToken<HashMap<String, String>>() {}.type)
            return if (savedImages.containsKey(schema))
                savedImages[schema]
            else {
                val result = repository.uploadImage(bitmap.toFile(this.context, "art"))
                result?.let {
                    savedImages[schema] = it
                    Prefs[Prefs.SAVED_ARTWORK] = Gson().toJson(savedImages)
                }
                result
            }
        }
    }
}
