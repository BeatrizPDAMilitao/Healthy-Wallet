package com.example.ethktprototype

import android.graphics.Bitmap
import com.example.ethktprototype.data.NftValue
import com.example.ethktprototype.data.PatientEntity
import com.example.ethktprototype.data.PractitionerEntity
import com.example.ethktprototype.data.TokenBalance
import com.example.ethktprototype.data.Transaction

data class WalletUiState(
    val walletAddress: String = "",
    val userEnsName: String = "",
    val tokens: List<TokenBalance> = emptyList(),
    val nfts: List<NftValue> = emptyList(),
    val nftsLoading: Boolean = false,
    val totalBalanceUSD: Double = 0.0,
    val transactionHash: String = "",
    val selectedToken: TokenBalance? = null,
    val selectedNetwork: Network = Network.SEPOLIA,
    val isTokensLoading: Boolean = false,
    val isNftsLoading: Boolean = false,
    val showPayDialog: Boolean = false,
    val showTokenBottomSheet: Boolean = false,
    val showWalletModal: Boolean = false,
    val showSuccessModal: Boolean = false,
    val toAddress: String = "",
    val sentAmount: Double = 0.0,
    val sentCurrency: String = "",
    val tokenBlocklist: List<TokenBalance> = emptyList(),
    val hash: String = "",
    val ens: String = "",
    val mnemonicLoaded: Boolean = false,
    val tokensBlocked: List<TokenBalance> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val showRecordDialog: Boolean = false,
    val showDenyDialog: Boolean = false,
    val showSyncSuccessDialog: Boolean = false,
    val showSyncErrorDialog: Boolean = false,
    var isTransactionProcessing: Boolean = false,
    var showDataDialog: Boolean = false,
    var practitionerData: PractitionerEntity? = null,
    var medPlumToken: Boolean = false,
    var patientId: String = "",
    var hasFetchedPatient: Boolean = false,
    var hasFetchedDiagnosticReports: Boolean = false,
    var hasFetchedConditions: Boolean = false,
    var hasFetchedObservations: Boolean = false,
    var hasFetchedMedicationRequests: Boolean = false,
    var hasFetchedMedicationStatements: Boolean = false,
    var hasFetchedImmunizations: Boolean = false,
    var hasFetchedAllergies: Boolean = false,
    var hasFetchedDevices: Boolean = false,
    var hasFetchedProcedures: Boolean = false,
    )
