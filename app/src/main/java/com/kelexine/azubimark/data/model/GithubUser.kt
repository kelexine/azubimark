package com.kelexine.azubimark.data.model

import com.squareup.moshi.Json

data class GithubUser(
    @Json(name = "name") val name: String?,
    @Json(name = "bio") val bio: String?,
    @Json(name = "avatar_url") val avatarUrl: String?,
    @Json(name = "html_url") val htmlUrl: String?
)
