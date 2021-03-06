package com.uu_uce.services

import android.app.Activity
import android.widget.Toast
import androidx.preference.PreferenceManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.*


@Suppress("BlockingMethodInNonBlockingContext")
        /**
         * Send POST containing user credentials to specified server in attempt to log in.
         * @param[username] the username with which to log in.
         * @param[password] a SHA256 hash of the password with which to log in.
         * @param[ip] the ip of the server to which the login POST should be sent.
         * @param[activity] the current activity.
         * @param[onCompleteAction] action to be executed when login result is received.
         */
fun login(username : String, password : String, ip : String, activity: Activity, onCompleteAction : ((responseCode : Int) -> Unit)){
    val loginApi = URL("$ip/api/user/login")
    val reqParam =
        URLEncoder.encode("username", "UTF-8") +
        "=" +
        URLEncoder.encode(username, "UTF-8") +
        "&" +
        URLEncoder.encode("password", "UTF-8") +
        "=" +
        URLEncoder.encode(password.toLowerCase(Locale.ROOT), "UTF-8")

    GlobalScope.launch {
        try {
            with(loginApi.openConnection() as HttpURLConnection) {
                // optional default is GET
                requestMethod = "POST"

                val wr = OutputStreamWriter(outputStream)
                wr.write(reqParam)
                wr.flush()

                println("URL : $url")
                println("Response Code : $responseCode")

                when (responseCode) {
                    HttpURLConnection.HTTP_OK -> {
                        val reader = BufferedReader(InputStreamReader(this.inputStream))
                        val jsonWebToken = reader.readLine()
                        reader.close()
                        val sharedPref = PreferenceManager.getDefaultSharedPreferences(activity)
                        with(sharedPref.edit()) {
                            putString("com.uu_uce.WEBTOKEN", jsonWebToken)
                            apply()
                        }
                        onCompleteAction(responseCode)
                    }
                    HttpURLConnection.HTTP_NOT_FOUND -> {
                        onCompleteAction(responseCode)
                    }
                    HttpURLConnection.HTTP_INTERNAL_ERROR -> {
                        onCompleteAction(responseCode)
                    }
                    else -> {
                        throw IOException("Server returned non-OK status: $responseCode")
                    }
                }
            }
        } catch (e: Exception) {
            activity.runOnUiThread {
                Toast.makeText(
                    activity,
                    "No connection to the server could be established",
                    Toast.LENGTH_LONG
                ).show()
            }
            e.printStackTrace()
        }
    }
}

/* This program has been developed by students from the bachelor Computer
# Science at Utrecht University within the Software Project course. ©️ Copyright
# Utrecht University (Department of Information and Computing Sciences)*/

