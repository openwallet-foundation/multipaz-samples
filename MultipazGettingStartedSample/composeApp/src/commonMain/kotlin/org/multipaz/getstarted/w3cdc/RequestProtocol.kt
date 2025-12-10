package org.multipaz.getstarted.w3cdc

enum class RequestProtocol(
    val displayName: String,
    val exchangeProtocolNames: List<String>,
    val signRequest: Boolean,
) {
    W3C_DC_OPENID4VP_29(
        displayName = "OpenID4VP 1.0",
        exchangeProtocolNames = listOf("openid4vp-v1-signed"),
        signRequest = true,
    ),
    W3C_DC_OPENID4VP_29_UNSIGNED(
        displayName = "OpenID4VP 1.0 (Unsigned)",
        exchangeProtocolNames = listOf("openid4vp-v1-unsigned"),
        signRequest = false,
    ),
    W3C_DC_OPENID4VP_24(
        displayName = "OpenID4VP Draft 24",
        exchangeProtocolNames = listOf("openid4vp"),
        signRequest = true,
    ),
    W3C_DC_OPENID4VP_24_UNSIGNED(
        displayName = "OpenID4VP Draft 24 (Unsigned)",
        exchangeProtocolNames = listOf("openid4vp"),
        signRequest = false,
    ),
    W3C_DC_MDOC_API(
        displayName = "ISO 18013-7 Annex C",
        exchangeProtocolNames = listOf("org-iso-mdoc"),
        signRequest = true
    ),
    W3C_DC_MDOC_API_UNSIGNED(
        displayName = "ISO 18013-7 Annex C (Unsigned)",
        exchangeProtocolNames = listOf("org-iso-mdoc"),
        signRequest = false
    ),

    W3C_DC_MDOC_API_AND_OPENID4VP_29(
        displayName = "ISO 18013-7 Annex C + OpenID4VP 1.0",
        exchangeProtocolNames = listOf("org-iso-mdoc", "openid4vp-v1-signed"),
        signRequest = true
    ),
    W3C_DC_MDOC_API_AND_OPENID4VP_29_UNSIGNED(
        displayName = "ISO 18013-7 Annex C + OpenID4VP 1.0 (Unsigned)",
        exchangeProtocolNames = listOf("org-iso-mdoc", "openid4vp-v1-unsigned"),
        signRequest = false
    ),
    W3C_DC_MDOC_API_AND_OPENID4VP_24(
        displayName = "ISO 18013-7 Annex C + OpenID4VP Draft 24",
        exchangeProtocolNames = listOf("org-iso-mdoc", "openid4vp"),
        signRequest = true
    ),
    W3C_DC_MDOC_API_AND_OPENID4VP_24_UNSIGNED(
        displayName = "ISO 18013-7 Annex C + OpenID4VP Draft 24 (Unsigned)",
        exchangeProtocolNames = listOf("org-iso-mdoc", "openid4vp"),
        signRequest = false
    ),

    OPENID4VP_29_AND_W3C_DC_MDOC_API(
        displayName = "OpenID4VP 1.0 + ISO 18013-7 Annex C",
        exchangeProtocolNames = listOf("openid4vp-v1-signed", "org-iso-mdoc"),
        signRequest = true
    ),
    OPENID4VP_29_UNSIGNED_AND_W3C_DC_MDOC_API(
        displayName = "OpenID4VP 1.0 + ISO 18013-7 Annex C (Unsigned)",
        exchangeProtocolNames = listOf("openid4vp-v1-unsigned", "org-iso-mdoc"),
        signRequest = false
    ),
    OPENID4VP_24_AND_W3C_DC_MDOC_API(
        displayName = "OpenID4VP Draft 24 + ISO 18013-7 Annex C",
        exchangeProtocolNames = listOf("openid4vp", "org-iso-mdoc"),
        signRequest = true
    ),
    OPENID4VP_24_UNSIGNED_AND_W3C_DC_MDOC_API(
        displayName = "OpenID4VP Draft 24 + ISO 18013-7 Annex C (Unsigned)",
        exchangeProtocolNames = listOf("openid4vp", "org-iso-mdoc"),
        signRequest = false
    ),
}
