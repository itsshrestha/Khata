package com.khata.app.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.khata.app.ui.credit.AddCreditRoute
import com.khata.app.ui.customer.CustomerDetailsRoute
import com.khata.app.ui.customer.CustomerStatementRoute
import com.khata.app.ui.customers.AddEditCustomerRoute
import com.khata.app.ui.customers.CustomerListRoute
import com.khata.app.ui.dashboard.DashboardRoute
import com.khata.app.ui.payment.RecordPaymentRoute
import com.khata.app.ui.reports.ReportsRoute
import com.khata.app.ui.settings.SettingsRoute
import com.khata.app.ui.transactions.TransactionsRoute
import com.khata.app.ui.navigation.Routes
import com.khata.app.ui.navigation.TopLevelDestination
import com.khata.app.ui.placeholder.PlaceholderScreen

private val customerIdArguments = listOf(
    navArgument(Routes.ARG_CUSTOMER_ID) { type = NavType.StringType },
)

private fun NavBackStackEntry.customerId(): String =
    requireNotNull(arguments).getString(Routes.ARG_CUSTOMER_ID).orEmpty()

@Composable
fun KhataApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    // The bottom bar belongs to the five main sections only; detail screens use the full height.
    val showBottomBar = TopLevelDestination.entries.any { destination ->
        currentDestination?.hierarchy?.any { it.route == destination.route } == true
    }

    Scaffold(
        // Each screen handles its own top (status bar) inset; the NavigationBar handles the bottom one.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true,
                            onClick = {
                                val isDashboard = destination.route == TopLevelDestination.Dashboard.route
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = !isDashboard
                                    }
                                    launchSingleTop = true
                                    restoreState = !isDashboard
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.Dashboard.route,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            composable(TopLevelDestination.Dashboard.route) {
                DashboardRoute(
                    onNavigateToCustomerDetails = { id -> navController.navigate(Routes.customerDetails(id)) },
                    onNavigateToCustomers = { navController.navigate(TopLevelDestination.Customers.route) },
                    onAddCustomer = { navController.navigate(Routes.ADD_CUSTOMER) },
                )
            }
            composable(TopLevelDestination.Customers.route) {
                CustomerListRoute(
                    onAddCustomer = { navController.navigate(Routes.ADD_CUSTOMER) },
                    onCustomerClick = { id -> navController.navigate(Routes.customerDetails(id)) },
                )
            }
            composable(TopLevelDestination.Transactions.route) {
                TransactionsRoute(
                    onTransactionClick = { customerId -> navController.navigate(Routes.customerDetails(customerId)) },
                )
            }
            composable(TopLevelDestination.Reports.route) {
                ReportsRoute(
                    onCustomerClick = { customerId -> navController.navigate(Routes.customerDetails(customerId)) },
                )
            }
            composable(TopLevelDestination.Settings.route) {
                SettingsRoute()
            }

            // ---- Customers ----
            composable(Routes.ADD_CUSTOMER) {
                AddEditCustomerRoute(
                    customerId = null,
                    onBack = { navController.popBackStack() },
                    // Go straight to the new customer, ready for "Add Credit".
                    onSaved = { newId ->
                        navController.navigate(Routes.customerDetails(newId)) {
                            popUpTo(Routes.ADD_CUSTOMER) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.EDIT_CUSTOMER, arguments = customerIdArguments) { entry ->
                AddEditCustomerRoute(
                    customerId = entry.customerId(),
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }
            composable(Routes.CUSTOMER_DETAILS, arguments = customerIdArguments) { entry ->
                val customerId = entry.customerId()
                val feedbackMessage by entry.savedStateHandle
                    .getStateFlow<String?>(Routes.RESULT_MESSAGE, null)
                    .collectAsStateWithLifecycle()
                val navigateToDashboard = {
                    navController.navigate(TopLevelDestination.Dashboard.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = false }
                        launchSingleTop = true
                        restoreState = false
                    }
                }
                CustomerDetailsRoute(
                    customerId = customerId,
                    feedbackMessage = feedbackMessage,
                    onFeedbackShown = { entry.savedStateHandle[Routes.RESULT_MESSAGE] = null },
                    onBack = {
                        if (!navController.popBackStack()) {
                            navigateToDashboard()
                        }
                    },
                    onNavigateToDashboard = navigateToDashboard,
                    onEdit = { navController.navigate(Routes.editCustomer(customerId)) },
                    onAddCredit = { navController.navigate(Routes.addCredit(customerId)) },
                    onRecordPayment = { navController.navigate(Routes.recordPayment(customerId)) },
                    onViewStatement = { navController.navigate(Routes.customerStatement(customerId)) },
                )
            }
            composable(Routes.CUSTOMER_STATEMENT, arguments = customerIdArguments) { entry ->
                CustomerStatementRoute(
                    customerId = entry.customerId(),
                    onBack = { navController.popBackStack() },
                )
            }

            // ---- Built in the next phases ----
            composable(Routes.ADD_CREDIT, arguments = customerIdArguments) { entry ->
                AddCreditRoute(
                    customerId = entry.customerId(),
                    onBack = { navController.popBackStack() },
                    onSaved = {
                        // Hand a success message to the customer screen, which shows it as a snackbar.
                        navController.previousBackStackEntry?.savedStateHandle
                            ?.set(Routes.RESULT_MESSAGE, "✓ Credit added successfully")
                        navController.popBackStack()
                    },
                )
            }
            composable(Routes.RECORD_PAYMENT, arguments = customerIdArguments) { entry ->
                val container = LocalAppContainer.current
                RecordPaymentRoute(
                    customerId = entry.customerId(),
                    onBack = { navController.popBackStack() },
                    onPaymentRecorded = { receipt ->
                        val remainingText = container.currencyFormatter.format(receipt.remainingOutstanding)
                        val message = "✓ Payment recorded\nRemaining balance: $remainingText"
                        navController.previousBackStackEntry?.savedStateHandle
                            ?.set(Routes.RESULT_MESSAGE, message)
                        navController.popBackStack()
                    },
                )
            }
        }
    }
}
