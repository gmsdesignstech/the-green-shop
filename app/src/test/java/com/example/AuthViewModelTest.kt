package com.example

import com.example.data.auth.AuthRepository
import com.example.data.auth.AuthenticatedUser
import com.example.viewmodel.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun unconfiguredAuthenticationFailsClosed() = runTest(dispatcher) {
        val viewModel = AuthViewModel(FakeAuthRepository(isConfigured = false))

        viewModel.sendOtp("9876543210")

        assertFalse(viewModel.uiState.value.otpSent)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("not configured"))
    }

    @Test
    fun successfulOtpFlowRequiresRepositoryVerification() = runTest(dispatcher) {
        val repository = FakeAuthRepository(isConfigured = true)
        val viewModel = AuthViewModel(repository)

        viewModel.sendOtp("9876543210")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.otpSent)
        assertFalse(viewModel.uiState.value.isAuthenticated)

        viewModel.verifyOtp("123456")
        advanceUntilIdle()

        assertTrue(repository.verifyCalled)
        assertTrue(viewModel.uiState.value.isAuthenticated)
    }

    @Test
    fun invalidOtpNeverCallsBackend() = runTest(dispatcher) {
        val repository = FakeAuthRepository(isConfigured = true)
        val viewModel = AuthViewModel(repository)
        viewModel.sendOtp("9876543210")
        advanceUntilIdle()

        viewModel.verifyOtp("12")
        advanceUntilIdle()

        assertFalse(repository.verifyCalled)
        assertFalse(viewModel.uiState.value.isAuthenticated)
    }

    @Test
    fun invalidPhoneNumberNeverRequestsOtp() = runTest(dispatcher) {
        val repository = FakeAuthRepository(isConfigured = true)
        val viewModel = AuthViewModel(repository)

        viewModel.sendOtp("12345")

        assertFalse(repository.sendCalled)
        assertEquals("Enter a valid 10-digit mobile number", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun invalidIndianPrefixNeverRequestsOtp() = runTest(dispatcher) {
        val repository = FakeAuthRepository(isConfigured = true)
        val viewModel = AuthViewModel(repository)

        viewModel.sendOtp("1234567890")

        assertFalse(repository.sendCalled)
        assertFalse(viewModel.uiState.value.otpSent)
    }

    @Test
    fun phoneIsConvertedToIndianE164BeforeRequest() = runTest(dispatcher) {
        val repository = FakeAuthRepository(isConfigured = true)
        val viewModel = AuthViewModel(repository)

        viewModel.sendOtp("9876543210")
        advanceUntilIdle()

        assertEquals("+919876543210", repository.sentPhone)
    }

    @Test
    fun expiredOtpDoesNotAuthenticate() = runTest(dispatcher) {
        val repository = FakeAuthRepository(
            isConfigured = true,
            verifyResult = Result.failure(IllegalStateException("OTP expired"))
        )
        val viewModel = AuthViewModel(repository)
        viewModel.sendOtp("9876543210")
        advanceUntilIdle()

        viewModel.verifyOtp("654321")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isAuthenticated)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("expired"))
    }

    @Test
    fun unreachableDevelopmentBackendShowsActionableError() = runTest(dispatcher) {
        val repository = FakeAuthRepository(
            isConfigured = true,
            verifyResult = Result.failure(java.io.IOException("Authentication server unreachable at 192.168.1.20:3000"))
        )
        val viewModel = AuthViewModel(repository)
        viewModel.sendOtp("9876543210")
        advanceUntilIdle()

        viewModel.verifyOtp("123456")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isAuthenticated)
        assertEquals(
            "Couldn't reach the development API. Check this build's API_BASE_URL, make sure the phone and computer are on the same Wi-Fi, and allow port 3000 through the computer's firewall.",
            viewModel.uiState.value.errorMessage
        )
    }

    @Test
    fun productionModeDoesNotAcceptFixedOtpLocally() = runTest(dispatcher) {
        val repository = FakeAuthRepository(
            isConfigured = true,
            verifyResult = Result.failure(IllegalStateException("Invalid OTP"))
        )
        val viewModel = AuthViewModel(repository)
        viewModel.sendOtp("9876543210")
        advanceUntilIdle()

        viewModel.verifyOtp("123456")
        advanceUntilIdle()

        assertTrue(repository.verifyCalled)
        assertFalse(viewModel.uiState.value.isAuthenticated)
        assertFalse(viewModel.uiState.value.isDevelopmentTestOtpEnabled)
    }

    @Test
    fun developmentModeIsOnlyAnIndicatorAndStillUsesProviderVerification() = runTest(dispatcher) {
        val repository = FakeAuthRepository(
            isConfigured = true,
            testModeEnabled = true,
            testPhone = "+919876543210"
        )
        val viewModel = AuthViewModel(repository)
        viewModel.sendOtp("9876543210")
        advanceUntilIdle()
        viewModel.verifyOtp("123456")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isDevelopmentTestOtpEnabled)
        assertEquals("+919876543210", viewModel.uiState.value.developmentTestPhoneNumber)
        assertTrue(repository.verifyCalled)
        assertTrue(viewModel.uiState.value.isAuthenticated)
    }

    @Test
    fun logoutClearsTheAuthenticatedState() = runTest(dispatcher) {
        val repository = FakeAuthRepository(isConfigured = true)
        repository.createSession()
        val viewModel = AuthViewModel(repository)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isAuthenticated)

        viewModel.signOut()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isAuthenticated)
        assertFalse(repository.hasSession())
    }
}

private class FakeAuthRepository(
    override val isConfigured: Boolean,
    private val verifyResult: Result<Unit> = Result.success(Unit),
    testModeEnabled: Boolean = false,
    testPhone: String? = null,
) : AuthRepository {
    var verifyCalled = false
    var sendCalled = false
    var sentPhone: String? = null
    private var session = false
    override val isDevelopmentTestOtpEnabled = testModeEnabled
    override val developmentTestPhoneNumber = testPhone

    override fun hasSession(): Boolean = session

    override suspend fun currentUser(): Result<AuthenticatedUser?> = Result.success(
        if (session) AuthenticatedUser("test-user", "+919876543210", "Test customer") else null
    )

    override suspend fun sendPhoneOtp(phone: String): Result<Unit> {
        sendCalled = true
        sentPhone = phone
        return Result.success(Unit)
    }

    override suspend fun verifyPhoneOtp(phone: String, token: String): Result<Unit> {
        verifyCalled = true
        if (verifyResult.isSuccess) session = true
        return verifyResult
    }

    fun createSession() {
        session = true
    }

    override suspend fun signOut(): Result<Unit> {
        session = false
        return Result.success(Unit)
    }
}
