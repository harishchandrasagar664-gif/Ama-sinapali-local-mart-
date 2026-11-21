package com.example.service

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

sealed class PhoneAuthSignInResult {
    data class Success(val user: FirebaseUser) : PhoneAuthSignInResult()
    data class Failure(val message: String) : PhoneAuthSignInResult()
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

object FirebasePhoneAuthHelper {

    fun currentUser(): FirebaseUser? = try {
        FirebaseAuth.getInstance().currentUser
    } catch (_: Throwable) {
        null
    }

    fun currentUid(): String? = currentUser()?.uid?.takeIf { it.isNotBlank() }

    fun signOut() {
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (_: Throwable) { }
    }

    /**
     * Normalizes a 10-digit Indian mobile number into E.164 (+91XXXXXXXXXX).
     */
    fun normalizeIndianPhoneToE164(raw: String): String {
        val trimmed = raw.trim().replace(" ", "").replace("-", "")
        if (trimmed.startsWith("+") && trimmed.length >= 11) {
            return trimmed
        }
        val digits = trimmed.filter { it.isDigit() }
        return when {
            digits.length == 10 -> "+91$digits"
            digits.length == 12 && digits.startsWith("91") -> "+$digits"
            digits.length > 10 && trimmed.startsWith("0") -> "+91${digits.trimStart('0').takeLast(10)}"
            else -> "+91${digits.takeLast(10)}"
        }
    }

    fun extractTenDigitPhone(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return if (digits.length >= 10) digits.takeLast(10) else digits
    }

    fun sendOtp(
        activity: Activity,
        rawPhone: String,
        resendToken: PhoneAuthProvider.ForceResendingToken? = null,
        onCodeSent: (verificationId: String, token: PhoneAuthProvider.ForceResendingToken) -> Unit,
        onAutoVerified: (PhoneAuthCredential) -> Unit,
        onFailed: (message: String) -> Unit
    ) {
        val e164Phone = normalizeIndianPhoneToE164(rawPhone)
        val digits = extractTenDigitPhone(rawPhone)
        if (digits.length < 10) {
            onFailed("Please enter a valid 10-digit mobile number.")
            return
        }

        val auth = FirebaseAuth.getInstance()
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                onAutoVerified(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                onFailed(mapFirebaseAuthException(e))
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                onCodeSent(verificationId, token)
            }
        }

        val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(e164Phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)

        if (resendToken != null) {
            optionsBuilder.setForceResendingToken(resendToken)
        }

        try {
            PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
        } catch (e: Throwable) {
            onFailed(mapFirebaseAuthException(e))
        }
    }

    suspend fun signInWithOtpCode(
        verificationId: String,
        smsCode: String
    ): PhoneAuthSignInResult = withContext(Dispatchers.IO) {
        val cleanCode = smsCode.trim()
        if (verificationId.isBlank()) {
            return@withContext PhoneAuthSignInResult.Failure("Verification session expired. Please request a new OTP.")
        }
        if (cleanCode.length < 6) {
            return@withContext PhoneAuthSignInResult.Failure("Please enter the complete 6-digit OTP.")
        }
        val credential = PhoneAuthProvider.getCredential(verificationId, cleanCode)
        signInWithPhoneCredential(credential)
    }

    suspend fun signInWithPhoneCredential(
        credential: PhoneAuthCredential
    ): PhoneAuthSignInResult = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            FirebaseAuth.getInstance()
                .signInWithCredential(credential)
                .addOnSuccessListener { result ->
                    val firebaseUser = result.user
                    if (firebaseUser != null && firebaseUser.uid.isNotBlank()) {
                        if (continuation.isActive) {
                            continuation.resume(PhoneAuthSignInResult.Success(firebaseUser))
                        }
                    } else {
                        if (continuation.isActive) {
                            continuation.resume(
                                PhoneAuthSignInResult.Failure("Authentication completed without a valid Firebase user.")
                            )
                        }
                    }
                }
                .addOnFailureListener { e ->
                    if (continuation.isActive) {
                        continuation.resume(PhoneAuthSignInResult.Failure(mapFirebaseAuthException(e)))
                    }
                }
        }
    }

    fun mapFirebaseAuthException(e: Throwable): String {
        return when (e) {
            is FirebaseAuthInvalidCredentialsException -> {
                val code = (e as? FirebaseAuthException)?.errorCode
                when (code) {
                    "ERROR_INVALID_VERIFICATION_CODE" -> "Invalid OTP code. Please check the 6-digit SMS code and try again."
                    "ERROR_SESSION_EXPIRED" -> "OTP has expired. Please tap Resend OTP."
                    else -> "Invalid phone number or OTP verification code."
                }
            }
            is FirebaseTooManyRequestsException ->
                "Too many OTP attempts from this device. Please wait a few minutes and try again."
            is FirebaseNetworkException ->
                "Network error. Please check your internet connection and retry."
            is FirebaseAuthException -> when (e.errorCode) {
                "ERROR_OPERATION_NOT_ALLOWED" ->
                    "Phone OTP login is not enabled in Firebase Console yet. Enable Phone Authentication in Firebase Console."
                "ERROR_APP_NOT_AUTHORIZED" ->
                    "App fingerprint (SHA-1/SHA-256) is not authorized in Firebase Console for Phone Auth."
                else -> e.localizedMessage ?: "Firebase Phone Authentication failed."
            }
            else -> e.localizedMessage ?: "Phone OTP verification failed."
        }
    }
}
