package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.BottomNavBar
import com.example.ui.components.FloatingCartBar
import com.example.ui.components.ProductDetailSheet
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.example.ui.screens.CartSheet
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LandingScreen
import com.example.ui.screens.OrderAgainScreen
import com.example.ui.screens.OrderTrackingScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RecipesScreen
import com.example.ui.screens.RoleApplicationScreen
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.DeliveryPartnerScreen
import com.example.ui.screens.RoleSelectionScreen
import com.example.ui.screens.StaffAuthScreen
import com.example.ui.screens.UserRole
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.GreenShopViewModel
import com.example.data.applications.ApplicationRepository
import com.example.data.applications.ApplicationApiException
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val greenShopViewModel: GreenShopViewModel = viewModel()
            val authViewModel: AuthViewModel = viewModel()
            val isDark by greenShopViewModel.isDarkMode.collectAsState()
            val authState by authViewModel.uiState.collectAsState()
            // Customer phone login is the primary entry point. Staff can still reach the
            // existing server-authorized role chooser from the back affordance.
            var selectedRole by rememberSaveable {
                mutableStateOf<UserRole?>(if (authState.isAuthenticated) null else UserRole.CUSTOMER)
            }
            val applicationRepository = remember { ApplicationRepository() }
            val authScope = rememberCoroutineScope()
            var adminApproved by remember { mutableStateOf(false) }
            var partnerApproved by remember { mutableStateOf(false) }

            LaunchedEffect(authState.sessionRestored) {
                if (authState.sessionRestored) {
                    val token = authViewModel.validAccessToken(false).getOrNull()
                    val access = token?.let { applicationRepository.getRoleAccess(it).getOrNull() }
                    selectedRole = when {
                        access?.admin == true -> UserRole.ADMIN
                        access?.deliveryPartner == true -> UserRole.DELIVERY_PARTNER
                        else -> UserRole.CUSTOMER
                    }
                }
            }

            suspend fun refreshAuthoritativeAccess() {
                val token = authViewModel.validAccessToken(false).getOrNull() ?: return
                var result = applicationRepository.getRoleAccess(token)
                if ((result.exceptionOrNull() as? ApplicationApiException)?.statusCode == 401) {
                    result = authViewModel.validAccessToken(true).fold(
                        onSuccess = { applicationRepository.getRoleAccess(it) },
                        onFailure = { Result.failure(it) }
                    )
                }
                result.onSuccess {
                    adminApproved = it.admin
                    partnerApproved = it.deliveryPartner
                }
            }

            LaunchedEffect(authState.isAuthenticated, selectedRole) {
                adminApproved = false
                partnerApproved = false
                if (authState.isAuthenticated && selectedRole != UserRole.CUSTOMER) {
                    refreshAuthoritativeAccess()
                }
            }

            LaunchedEffect(selectedRole) {
                authViewModel.selectLoginRole(when(selectedRole){UserRole.ADMIN->"admin";UserRole.DELIVERY_PARTNER->"delivery_partner";else->"customer"})
            }

            MyApplicationTheme(darkTheme = isDark) {
                if (authState.isRestoringSession) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else when (val role = selectedRole) {
                    null -> RoleSelectionScreen(onSelectRole = { selectedRole = it })
                    UserRole.CUSTOMER -> if (!authState.isAuthenticated) {
                        LandingScreen(
                            otpSent = authState.otpSent,
                            isLoading = authState.isLoading,
                            isConfigured = authState.isConfigured,
                            errorMessage = authState.errorMessage,
                            isDevelopmentTestOtpEnabled = authState.isDevelopmentTestOtpEnabled,
                            developmentTestPhoneNumber = authState.developmentTestPhoneNumber,
                            onSendOtp = authViewModel::sendOtp,
                            onVerifyOtp = authViewModel::verifyOtp,
                            onChangePhoneNumber = authViewModel::changePhoneNumber,
                            onClearError = authViewModel::clearError,
                            onBack = {
                                authViewModel.changePhoneNumber()
                                selectedRole = null
                            },
                            selectedRole = UserRole.CUSTOMER,
                            onSelectRole = { authViewModel.changePhoneNumber(); selectedRole = it }
                        )
                    } else {
                        GreenShopApp(viewModel = greenShopViewModel, authenticatedUser = authState.currentUser, onRefreshIdentity = authViewModel::refreshIdentity, onSignOut = {
                            greenShopViewModel.clearCustomerSessionState()
                            authViewModel.signOut()
                            selectedRole = UserRole.CUSTOMER
                        })
                    }
                    UserRole.ADMIN, UserRole.DELIVERY_PARTNER -> if (!authState.isAuthenticated) {
                        StaffAuthScreen(
                            role = role,
                            loading = authState.isLoading,
                            error = authState.errorMessage,
                            otpSent = authState.otpSent,
                            onSendOtp = authViewModel::sendOtp,
                            onVerifyOtp = authViewModel::verifyOtp,
                            onChangePhoneNumber = authViewModel::changePhoneNumber,
                            developmentPhone = authState.developmentRolePhones[
                                if (role == UserRole.ADMIN) "admin" else "delivery_partner"
                            ],
                            localDevelopmentAuth = authState.isLocalDevelopmentAuth,
                            onClearMessage = authViewModel::clearError,
                            isConfigured = authState.isConfigured,
                            onSelectRole = { authViewModel.changePhoneNumber(); selectedRole = it },
                            onBack = {
                                authViewModel.clearError()
                                selectedRole = null
                            }
                        )
                    } else if (role == UserRole.ADMIN && adminApproved) {
                        AdminDashboardScreen(
                            tokenProvider = authViewModel::validAccessToken,
                            onSignOut = { adminApproved = false; authViewModel.signOut(); selectedRole = null }
                        )
                    } else if (role == UserRole.DELIVERY_PARTNER && partnerApproved) {
                        DeliveryPartnerScreen(
                            tokenProvider = authViewModel::validAccessToken,
                            onSignOut = { partnerApproved = false; authViewModel.signOut(); selectedRole = null }
                        )
                    } else {
                        RoleApplicationScreen(
                            role = role,
                            accessToken = authViewModel.accessToken(),
                            onBack = { selectedRole = null },
                            onSignOut = {
                                authViewModel.signOut()
                                selectedRole = null
                            },
                            onApproved = {
                                authScope.launch { refreshAuthoritativeAccess() }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GreenShopApp(
    viewModel: GreenShopViewModel,
    authenticatedUser: com.example.data.auth.AuthenticatedUser?,
    onRefreshIdentity: () -> Unit,
    onSignOut: () -> Unit
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val cartCount by viewModel.cartCount.collectAsState()
    val cartTotal by viewModel.cartTotal.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val selectedProductDetail by viewModel.selectedProductDetail.collectAsState()

    var showCartSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val productDetailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Handle back button on secondary screens
    BackHandler(enabled = currentScreen != AppScreen.HOME) {
        viewModel.navigateBack()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.statusBars,
        bottomBar = {
            ColumnNavigationWrapper(
                currentScreen = currentScreen,
                cartCount = cartCount,
                cartItems = cartItems,
                cartTotal = cartTotal,
                onViewCartClick = { showCartSheet = true },
                onNavigate = { screen -> viewModel.navigateTo(screen) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.HOME -> HomeScreen(
                    viewModel = viewModel,
                    onViewCartClick = { showCartSheet = true }
                )
                AppScreen.CATEGORIES -> CategoriesScreen(
                    viewModel = viewModel,
                    onViewCartClick = { showCartSheet = true }
                )
                AppScreen.ORDER_AGAIN -> OrderAgainScreen(
                    viewModel = viewModel
                )
                AppScreen.RECIPES -> RecipesScreen(
                    viewModel = viewModel
                )
                AppScreen.WALLET -> WalletScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateBack() }
                )
                AppScreen.PROFILE -> ProfileScreen(
                    viewModel = viewModel,
                    authenticatedUser = authenticatedUser,
                    onRefreshIdentity = onRefreshIdentity,
                    onBack = { viewModel.navigateBack() },
                    onSignOut = onSignOut
                )
                AppScreen.SEARCH -> SearchScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateBack() }
                )
                AppScreen.ORDER_TRACKING -> OrderTrackingScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateTo(AppScreen.HOME) }
                )
            }
        }
    }

    if (showCartSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCartSheet = false },
            sheetState = sheetState
        ) {
            CartSheet(
                viewModel = viewModel,
                onDismiss = { showCartSheet = false }
            )
        }
    }

    if (selectedProductDetail != null) {
        val product = selectedProductDetail!!
        val qty = viewModel.getProductQuantity(product.id)
        val isFav = favoriteIds.contains(product.id)

        ModalBottomSheet(
            onDismissRequest = { viewModel.hideProductDetail() },
            sheetState = productDetailSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            ProductDetailSheet(
                product = product,
                currentQuantity = qty,
                isFavorite = isFav,
                onAdd = { viewModel.addToCart(product) },
                onRemove = { viewModel.removeFromCart(product.id) },
                onToggleFavorite = { viewModel.toggleFavorite(product.id) },
                onDismiss = { viewModel.hideProductDetail() },
                onViewCart = {
                    viewModel.hideProductDetail()
                    showCartSheet = true
                }
            )
        }
    }
}

@Composable
fun ColumnNavigationWrapper(
    currentScreen: AppScreen,
    cartCount: Int,
    cartItems: List<com.example.model.CartItem>,
    cartTotal: Double,
    onViewCartClick: () -> Unit,
    onNavigate: (AppScreen) -> Unit
) {
    androidx.compose.foundation.layout.Column(
        modifier = androidx.compose.ui.Modifier.fillMaxWidth()
    ) {
        // Floating Cart Bar (visible on Home, Categories, Recipes, Order Again when items present)
        val showFloatingCart = cartCount > 0 && currentScreen in listOf(
            AppScreen.HOME,
            AppScreen.CATEGORIES,
            AppScreen.RECIPES,
            AppScreen.ORDER_AGAIN,
            AppScreen.SEARCH
        )

        if (showFloatingCart) {
            FloatingCartBar(
                cartItems = cartItems,
                totalCount = cartCount,
                totalPrice = cartTotal,
                onViewCartClick = onViewCartClick
            )
        }

        // Show Bottom Navigation on main tabs
        val showBottomNav = currentScreen in listOf(
            AppScreen.HOME,
            AppScreen.ORDER_AGAIN,
            AppScreen.CATEGORIES,
            AppScreen.PROFILE
        )

        if (showBottomNav) {
            BottomNavBar(
                currentScreen = currentScreen,
                cartCount = cartCount,
                onCartClick = onViewCartClick,
                onNavigate = onNavigate
            )
        }
    }
}
