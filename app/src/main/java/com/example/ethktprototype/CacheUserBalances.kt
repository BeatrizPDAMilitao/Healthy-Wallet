package com.example.ethktprototype

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.example.ethktprototype.data.NftValue
import com.example.ethktprototype.data.TokenBalance
import com.google.common.reflect.TypeToken
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun getBalancesSharedPreferences(application: Application): SharedPreferences {
    return application.getSharedPreferences("WalletPrefs", Context.MODE_PRIVATE)
}

fun getTokenBalancesSharedPreferencesKey(): String {
    return "TOKEN_BALANCES"
}

fun cacheUserBalance(tokenBalance: List<TokenBalance>, application: Application) {
    val sharedPreferences = getBalancesSharedPreferences(application)

    val json = Json.encodeToString(tokenBalance)
    sharedPreferences.edit().putString(getTokenBalancesSharedPreferencesKey(), json).apply()
}
fun cacheTotalBalanceUSD(totalBalanceUSD: Double, application: Application) {
    val sharedPreferences = getBalancesSharedPreferences(application)
    sharedPreferences.edit().putFloat("TOTAL_BALANCE_USD", totalBalanceUSD.toFloat()).apply()
}






