package com.kirkbushman.araw.http

import com.kirkbushman.araw.http.base.Envelope
import com.kirkbushman.araw.http.base.EnvelopeKind
import com.kirkbushman.araw.models.Trophy
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class EnvelopedTrophy(

    @Json(name = "kind")
    override val kind: EnvelopeKind,

    @Json(name = "data")
    override val data: Trophy

) : Envelope<Trophy>
