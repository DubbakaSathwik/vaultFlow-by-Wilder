package com.example.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.VaultFlowApplication
import com.example.presentation.screens.*
import com.example.presentation.viewmodel.MainViewModel
import com.example.presentation.viewmodel.SettingsViewModel
import com.example.presentation.viewmodel.TransactionViewModel
import com.example.presentation.viewmodel.ProfileViewModel
import com.example.presentation.viewmodel.OcrViewModel
import com.example.presentation.viewmodel.RecurringPaymentsViewModel
import com.example.presentation.viewmodel.SearchViewModel
import com.example.presentation.viewmodel.IntelligenceViewModel
import androidx.navigation.toRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph(
    mainViewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    sharedImageUri: android.net.Uri? = null,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isOnboarded by mainViewModel.isOnboarded.collectAsState()

    androidx.compose.runtime.LaunchedEffect(sharedImageUri) {
        if (sharedImageUri != null) {
            navController.navigate(OcrImportDestination)
        }
    }

    // Show BottomBar and FAB only on standard primary dashboard tabs
    val showBottomBarAndFab = currentDestination?.let { dest ->
        dest.hasRoute<HomeDestination>() ||
        dest.hasRoute<TransactionDestination>() ||
        dest.hasRoute<BorrowDestination>() ||
        dest.hasRoute<AnalyticsDestination>() ||
        dest.hasRoute<ProfileDestination>()
    } ?: false

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBarAndFab) {
                NavigationBar(
                    modifier = Modifier.testTag("app_bottom_bar")
                ) {
                    val tabs = listOf(
                        Triple(HomeDestination, Icons.Default.Home, "Home"),
                        Triple(TransactionDestination, Icons.Default.Receipt, "Transactions"),
                        Triple(BorrowDestination, Icons.Default.Payments, "Borrow"),
                        Triple(AnalyticsDestination, Icons.Default.BarChart, "Analytics"),
                        Triple(ProfileDestination, Icons.Default.Person, "Profile")
                    )

                    tabs.forEach { (destination, icon, label) ->
                        val isSelected = currentDestination?.hasRoute(destination::class) ?: false
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(destination) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(imageVector = icon, contentDescription = label) },
                            label = { Text(text = label) },
                            modifier = Modifier.testTag("nav_tab_${label.lowercase()}")
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (showBottomBarAndFab) {
                FloatingActionButton(
                    onClick = { /* Non-functional placeholder */ },
                    shape = CircleShape,
                    modifier = Modifier.testTag("app_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Item")
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = SplashDestination,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            }
        ) {
            composable<SplashDestination> {
                SplashScreen(
                    onSplashFinished = {
                        navController.navigate(
                            if (isOnboarded) HomeDestination else OnboardingDestination
                        ) {
                            popUpTo(SplashDestination) { inclusive = true }
                        }
                    }
                )
            }

            composable<OnboardingDestination> {
                OnboardingScreen(
                    onFinished = {
                        mainViewModel.completeOnboarding()
                        navController.navigate(HomeDestination) {
                            popUpTo(OnboardingDestination) { inclusive = true }
                        }
                    }
                )
            }

            composable<HomeDestination> {
                HomeScreen(
                    onSettingsClick = { navController.navigate(SettingsDestination) },
                    onOcrImportClick = { navController.navigate(OcrImportDestination) },
                    onCategoriesClick = { navController.navigate(CategoryManagementDestination) },
                    onMerchantsClick = { navController.navigate(MerchantDatabaseDestination) },
                    onSavingsGoalsClick = { navController.navigate(SavingsGoalsDestination) },
                    onVaultClick = { navController.navigate(VaultDestination) },
                    modifier = Modifier.fillMaxSize()
                )
            }

            composable<TransactionDestination> {
                val context = LocalContext.current.applicationContext as VaultFlowApplication
                val appContainer = context.container
                val transactionViewModel: TransactionViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return TransactionViewModel(appContainer.vaultRepository) as T
                        }
                    }
                )
                TransactionScreen(viewModel = transactionViewModel)
            }

            composable<BorrowDestination> {
                BorrowScreen()
            }

            composable<AnalyticsDestination> {
                AnalyticsScreen()
            }

            composable<ProfileDestination> {
                val context = LocalContext.current.applicationContext as VaultFlowApplication
                val appContainer = context.container
                val profileViewModel: ProfileViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return ProfileViewModel(appContainer.vaultRepository) as T
                        }
                    }
                )
                ProfileScreen(viewModel = profileViewModel, settingsViewModel = settingsViewModel)
            }

            composable<SettingsDestination> {
                SettingsScreen(viewModel = settingsViewModel)
            }

            // mapped auxiliary routes for seamless feature plug-in in future prompts
            composable<OcrImportDestination> {
                val context = LocalContext.current.applicationContext as VaultFlowApplication
                val appContainer = context.container
                val ocrViewModel: OcrViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return OcrViewModel(appContainer.vaultRepository) as T
                        }
                    }
                )
                
                androidx.compose.runtime.LaunchedEffect(sharedImageUri) {
                    if (sharedImageUri != null) {
                        ocrViewModel.selectReceiptImage(context, sharedImageUri)
                    }
                }

                OcrImportScreen(viewModel = ocrViewModel)
            }

            composable<SavingsGoalsDestination> {
                SavingsGoalsScreen(onBackClick = { navController.navigateUp() })
            }

            composable<VaultDestination> {
                VaultScreen(onBackClick = { navController.navigateUp() })
            }

            composable<ReportsDestination> {
                ReportsScreen()
            }

            composable<RecurringPaymentsDestination> {
                val context = LocalContext.current.applicationContext as VaultFlowApplication
                val appContainer = context.container
                val recurringPaymentsViewModel: RecurringPaymentsViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return RecurringPaymentsViewModel(appContainer.vaultRepository) as T
                        }
                    }
                )
                RecurringPaymentsScreen(viewModel = recurringPaymentsViewModel)
            }

            composable<NotificationsDestination> {
                NotificationsScreen()
            }

            composable<SearchDestination> {
                val context = LocalContext.current.applicationContext as VaultFlowApplication
                val appContainer = context.container
                val searchViewModel: SearchViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return SearchViewModel(appContainer.vaultRepository) as T
                        }
                    }
                )
                SearchScreen(viewModel = searchViewModel)
            }

            composable<CategoryManagementDestination> {
                val context = LocalContext.current.applicationContext as VaultFlowApplication
                val appContainer = context.container
                val intelligenceViewModel: IntelligenceViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return IntelligenceViewModel(appContainer.vaultRepository) as T
                        }
                    }
                )
                CategoryManagementScreen(
                    viewModel = intelligenceViewModel,
                    onBackClick = { navController.popBackStack() },
                    onCategoryClick = { categoryId ->
                        navController.navigate(CategoryDetailsDestination(categoryId))
                    }
                )
            }

            composable<CategoryDetailsDestination> {
                val context = LocalContext.current.applicationContext as VaultFlowApplication
                val appContainer = context.container
                val args = it.toRoute<CategoryDetailsDestination>()
                val intelligenceViewModel: IntelligenceViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return IntelligenceViewModel(appContainer.vaultRepository) as T
                        }
                    }
                )
                CategoryDetailsScreen(
                    categoryId = args.categoryId,
                    viewModel = intelligenceViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable<MerchantDatabaseDestination> {
                val context = LocalContext.current.applicationContext as VaultFlowApplication
                val appContainer = context.container
                val intelligenceViewModel: IntelligenceViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return IntelligenceViewModel(appContainer.vaultRepository) as T
                        }
                    }
                )
                MerchantDatabaseScreen(
                    viewModel = intelligenceViewModel,
                    onBackClick = { navController.popBackStack() },
                    onMerchantClick = { merchantId ->
                        navController.navigate(MerchantDetailsDestination(merchantId))
                    }
                )
            }

            composable<MerchantDetailsDestination> {
                val context = LocalContext.current.applicationContext as VaultFlowApplication
                val appContainer = context.container
                val args = it.toRoute<MerchantDetailsDestination>()
                val intelligenceViewModel: IntelligenceViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return IntelligenceViewModel(appContainer.vaultRepository) as T
                        }
                    }
                )
                MerchantDetailsScreen(
                    merchantId = args.merchantId,
                    viewModel = intelligenceViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}

