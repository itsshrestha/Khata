package com.khata.app.data.sync

import com.khata.app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

object SupabaseClientProvider {

    val client: SupabaseClient by lazy {
        val rawUrl = BuildConfig.SUPABASE_URL
        val cleanUrl = rawUrl
            .trim()
            .removeSuffix("/")
            .replace(Regex("/rest/v1/?$"), "")
            .removeSuffix("/")

        createSupabaseClient(
            supabaseUrl = cleanUrl,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY.trim(),
        ) {
            install(Postgrest)
            install(Auth)
            install(Realtime)
        }
    }
}
