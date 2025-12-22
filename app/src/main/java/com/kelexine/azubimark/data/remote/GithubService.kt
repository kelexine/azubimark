package com.kelexine.azubimark.data.remote

import com.kelexine.azubimark.data.model.GithubUser
import retrofit2.http.GET
import retrofit2.http.Path

interface GithubService {
    @GET("users/{username}")
    suspend fun getUser(@Path("username") username: String): GithubUser
}
