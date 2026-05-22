package com.example.mapenumkm.presentation.screens.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mapenumkm.domain.model.Transaction
import com.example.mapenumkm.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

data class ChartData(
    val value: Float,
    val label: String
)

data class ReportUiState(
    val totalSales: Double = 0.0,
    val totalTransactions: Int = 0,
    val totalProductsSold: Int = 0,
    val averageTransactionValue: Double = 0.0,
    val transactions: List<Transaction> = emptyList(),
    val graphData: List<ChartData> = emptyList(),
    val selectedFilter: ReportFilter = ReportFilter.DAILY,
    val isLoading: Boolean = true
)

enum class ReportFilter {
    DAILY, WEEKLY, MONTHLY
}

class ReportViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(ReportFilter.DAILY)

    val uiState: StateFlow<ReportUiState> = combine(
        transactionRepository.getAllTransactions(),
        _filter
    ) { transactions, filter ->
        val now = Clock.System.now()
        val systemTZ = TimeZone.currentSystemDefault()
        val today = now.toLocalDateTime(systemTZ).date

        val filteredTransactions = when (filter) {
            ReportFilter.DAILY -> transactions.filter {
                it.createdAt.toLocalDateTime(systemTZ).date == today
            }
            ReportFilter.WEEKLY -> {
                val startOfWeek = now.minus(7, DateTimeUnit.DAY, systemTZ)
                transactions.filter { it.createdAt >= startOfWeek }
            }
            ReportFilter.MONTHLY -> {
                val startOfMonth = now.toLocalDateTime(systemTZ).let {
                    it.date.minus(it.dayOfMonth - 1, DateTimeUnit.DAY)
                }
                transactions.filter {
                    it.createdAt.toLocalDateTime(systemTZ).date >= startOfMonth
                }
            }
        }

        val totalSales = filteredTransactions.sumOf { it.total }
        val totalProductsSold = filteredTransactions.sumOf { t -> t.items.sumOf { it.quantity } }

        val graphData = calculateGraphData(filteredTransactions, filter, systemTZ)

        ReportUiState(
            totalSales = totalSales,
            totalTransactions = filteredTransactions.size,
            totalProductsSold = totalProductsSold,
            averageTransactionValue = if (filteredTransactions.isNotEmpty()) totalSales / filteredTransactions.size else 0.0,
            transactions = filteredTransactions,
            graphData = graphData,
            selectedFilter = filter,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportUiState()
    )

    private fun calculateGraphData(
        transactions: List<Transaction>,
        filter: ReportFilter,
        timeZone: TimeZone
    ): List<ChartData> {
        return when (filter) {
            ReportFilter.DAILY -> {
                // Hourly for today (0-23)
                (0..23 step 2).map { hour ->
                    val total = transactions.filter {
                        it.createdAt.toLocalDateTime(timeZone).hour == hour
                    }.sumOf { it.total }.toFloat()
                    ChartData(total, "${hour.toString().padStart(2, '0')}:00")
                }
            }
            ReportFilter.WEEKLY -> {
                // Last 7 days
                val now = Clock.System.now().toLocalDateTime(timeZone).date
                (6 downTo 0).map { i ->
                    val date = now.minus(i, DateTimeUnit.DAY)
                    val total = transactions.filter {
                        it.createdAt.toLocalDateTime(timeZone).date == date
                    }.sumOf { it.total }.toFloat()
                    ChartData(total, "${date.dayOfMonth}/${date.monthNumber}")
                }
            }
            ReportFilter.MONTHLY -> {
                // Last 30 days, group by 3 days for display if needed, but let's do all and filter in UI if too many
                val now = Clock.System.now().toLocalDateTime(timeZone).date
                (29 downTo 0 step 3).map { i ->
                    val date = now.minus(i, DateTimeUnit.DAY)
                    val total = transactions.filter {
                        it.createdAt.toLocalDateTime(timeZone).date == date
                    }.sumOf { it.total }.toFloat()
                    ChartData(total, "${date.dayOfMonth}/${date.monthNumber}")
                }
            }
        }
    }

    fun onFilterSelected(filter: ReportFilter) {
        _filter.value = filter
    }
}
