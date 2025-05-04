package com.example.personalfinancetracker

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.personalfinancetracker.databinding.ActivityCategoryAnalysisBinding
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.coroutines.launch
import java.util.Calendar

class CategoryAnalysisActivity : AppCompatActivity() {

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var binding: ActivityCategoryAnalysisBinding
    private lateinit var repository: FinanceRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferencesManager = PreferencesManager(this)
        val database = AppDatabase.getDatabase(this)
        repository = FinanceRepository(database)

        // Set up back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Category Analysis"

        // Setup pie chart
        setupPieChart()

        // Update data
        updateCategoryAnalysis()

        setupClickListeners()
    }

    private fun setupPieChart() {
        binding.pieChart.apply {
            description.isEnabled = false
            setUsePercentValues(false)
            setExtraOffsets(20f, 20f, 20f, 20f)
            dragDecelerationFrictionCoef = 0.95f
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            setTransparentCircleColor(Color.WHITE)
            setTransparentCircleAlpha(110)
            holeRadius = 60f
            transparentCircleRadius = 65f
            setDrawCenterText(true)
            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            animateY(1400, Easing.EaseInOutQuad)
            legend.isEnabled = false
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(16f)
        }
    }

    private fun setupClickListeners() {
        binding.categoryBtn.setOnClickListener{
            startActivity(Intent(this, CategoryAnalysisActivity::class.java))
            finish()
        }
        binding.transactionBtn.setOnClickListener{
            startActivity(Intent(this, TransactionActivity::class.java))
            finish()
        }
        binding.homeBtn.setOnClickListener{
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        binding.userBtn.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
    }

    private fun updateCategoryAnalysis() {
        lifecycleScope.launch {
            // Get current month and year
            val calendar = Calendar.getInstance()
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentYear = calendar.get(Calendar.YEAR)

            // Get start and end of current month
            val startOfMonth = Calendar.getInstance().apply {
                set(Calendar.YEAR, currentYear)
                set(Calendar.MONTH, currentMonth)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val endOfMonth = Calendar.getInstance().apply {
                set(Calendar.YEAR, currentYear)
                set(Calendar.MONTH, currentMonth)
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }

            // Get transactions from Room database
            repository.getTransactionsByDateRange(
                startOfMonth.timeInMillis,
                endOfMonth.timeInMillis
            ).collect { transactions ->
                // Get categories from Room database
                repository.allCategories.collect { categories ->
                    // Filter expenses only
                    val currentMonthExpenses = transactions.filter { it.isExpense }

                    // Group expenses by category
                    val expensesByCategory = currentMonthExpenses.groupBy { it.category }

                    // Calculate total expenses
                    val totalExpenses = currentMonthExpenses.sumOf { it.amount }

                    // Update total expenses text
                    val currency = preferencesManager.getCurrency()
                    binding.tvTotalExpenses.text = "Total Expenses: $currency ${totalExpenses.format(2)}"

                    // Create category summary items
                    val categorySummaries = mutableListOf<CategorySummary>()
                    val pieEntries = ArrayList<PieEntry>()
                    val categoryColors = ArrayList<Int>()

                    for (category in categories) {
                        val expenses = expensesByCategory[category.name]?.sumOf { it.amount } ?: 0.0
                        val percentage = if (totalExpenses > 0) (expenses / totalExpenses * 100) else 0.0

                        // Only add categories with expenses to the pie chart
                        if (expenses > 0) {
                            categorySummaries.add(
                                CategorySummary(
                                    category = category,
                                    amount = expenses,
                                    percentage = percentage
                                )
                            )

                            // Add entry to pie chart data
                            pieEntries.add(PieEntry(expenses.toFloat(), category.name))
                            categoryColors.add(category.color)
                        }
                    }

                    // Sort by amount (highest first)
                    categorySummaries.sortByDescending { it.amount }

                    // Update RecyclerView
                    val adapter = CategorySummaryAdapter(categorySummaries)
                    binding.rvCategories.adapter = adapter
                    binding.rvCategories.layoutManager = LinearLayoutManager(this@CategoryAnalysisActivity)

                    // Update pie chart
                    updatePieChartData(pieEntries, categoryColors, totalExpenses)
                }
            }
        }
    }

    private fun updatePieChartData(entries: ArrayList<PieEntry>, colors: ArrayList<Int>, totalExpenses: Double) {
        // If no data, show empty message
        if (entries.isEmpty()) {
            binding.pieChart.setNoDataText("No expenses for this month")
            binding.pieChart.setNoDataTextColor(Color.GRAY)
            binding.pieChart.invalidate()
            return
        }

        // Create dataset
        val dataSet = PieDataSet(entries, "")

        // Use category colors if available, otherwise use default colors
        if (colors.isNotEmpty()) {
            dataSet.colors = colors
        } else {
            dataSet.colors = listOf(
                Color.parseColor("#FF6B6B"), // Coral Red
                Color.parseColor("#4ECDC4"), // Turquoise
                Color.parseColor("#45B7D1"), // Sky Blue
                Color.parseColor("#96CEB4"), // Sage Green
                Color.parseColor("#FFEEAD"), // Light Yellow
                Color.parseColor("#D4A5A5"), // Dusty Rose
                Color.parseColor("#9B59B6"), // Purple
                Color.parseColor("#3498DB"), // Blue
                Color.parseColor("#E67E22"), // Orange
                Color.parseColor("#2ECC71")  // Green
            )
        }

        dataSet.apply {
            sliceSpace = 3f
            selectionShift = 5f
            valueTextSize = 14f
            valueTextColor = Color.BLACK
            setDrawValues(false)
            setValueLinePart1OffsetPercentage(80f)
            setValueLinePart1Length(0.5f)
            setValueLinePart2Length(0.4f)
            setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE)
        }

        // Create pie data
        val data = PieData(dataSet)

        // Set data to chart
        binding.pieChart.data = data

        // Set center text
        val currency = preferencesManager.getCurrency()
        binding.pieChart.centerText = "Total\n$currency ${totalExpenses.format(2)}"
        binding.pieChart.setCenterTextSize(18f)
        binding.pieChart.setCenterTextColor(Color.parseColor("#333333"))

        // Refresh chart
        binding.pieChart.invalidate()
    }

    private fun Double.format(digits: Int) = "%.${digits}f".format(this)

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    // Data class for category summary
    data class CategorySummary(
        val category: Category,
        val amount: Double,
        val percentage: Double
    )

    // Adapter for category summary
    inner class CategorySummaryAdapter(
        private val categorySummaries: List<CategorySummary>
    ) : RecyclerView.Adapter<CategorySummaryAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_category_summary, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(categorySummaries[position])
        }

        override fun getItemCount() = categorySummaries.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvCategoryName: TextView = itemView.findViewById(R.id.tvCategoryName)
            private val tvCategoryAmount: TextView = itemView.findViewById(R.id.tvCategoryAmount)
            private val tvCategoryPercentage: TextView = itemView.findViewById(R.id.tvCategoryPercentage)
            private val progressCategory: ProgressBar = itemView.findViewById(R.id.progressCategory)
            private val viewCategoryColor: View = itemView.findViewById(R.id.viewCategoryColor)

            fun bind(categorySummary: CategorySummary) {
                tvCategoryName.text = categorySummary.category.name

                val currency = preferencesManager.getCurrency()
                tvCategoryAmount.text = "$currency ${categorySummary.amount.format(2)}"

                tvCategoryPercentage.text = "${categorySummary.percentage.format(1)}%"

                progressCategory.progress = categorySummary.percentage.toInt()

                viewCategoryColor.setBackgroundColor(categorySummary.category.color)
            }

            private fun Double.format(digits: Int) = "%.${digits}f".format(this)
        }
    }
}