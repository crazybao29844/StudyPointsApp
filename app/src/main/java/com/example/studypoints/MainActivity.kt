package com.example.studypoints

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** 勾选项（名称 + 分值） */
data class CheckItem(val id: String, val text: String, val score: Double)

/** 积分类别描述 */
data class Cat(
    val key: String,
    val name: String,
    val color: String,
    val dashboard: Boolean,
    val bottom: Boolean,
    val calendar: Boolean,
    val total: Boolean,
    val hours: Boolean,
    val checkbox: Boolean,
    val items: List<CheckItem>
)

class MainActivity : Activity() {

    private val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val prefsName = "studypoints"
    private val REQ_EXPORT = 1001
    private val REQ_IMPORT = 1002

    private var data: JSONObject = JSONObject()
    private var curDate: Calendar = Calendar.getInstance()
    private var viewYear: Int = 0
    private var viewMonth0: Int = 0
    private var updating: Boolean = false

    private lateinit var weekHeader: LinearLayout
    private lateinit var calendarContainer: LinearLayout
    private lateinit var tvMonth: TextView
    private lateinit var tvPointsTitle: TextView
    private lateinit var etStudy: EditText
    private lateinit var etWork: EditText
    private lateinit var cbSleep: CheckBox
    private lateinit var cbDiet: CheckBox
    private lateinit var cbExercise: CheckBox
    private lateinit var tvStudyScore: TextView
    private lateinit var tvWorkScore: TextView
    private lateinit var tvLifeScore: TextView
    private lateinit var tvDayExpense: TextView
    private lateinit var tvAvg7: TextView
    private lateinit var tvTotal: TextView
    private lateinit var tvExpenseStat: TextView

    private lateinit var pagePoints: ScrollView
    private lateinit var pageQuadrant: FrameLayout
    private lateinit var quadrantList: LinearLayout
    private lateinit var tabPoints: TextView
    private lateinit var tabQuadrant: TextView
    private lateinit var tvPlanToday: TextView
    private lateinit var tvPlanTomorrow: TextView
    private lateinit var tvPlanTodayHeader: TextView
    private lateinit var tvPlanTomorrowHeader: TextView
    private lateinit var reviewContainer: LinearLayout
    private lateinit var reviewEditorList: LinearLayout
    private lateinit var etReviewItem: EditText

    private lateinit var dayTaskPanel: LinearLayout
    private lateinit var dayTaskList: LinearLayout
    private lateinit var tvDayTaskTitle: TextView

    private lateinit var customInputContainer: LinearLayout
    private lateinit var customScoreContainer: LinearLayout
    private lateinit var pointsWatcher: TextWatcher

    private var dashYear: Int = 0
    private var dashMonth0: Int = 0
    private var dashWeekStart: Calendar = Calendar.getInstance()

    private var catsCache: List<Cat>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        data = loadData()
        migrateTasks()
        val now = Calendar.getInstance()
        curDate = now.clone() as Calendar
        viewYear = now.get(Calendar.YEAR)
        viewMonth0 = now.get(Calendar.MONTH)

        weekHeader = findViewById(R.id.weekHeader)
        calendarContainer = findViewById(R.id.calendarContainer)
        dayTaskPanel = findViewById(R.id.dayTaskPanel)
        dayTaskList = findViewById(R.id.dayTaskList)
        tvDayTaskTitle = findViewById(R.id.tvDayTaskTitle)
        tvMonth = findViewById(R.id.tvMonth)
        tvPointsTitle = findViewById(R.id.tvPointsTitle)
        etStudy = findViewById(R.id.etStudy)
        etWork = findViewById(R.id.etWork)
        cbSleep = findViewById(R.id.cbSleep)
        cbDiet = findViewById(R.id.cbDiet)
        cbExercise = findViewById(R.id.cbExercise)
        tvStudyScore = findViewById(R.id.tvStudyScore)
        tvWorkScore = findViewById(R.id.tvWorkScore)
        tvLifeScore = findViewById(R.id.tvLifeScore)
        tvDayExpense = findViewById(R.id.tvDayExpense)
        tvAvg7 = findViewById(R.id.tvAvg7)
        tvTotal = findViewById(R.id.tvTotal)
        tvExpenseStat = findViewById(R.id.tvExpenseStat)
        pagePoints = findViewById(R.id.pagePoints)
        pageQuadrant = findViewById(R.id.pageQuadrant)
        quadrantList = findViewById(R.id.quadrantList)
        tabPoints = findViewById(R.id.tabPoints)
        tabQuadrant = findViewById(R.id.tabQuadrant)
        tvPlanToday = findViewById(R.id.tvPlanToday)
        tvPlanTomorrow = findViewById(R.id.tvPlanTomorrow)
        tvPlanTodayHeader = findViewById(R.id.tvPlanTodayHeader)
        tvPlanTomorrowHeader = findViewById(R.id.tvPlanTomorrowHeader)
        reviewContainer = findViewById(R.id.reviewContainer)
        reviewEditorList = findViewById(R.id.reviewEditorList)
        etReviewItem = findViewById(R.id.etReviewItem)
        customInputContainer = findViewById(R.id.customInputContainer)
        customScoreContainer = findViewById(R.id.customScoreContainer)

        pointsWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                onPointsChanged()
            }
        }
        etStudy.addTextChangedListener(pointsWatcher)
        etWork.addTextChangedListener(pointsWatcher)
        cbSleep.setOnCheckedChangeListener { _, _ -> onPointsChanged() }
        cbDiet.setOnCheckedChangeListener { _, _ -> onPointsChanged() }
        cbExercise.setOnCheckedChangeListener { _, _ -> onPointsChanged() }

        findViewById<Button>(R.id.btnPrevMonth).setOnClickListener { changeMonth(-1) }
        findViewById<Button>(R.id.btnNextMonth).setOnClickListener { changeMonth(1) }
        findViewById<Button>(R.id.btnToday).setOnClickListener { goToday() }
        findViewById<Button>(R.id.btnLedger).setOnClickListener { openLedger() }
        findViewById<Button>(R.id.btnDashboard).setOnClickListener { openDashboard() }
        findViewById<Button>(R.id.btnBackup).setOnClickListener { openBackupDialog() }
        findViewById<Button>(R.id.btnAddCat).setOnClickListener { openCategoryMenu() }
        findViewById<Button>(R.id.btnAddReview).setOnClickListener { addReviewItem() }

        tabPoints.setOnClickListener { switchTab(0) }
        tabQuadrant.setOnClickListener { switchTab(1) }
        findViewById<Button>(R.id.btnMovePlan).setOnClickListener { moveTomorrowToToday() }
        findViewById<TextView>(R.id.fabAdd).setOnClickListener { openAddTaskDialog() }
        tvPlanTodayHeader.setOnClickListener { openEditPlan("today", "编辑今日规划") }
        tvPlanTomorrowHeader.setOnClickListener { openEditPlan("tomorrow", "编辑明日规划") }

        rebuildCategoryInputs()
        switchTab(0)
        loadDay()
    }

    // ---------------- 数据 ----------------
    private fun loadData(): JSONObject {
        val s = getSharedPreferences(prefsName, Context.MODE_PRIVATE).getString("data", null)
        if (s != null) {
            try {
                return JSONObject(s)
            } catch (e: Exception) {
                // 损坏则重建
            }
        }
        val d = JSONObject()
        d.put("records", JSONObject())
        return d
    }

    private fun saveData() {
        getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit().putString("data", data.toString()).apply()
    }

    private fun records(): JSONObject {
        if (!data.has("records")) data.put("records", JSONObject())
        return data.getJSONObject("records")
    }

    private fun getOrCreateRecord(key: String): JSONObject {
        val recs = records()
        var rec = recs.optJSONObject(key)
        if (rec == null) {
            rec = JSONObject()
            recs.put(key, rec)
        }
        return rec
    }

    private fun getRecordOrNull(key: String): JSONObject? = records().optJSONObject(key)

    private fun dayPoints(rec: JSONObject?): Triple<Double, Double, Int> {
        if (rec == null) return Triple(0.0, 0.0, 0)
        val study = rec.optDouble("study_hours", 0.0)
        val work = rec.optDouble("work_hours", 0.0)
        var life = 0
        if (rec.optBoolean("sleep_ok", false)) life++
        if (rec.optBoolean("diet_ok", false)) life++
        if (rec.optBoolean("exercise_ok", false)) life++
        return Triple(study, work, life)
    }

    private fun dayExpense(rec: JSONObject?): Double {
        if (rec == null) return 0.0
        val arr = rec.optJSONArray("expenses") ?: return 0.0
        var sum = 0.0
        for (i in 0 until arr.length()) {
            sum += arr.getJSONObject(i).optDouble("price", 0.0)
        }
        return sum
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun fmtMoney(v: Double): String {
        return if (v == Math.floor(v) && !v.isInfinite()) {
            v.toLong().toString()
        } else {
            String.format(Locale.getDefault(), "%.2f", v).trimEnd('0').trimEnd('.')
        }
    }

    private fun fmtNum(v: Double): String {
        return if (v == Math.floor(v) && !v.isInfinite()) v.toLong().toString()
        else String.format(Locale.getDefault(), "%.1f", v)
    }

    private fun keyOf(c: Calendar): String = df.format(c.time)

    // ---------------- 积分类别 ----------------
    private fun customCatsArray(): JSONArray {
        if (!data.has("customCats")) data.put("customCats", JSONArray())
        return data.getJSONArray("customCats")
    }

    private fun invalidateCats() {
        catsCache = null
    }

    private fun allCats(): List<Cat> {
        catsCache?.let { return it }
        val list = ArrayList<Cat>()
        list.add(Cat("study", "学习", "#2E9E6B", true, true, true, true, true, false, emptyList()))
        list.add(Cat("work", "工作", "#2563EB", true, true, true, false, true, false, emptyList()))
        list.add(Cat("life", "生活", "#C2410C", true, true, true, true, false, false, emptyList()))
        val palette = arrayOf("#7C3AED", "#DB2777", "#0891B2", "#65A30D", "#D97706", "#4F46E5")
        val arr = customCatsArray()
        for (i in 0 until arr.length()) {
            val c = arr.getJSONObject(i)
            val items = ArrayList<CheckItem>()
            val itemsArr = c.optJSONArray("items") ?: JSONArray()
            for (j in 0 until itemsArr.length()) {
                val it2 = itemsArr.getJSONObject(j)
                items.add(CheckItem(it2.optString("id"), it2.optString("text"), it2.optDouble("score", 0.0)))
            }
            list.add(
                Cat(
                    c.optString("key"),
                    c.optString("name"),
                    palette[i % palette.size],
                    c.optBoolean("dashboard", true),
                    c.optBoolean("bottom", true),
                    c.optBoolean("calendar", true),
                    true,
                    true,
                    c.optBoolean("checkbox", false),
                    items
                )
            )
        }
        catsCache = list
        return list
    }

    private fun catValue(rec: JSONObject?, key: String): Double {
        if (rec == null) return 0.0
        return when (key) {
            "study" -> rec.optDouble("study_hours", 0.0)
            "work" -> rec.optDouble("work_hours", 0.0)
            "life" -> ((if (rec.optBoolean("sleep_ok", false)) 1 else 0) +
                    (if (rec.optBoolean("diet_ok", false)) 1 else 0) +
                    (if (rec.optBoolean("exercise_ok", false)) 1 else 0)).toDouble()
            else -> {
                var v = rec.optJSONObject("custom")?.optDouble(key, 0.0) ?: 0.0
                val cat = catByKey(key)
                if (cat != null && cat.checkbox && cat.items.isNotEmpty()) {
                    val checked = rec.optJSONObject("checks")?.optJSONArray(key)
                    if (checked != null) {
                        val ids = HashSet<String>()
                        for (j in 0 until checked.length()) ids.add(checked.optString(j))
                        for (item in cat.items) {
                            if (ids.contains(item.id)) v += item.score
                        }
                    }
                }
                v
            }
        }
    }

    private fun catByKey(key: String): Cat? = allCats().firstOrNull { it.key == key }

    private fun rebuildCategoryInputs() {
        customInputContainer.removeAllViews()
        val arr = customCatsArray()
        for (i in 0 until arr.length()) {
            val c = arr.getJSONObject(i)
            val key = c.optString("key")
            val name = c.optString("name")
            val checkboxMode = c.optBoolean("checkbox", false)
            val itemsArr = c.optJSONArray("items") ?: JSONArray()

            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL

            if (checkboxMode) {
                val btnPlus = Button(this)
                btnPlus.text = "＋"
                btnPlus.textSize = 12f
                btnPlus.backgroundTintList =
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#7C3AED"))
                btnPlus.setTextColor(Color.WHITE)
                btnPlus.layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
                btnPlus.setPadding(0, 0, 0, 0)
                btnPlus.setOnClickListener { openAddCheckItem(key) }
                row.addView(btnPlus)
            }

            val label = TextView(this)
            label.text = name
            label.setTextColor(Color.parseColor("#2B2B2B"))
            label.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            label.setPadding(dp(6), 0, 0, 0)
            row.addView(label)

            val et = EditText(this)
            et.tag = key
            et.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            et.setText("0")
            et.setTextColor(Color.parseColor("#2B2B2B"))
            et.layoutParams = LinearLayout.LayoutParams(dp(80), LinearLayout.LayoutParams.WRAP_CONTENT)
            et.addTextChangedListener(pointsWatcher)
            row.addView(et)
            customInputContainer.addView(row)

            if (checkboxMode) {
                for (j in 0 until itemsArr.length()) {
                    val itemObj = itemsArr.getJSONObject(j)
                    val itemId = itemObj.optString("id")
                    val cb = CheckBox(this)
                    cb.text = itemObj.optString("text") + "  +" + fmtNum(itemObj.optDouble("score", 0.0))
                    cb.textSize = 14f
                    cb.setPadding(dp(20), 0, 0, 0)
                    cb.tag = key + "|" + itemId
                    cb.setOnCheckedChangeListener { _, checked ->
                        if (updating) return@setOnCheckedChangeListener
                        setChecked(key, itemId, checked)
                    }
                    cb.setOnLongClickListener {
                        AlertDialog.Builder(this)
                            .setTitle(itemObj.optString("text"))
                            .setItems(arrayOf("修改名称 / 分值", "删除该勾选项")) { _, which ->
                                if (which == 0) {
                                    openEditCheckItem(key, itemId)
                                } else {
                                    deleteCheckItem(key, itemId)
                                }
                            }
                            .setNegativeButton("取消", null)
                            .show()
                        true
                    }
                    customInputContainer.addView(cb)
                }
            }
        }
    }

    private fun openAddCategory() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_add_category)
        val etName = dialog.findViewById<EditText>(R.id.etCatName)
        val cbDash = dialog.findViewById<CheckBox>(R.id.cbCatDashboard)
        val cbBottom = dialog.findViewById<CheckBox>(R.id.cbCatBottom)
        val cbCal = dialog.findViewById<CheckBox>(R.id.cbCatCalendar)
        val cbCheckbox = dialog.findViewById<CheckBox>(R.id.cbCatCheckbox)
        cbDash.isChecked = true
        cbBottom.isChecked = true
        cbCal.isChecked = true

        dialog.findViewById<Button>(R.id.btnCatCancel).setOnClickListener { dialog.dismiss() }
        dialog.findViewById<Button>(R.id.btnCatConfirm).setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "请填写积分名称", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val arr = customCatsArray()
            val obj = JSONObject()
            obj.put("key", "c" + System.currentTimeMillis())
            obj.put("name", name)
            obj.put("dashboard", cbDash.isChecked)
            obj.put("bottom", cbBottom.isChecked)
            obj.put("calendar", cbCal.isChecked)
            obj.put("checkbox", cbCheckbox.isChecked)
            obj.put("items", JSONArray())
            arr.put(obj)
            saveData()
            invalidateCats()
            dialog.dismiss()
            rebuildCategoryInputs()
            loadDay()
        }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    private fun isChecked(catKey: String, itemId: String): Boolean {
        val rec = getRecordOrNull(keyOf(curDate)) ?: return false
        val arr = rec.optJSONObject("checks")?.optJSONArray(catKey) ?: return false
        for (i in 0 until arr.length()) {
            if (arr.optString(i) == itemId) return true
        }
        return false
    }

    private fun setChecked(catKey: String, itemId: String, checked: Boolean) {
        val rec = getOrCreateRecord(keyOf(curDate))
        var checks = rec.optJSONObject("checks")
        if (checks == null) {
            checks = JSONObject()
            rec.put("checks", checks)
        }
        val arr = checks.optJSONArray(catKey) ?: JSONArray()
        val newArr = JSONArray()
        for (i in 0 until arr.length()) {
            val id = arr.optString(i)
            if (id != itemId) newArr.put(id)
        }
        if (checked) newArr.put(itemId)
        checks.put(catKey, newArr)
        saveData()
        updateScores()
        updateStats()
        renderCalendar()
    }

    private fun openAddCheckItem(catKey: String) {
        showCheckItemDialog(catKey, null)
    }

    private fun openEditCheckItem(catKey: String, itemId: String) {
        showCheckItemDialog(catKey, itemId)
    }

    private fun deleteCheckItem(catKey: String, itemId: String) {
        AlertDialog.Builder(this)
            .setMessage("删除这个勾选项？")
            .setPositiveButton("删除") { _, _ ->
                val arr = customCatsArray()
                for (i in 0 until arr.length()) {
                    val c = arr.getJSONObject(i)
                    if (c.optString("key") != catKey) continue
                    val items = c.optJSONArray("items")
                    if (items == null) continue
                    val newArr = JSONArray()
                    for (j in 0 until items.length()) {
                        val it2 = items.getJSONObject(j)
                        if (it2.optString("id") != itemId) newArr.put(it2)
                    }
                    c.put("items", newArr)
                    break
                }
                saveData()
                invalidateCats()
                rebuildCategoryInputs()
                loadDay()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showCheckItemDialog(catKey: String, itemId: String?) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_add_check_item)
        val tvTitle = dialog.findViewById<TextView>(R.id.tvCheckTitle)
        val etName = dialog.findViewById<EditText>(R.id.etCheckName)
        val etScore = dialog.findViewById<EditText>(R.id.etCheckScore)
        val btnOk = dialog.findViewById<Button>(R.id.btnCheckOk)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCheckCancel)

        if (itemId == null) {
            tvTitle.text = "添加勾选项"
        } else {
            tvTitle.text = "设置勾选项"
            val cat = catByKey(catKey)
            val item = cat?.items?.firstOrNull { it.id == itemId }
            if (item != null) {
                etName.setText(item.text)
                etScore.setText(fmtNum(item.score))
            }
        }

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnOk.setOnClickListener {
            val text = etName.text.toString().trim()
            if (text.isEmpty()) {
                Toast.makeText(this, "请填写名称", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val score = etScore.text.toString().trim().toDoubleOrNull()
            if (score == null) {
                Toast.makeText(this, "分数请填数字", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val arr = customCatsArray()
            var found = false
            for (i in 0 until arr.length()) {
                val c = arr.getJSONObject(i)
                if (c.optString("key") != catKey) continue
                var items = c.optJSONArray("items")
                if (items == null) {
                    items = JSONArray()
                    c.put("items", items)
                }
                if (itemId == null) {
                    val obj = JSONObject()
                    obj.put("id", System.currentTimeMillis().toString())
                    obj.put("text", text)
                    obj.put("score", score)
                    items.put(obj)
                } else {
                    for (j in 0 until items.length()) {
                        val it2 = items.getJSONObject(j)
                        if (it2.optString("id") == itemId) {
                            it2.put("text", text)
                            it2.put("score", score)
                        }
                    }
                }
                found = true
                break
            }
            if (found) {
                saveData()
                invalidateCats()
                dialog.dismiss()
                rebuildCategoryInputs()
                loadDay()
            }
        }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    private fun openCategoryMenu() {
        AlertDialog.Builder(this)
            .setTitle("积分")
            .setItems(arrayOf<CharSequence>("添加积分", "删除积分")) { _, which ->
                if (which == 0) openAddCategory() else openDeleteCategory()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun openDeleteCategory() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_delete_category)
        val list = dialog.findViewById<LinearLayout>(R.id.deleteCatList)
        val tvEmpty = dialog.findViewById<TextView>(R.id.tvDeleteEmpty)

        fun render() {
            list.removeAllViews()
            val arr = customCatsArray()
            tvEmpty.visibility = if (arr.length() == 0) View.VISIBLE else View.GONE
            for (i in 0 until arr.length()) {
                val c = arr.getJSONObject(i)
                val name = c.optString("name")
                val key = c.optString("key")
                val row = TextView(this)
                row.text = name
                row.textSize = 15f
                row.setTextColor(Color.parseColor("#2B2B2B"))
                row.setPadding(dp(10), dp(12), dp(10), dp(12))
                row.setOnLongClickListener {
                    confirmDeleteCategory(key, name) { render() }
                    true
                }
                list.addView(row)
            }
        }

        dialog.findViewById<Button>(R.id.btnCloseDeleteCat).setOnClickListener { dialog.dismiss() }
        render()
        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    private fun confirmDeleteCategory(key: String, name: String, onDeleted: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("删除积分")
            .setMessage("确定删除「" + name + "」吗？")
            .setPositiveButton("删除") { _, _ ->
                val arr = customCatsArray()
                val newArr = JSONArray()
                for (i in 0 until arr.length()) {
                    val c = arr.getJSONObject(i)
                    if (c.optString("key") != key) newArr.put(c)
                }
                data.put("customCats", newArr)
                saveData()
                invalidateCats()
                rebuildCategoryInputs()
                loadDay()
                onDeleted()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // ---------------- 日历 ----------------
    private fun renderCalendar() {
        tvMonth.text = "${viewYear} 年 ${viewMonth0 + 1} 月"

        weekHeader.removeAllViews()
        val wdNames = arrayOf("一", "二", "三", "四", "五", "六", "日")
        for (w in wdNames) {
            val tv = TextView(this)
            tv.text = w
            tv.gravity = Gravity.CENTER
            tv.setTextColor(Color.parseColor("#666666"))
            tv.textSize = 12f
            tv.typeface = Typeface.DEFAULT_BOLD
            tv.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            weekHeader.addView(tv)
        }

        val cal = Calendar.getInstance()
        cal.set(viewYear, viewMonth0, 1)
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        val mondayBased = (dow + 5) % 7
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val todayKey = keyOf(Calendar.getInstance())
        val selKey = keyOf(curDate)
        val cats = allCats()

        calendarContainer.removeAllViews()

        var day = 1
        var row: LinearLayout? = null
        for (i in 0 until 42) {
            if (i % 7 == 0) {
                row = LinearLayout(this)
                row.orientation = LinearLayout.HORIZONTAL
                row.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(60)
                )
                calendarContainer.addView(row)
            }

            val cell = LinearLayout(this)
            cell.orientation = LinearLayout.VERTICAL
            cell.gravity = Gravity.CENTER_HORIZONTAL
            cell.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)

            if (i >= mondayBased && day <= daysInMonth) {
                val key = String.format(Locale.getDefault(), "%04d-%02d-%02d", viewYear, viewMonth0 + 1, day)
                val rec = getRecordOrNull(key)
                val isToday = key == todayKey
                val isSel = key == selKey

                if (isSel) cell.setBackgroundColor(Color.parseColor("#2E9E6B"))
                else if (isToday) cell.setBackgroundColor(Color.parseColor("#D9F0E8"))
                else cell.setBackgroundColor(Color.WHITE)

                val numColor = if (isSel) Color.WHITE else Color.parseColor("#2B2B2B")
                val num = TextView(this)
                num.text = day.toString()
                num.setTextColor(numColor)
                num.textSize = 13f
                num.typeface = Typeface.DEFAULT_BOLD
                num.gravity = Gravity.CENTER
                cell.addView(num)

                val exp = dayExpense(rec)
                val vals = ArrayList<Double>()
                var any = false
                for (c in cats) {
                    if (!c.calendar) continue
                    val v = catValue(rec, c.key)
                    vals.add(v)
                    if (v > 0.0) any = true
                }
                val ptxt = if (any) vals.joinToString("/") { fmtNum(it) } else ""
                val etxt = if (exp > 0.0) "￥" + fmtMoney(exp) else ""

                val pv = TextView(this)
                pv.text = ptxt
                pv.textSize = 8f
                pv.gravity = Gravity.CENTER
                pv.setTextColor(if (isSel) Color.WHITE else Color.parseColor("#2E9E6B"))
                cell.addView(pv)

                val ev = TextView(this)
                ev.text = etxt
                ev.textSize = 8f
                ev.gravity = Gravity.CENTER
                ev.setTextColor(if (isSel) Color.parseColor("#FFE0B2") else Color.parseColor("#C2410C"))
                cell.addView(ev)

                val dayNum = day
                cell.setOnClickListener {
                    val c2 = Calendar.getInstance()
                    c2.set(viewYear, viewMonth0, dayNum)
                    curDate = c2
                    loadDay()
                }
                day++
            }

            row?.addView(cell)
        }
    }

    private fun changeMonth(delta: Int) {
        viewMonth0 += delta
        if (viewMonth0 < 0) {
            viewMonth0 = 11
            viewYear--
        } else if (viewMonth0 > 11) {
            viewMonth0 = 0
            viewYear++
        }
        renderCalendar()
    }

    private fun goToday() {
        val now = Calendar.getInstance()
        curDate = now.clone() as Calendar
        viewYear = now.get(Calendar.YEAR)
        viewMonth0 = now.get(Calendar.MONTH)
        loadDay()
    }

    // ---------------- 积分 ----------------
    private fun loadDay() {
        updating = true
        val key = keyOf(curDate)
        val rec = getRecordOrNull(key)
        val study = rec?.optDouble("study_hours", 0.0) ?: 0.0
        val work = rec?.optDouble("work_hours", 0.0) ?: 0.0
        etStudy.setText(fmtNum(study))
        etWork.setText(fmtNum(work))
        // 自定义积分输入 + 勾选项状态
        val custom = rec?.optJSONObject("custom")
        for (i in 0 until customInputContainer.childCount) {
            val child = customInputContainer.getChildAt(i)
            if (child is LinearLayout) {
                var et: EditText? = null
                for (j in 0 until child.childCount) {
                    val v = child.getChildAt(j)
                    if (v is EditText) {
                        et = v
                        break
                    }
                }
                if (et != null) {
                    val k = et.tag as String
                    et.setText(fmtNum(custom?.optDouble(k, 0.0) ?: 0.0))
                }
            } else if (child is CheckBox) {
                val tag = child.tag as? String
                if (tag != null && tag.contains("|")) {
                    val parts = tag.split("|")
                    if (parts.size == 2) {
                        child.isChecked = isChecked(parts[0], parts[1])
                    }
                }
            }
        }
        cbSleep.isChecked = rec?.optBoolean("sleep_ok", false) ?: false
        cbDiet.isChecked = rec?.optBoolean("diet_ok", false) ?: false
        cbExercise.isChecked = rec?.optBoolean("exercise_ok", false) ?: false
        tvPointsTitle.text = "积分记录（" + df.format(curDate.time) + "）"
        updating = false
        renderCalendar()
        updateScores()
        updateStats()
        renderReviewEditor()
        renderDayTasks()
    }

    private fun onPointsChanged() {
        if (updating) return
        val hours = etStudy.text.toString().trim().toDoubleOrNull() ?: return
        val workHours = etWork.text.toString().trim().toDoubleOrNull() ?: 0.0
        val key = keyOf(curDate)
        val rec = getOrCreateRecord(key)
        rec.put("study_hours", hours.coerceIn(0.0, 24.0))
        rec.put("work_hours", workHours.coerceIn(0.0, 24.0))
        // 自定义积分
        val custom = rec.optJSONObject("custom") ?: JSONObject().also { rec.put("custom", it) }
        for (i in 0 until customInputContainer.childCount) {
            val child = customInputContainer.getChildAt(i)
            if (child is LinearLayout) {
                var et: EditText? = null
                for (j in 0 until child.childCount) {
                    val v = child.getChildAt(j)
                    if (v is EditText) {
                        et = v
                        break
                    }
                }
                if (et != null) {
                    val k = et.tag as String
                    val v = et.text.toString().trim().toDoubleOrNull() ?: 0.0
                    custom.put(k, v.coerceIn(0.0, 24.0))
                }
            }
        }
        rec.put("sleep_ok", cbSleep.isChecked)
        rec.put("diet_ok", cbDiet.isChecked)
        rec.put("exercise_ok", cbExercise.isChecked)
        saveData()
        updateScores()
        updateStats()
        renderCalendar()
    }

    private fun updateScores() {
        val rec = getRecordOrNull(keyOf(curDate))
        val p = dayPoints(rec)
        val exp = dayExpense(rec)
        tvStudyScore.text = "学习 " + fmtNum(p.first)
        tvWorkScore.text = "工作 " + fmtNum(p.second)
        tvLifeScore.text = "生活 ${p.third}/3"
        tvDayExpense.text = "开销 ￥" + fmtMoney(exp)

        customScoreContainer.removeAllViews()
        val arr = customCatsArray()
        for (i in 0 until arr.length()) {
            val c = arr.getJSONObject(i)
            val k = c.optString("key")
            val name = c.optString("name")
            val cat = catByKey(k)
            val tv = TextView(this)
            tv.text = name + " " + fmtNum(catValue(rec, k))
            tv.setTextColor(Color.parseColor(cat?.color ?: "#7C3AED"))
            tv.textSize = 15f
            tv.typeface = Typeface.DEFAULT_BOLD
            tv.setPadding(dp(2), dp(2), dp(2), dp(2))
            customScoreContainer.addView(tv)
        }
    }

    private fun updateStats() {
        val today = Calendar.getInstance()
        val cats = allCats()
        val keysAll = allKeys()

        val avgParts = ArrayList<String>()
        val cumParts = ArrayList<String>()
        for (c in cats) {
            if (!c.bottom) continue
            var s7 = 0.0
            for (i in 0 until 7) {
                val cal = (today.clone() as Calendar).also { it.add(Calendar.DAY_OF_MONTH, -i) }
                s7 += catValue(getRecordOrNull(keyOf(cal)), c.key)
            }
            avgParts.add(c.name + " " + String.format(Locale.getDefault(), "%.1f", s7 / 7.0))

            if (c.total) {
                var sAll = 0.0
                for (k in keysAll) sAll += catValue(getRecordOrNull(k), c.key)
                val n = keysAll.size
                val avg = if (n > 0) sAll / n else 0.0
                cumParts.add(String.format(Locale.getDefault(), "%s共 %.1f（日均 %.1f）", c.name, sAll, avg))
            }
        }

        tvAvg7.text = "近 7 天日均　" + if (avgParts.isEmpty()) "（无）" else avgParts.joinToString(" ｜ ")
        tvTotal.text = if (keysAll.isEmpty()) "记录以来 0 天"
        else "记录以来 ${keysAll.size} 天　" + cumParts.joinToString(" ｜ ")

        var e30 = 0.0
        for (i in 0 until 30) {
            val cal = (today.clone() as Calendar).also { it.add(Calendar.DAY_OF_MONTH, -i) }
            e30 += dayExpense(getRecordOrNull(keyOf(cal)))
        }
        var eAll = 0.0
        for (k in keysAll) eAll += dayExpense(getRecordOrNull(k))

        tvExpenseStat.text = String.format(Locale.getDefault(),
            "近 30 天开销　日均 ￥%.1f　｜　合计 ￥%.1f（累计全部 ￥%.1f）",
            e30 / 30.0, e30, eAll)
    }

    // ---------------- 页面切换 ----------------
    private fun switchTab(which: Int) {
        pagePoints.visibility = if (which == 0) View.VISIBLE else View.GONE
        pageQuadrant.visibility = if (which == 1) View.VISIBLE else View.GONE
        val on = Color.parseColor("#2E9E6B")
        val off = Color.parseColor("#999999")
        tabPoints.setTextColor(if (which == 0) on else off)
        tabQuadrant.setTextColor(if (which == 1) on else off)
        when (which) {
            0 -> renderDayTasks()
            1 -> renderQuadrant()
        }
    }

    // ---------------- 记账 ----------------
    private fun openLedger() {
        val key = keyOf(curDate)
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_ledger)

        val tvLedgerTitle = dialog.findViewById<TextView>(R.id.tvLedgerTitle)
        val ledgerList = dialog.findViewById<LinearLayout>(R.id.ledgerList)
        val tvLedgerTotal = dialog.findViewById<TextView>(R.id.tvLedgerTotal)
        val etName = dialog.findViewById<EditText>(R.id.etItemName)
        val etPrice = dialog.findViewById<EditText>(R.id.etItemPrice)
        val btnAdd = dialog.findViewById<Button>(R.id.btnAddItem)
        val btnClear = dialog.findViewById<Button>(R.id.btnClearItem)
        val btnClose = dialog.findViewById<Button>(R.id.btnCloseLedger)

        tvLedgerTitle.text = "记账  |  " + df.format(curDate.time)

        var selectedIndex = -1

        fun renderList() {
            ledgerList.removeAllViews()
            selectedIndex = -1
            val rec = getRecordOrNull(key)
            val arr = rec?.optJSONArray("expenses") ?: JSONArray()
            var total = 0.0
            for (i in 0 until arr.length()) {
                val it2 = arr.getJSONObject(i)
                val row = TextView(this)
                row.text = it2.optString("name") + "    ￥" + fmtMoney(it2.optDouble("price", 0.0))
                row.textSize = 15f
                row.setTextColor(Color.parseColor("#2B2B2B"))
                row.setPadding(dp(8), dp(8), dp(8), dp(8))
                val idx = i
                row.setOnClickListener {
                    selectedIndex = idx
                    for (j in 0 until ledgerList.childCount) {
                        ledgerList.getChildAt(j).setBackgroundColor(Color.TRANSPARENT)
                    }
                    row.setBackgroundColor(Color.parseColor("#FFF3E0"))
                }
                ledgerList.addView(row)
                total += it2.optDouble("price", 0.0)
            }
            tvLedgerTotal.text = "当日合计：￥" + fmtMoney(total)
        }

        fun addItem() {
            val name = etName.text.toString().trim()
            val priceS = etPrice.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "请填写项目名称", Toast.LENGTH_SHORT).show()
                return
            }
            val price = priceS.toDoubleOrNull()
            if (price == null || price < 0.0) {
                Toast.makeText(this, "价格请填非负数字，如 12.5", Toast.LENGTH_SHORT).show()
                return
            }
            val rec = getOrCreateRecord(key)
            var arr = rec.optJSONArray("expenses")
            if (arr == null) {
                arr = JSONArray()
                rec.put("expenses", arr)
            }
            val item = JSONObject()
            item.put("name", name)
            item.put("price", price)
            arr.put(item)
            saveData()
            etName.setText("")
            etPrice.setText("")
            renderList()
            updateScores()
            updateStats()
            renderCalendar()
        }

        fun clearItem() {
            if (selectedIndex < 0) {
                Toast.makeText(this, "请先选中要删除的项目", Toast.LENGTH_SHORT).show()
                return
            }
            val rec = getRecordOrNull(key) ?: return
            val arr = rec.optJSONArray("expenses") ?: return
            val newArr = JSONArray()
            for (i in 0 until arr.length()) {
                if (i != selectedIndex) newArr.put(arr.getJSONObject(i))
            }
            rec.put("expenses", newArr)
            saveData()
            renderList()
            updateScores()
            updateStats()
            renderCalendar()
        }

        btnAdd.setOnClickListener { addItem() }
        btnClear.setOnClickListener { clearItem() }
        btnClose.setOnClickListener { dialog.dismiss() }

        renderList()
        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    // ---------------- 四象限 ----------------
    private fun tasksArray(): JSONArray {
        if (!data.has("tasks")) data.put("tasks", JSONArray())
        return data.getJSONArray("tasks")
    }

    private fun renderQuadrant() {
        quadrantList.removeAllViews()
        val names = arrayOf("① 重要且紧急", "② 重要不紧急", "③ 紧急不重要", "④ 不重要不紧急")
        val colors = arrayOf("#D9534F", "#2E9E6B", "#3B82C4", "#8A8A8A")
        val arr = tasksArray()

        for (q in 1..4) {
            val hdr = TextView(this)
            hdr.text = names[q - 1]
            hdr.setTextColor(Color.WHITE)
            hdr.setBackgroundColor(Color.parseColor(colors[q - 1]))
            hdr.setPadding(dp(10), dp(6), dp(10), dp(6))
            hdr.textSize = 14f
            hdr.typeface = Typeface.DEFAULT_BOLD
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, dp(10), 0, 0)
            hdr.layoutParams = lp
            quadrantList.addView(hdr)

            val box = LinearLayout(this)
            box.orientation = LinearLayout.VERTICAL
            box.setBackgroundColor(Color.WHITE)
            box.setPadding(dp(8), dp(4), dp(8), dp(4))
            quadrantList.addView(box)

            var hasTask = false
            for (i in 0 until arr.length()) {
                val t = arr.getJSONObject(i)
                if (t.optInt("quadrant", 1) != q) continue
                hasTask = true
                val done = t.optBoolean("done", false)
                val row = TextView(this)
                row.text = taskDatePrefix(t) + (if (done) "✓  " else "·  ") + t.optString("name")
                row.textSize = 15f
                row.setPadding(dp(6), dp(10), dp(6), dp(10))
                if (done) {
                    row.setTextColor(Color.parseColor("#9A9A9A"))
                    row.paintFlags = row.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                } else {
                    row.setTextColor(Color.parseColor("#2B2B2B"))
                }
                val idx = i
                row.setOnClickListener {
                    val tt = arr.getJSONObject(idx)
                    tt.put("done", !tt.optBoolean("done", false))
                    saveData()
                    renderQuadrant()
                }
                row.setOnLongClickListener {
                    val tt = arr.getJSONObject(idx)
                    AlertDialog.Builder(this)
                        .setTitle(tt.optString("name"))
                        .setItems(arrayOf("更改重要程度", "链接到日期", "删除")) { _, which ->
                            when (which) {
                                0 -> changeTaskQuadrant(idx)
                                1 -> linkTaskDates(idx)
                                else -> confirmDeleteTask(idx)
                            }
                        }
                        .setNegativeButton("取消", null)
                        .show()
                    true
                }
                box.addView(row)
            }
            if (!hasTask) {
                val empty = TextView(this)
                empty.text = "（暂无）"
                empty.setTextColor(Color.parseColor("#BBBBBB"))
                empty.textSize = 13f
                empty.setPadding(dp(6), dp(8), dp(6), dp(8))
                box.addView(empty)
            }
        }
        renderPlans()
        renderReview()
    }

    private fun changeTaskQuadrant(idx: Int) {
        val arr = tasksArray()
        val t = arr.getJSONObject(idx)
        val names = arrayOf("① 重要且紧急", "② 重要不紧急", "③ 紧急不重要", "④ 不重要不紧急")
        val current = t.optInt("quadrant", 1)
        AlertDialog.Builder(this)
            .setTitle("更改重要程度")
            .setSingleChoiceItems(names, current - 1) { d, which ->
                t.put("quadrant", which + 1)
                saveData()
                d.dismiss()
                renderQuadrant()
                renderDayTasks()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun linkTaskDates(idx: Int) {
        val arr = tasksArray()
        val t = arr.getJSONObject(idx)
        val current = ArrayList<String>()
        val dates = t.optJSONArray("dates")
        if (dates != null) {
            for (i in 0 until dates.length()) current.add(dates.optString(i))
        }
        openLinkDatePicker(current) { picked ->
            val newArr = JSONArray()
            for (d in picked) newArr.put(d)
            t.put("dates", newArr)
            saveData()
            renderQuadrant()
            renderDayTasks()
        }
    }

    private fun confirmDeleteTask(idx: Int) {
        val arr = tasksArray()
        val t = arr.getJSONObject(idx)
        AlertDialog.Builder(this)
            .setMessage("删除「" + t.optString("name") + "」？")
            .setPositiveButton("删除") { _, _ ->
                val newArr = JSONArray()
                for (j in 0 until arr.length()) {
                    if (j != idx) newArr.put(arr.getJSONObject(j))
                }
                data.put("tasks", newArr)
                saveData()
                renderQuadrant()
                renderDayTasks()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // ---------------- 复习项目（每日编辑） ----------------
    private fun addReviewItem() {
        val text = etReviewItem.text.toString().trim()
        if (text.isEmpty()) return
        val rec = getOrCreateRecord(keyOf(curDate))
        var arr = rec.optJSONArray("review")
        if (arr == null) {
            arr = JSONArray()
            rec.put("review", arr)
        }
        val item = JSONObject()
        item.put("id", System.currentTimeMillis())
        item.put("text", text)
        item.put("done", false)
        arr.put(item)
        saveData()
        etReviewItem.setText("")
        renderReviewEditor()
        renderReview()
    }

    private fun showReviewEmpty() {
        val tv = TextView(this)
        tv.text = "（暂无，可在下面输入添加）"
        tv.setTextColor(Color.parseColor("#AAAAAA"))
        tv.textSize = 13f
        tv.setPadding(dp(6), dp(8), dp(6), dp(8))
        reviewEditorList.addView(tv)
    }

    private fun renderReviewEditor() {
        reviewEditorList.removeAllViews()
        val rec = getRecordOrNull(keyOf(curDate))
        if (rec == null) {
            showReviewEmpty()
            return
        }
        val arr = rec.optJSONArray("review") ?: JSONArray()
        if (arr.length() == 0) {
            showReviewEmpty()
            return
        }
        for (i in 0 until arr.length()) {
            val itemObj = arr.getJSONObject(i)
            val done = itemObj.optBoolean("done", false)
            val row = TextView(this)
            row.text = (if (done) "✓  " else "·  ") + itemObj.optString("text")
            row.textSize = 14f
            row.setPadding(dp(6), dp(8), dp(6), dp(8))
            if (done) {
                row.setTextColor(Color.parseColor("#9A9A9A"))
                row.paintFlags = row.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                row.setTextColor(Color.parseColor("#2B2B2B"))
            }
            val idx = i
            row.setOnClickListener {
                val t = arr.getJSONObject(idx)
                t.put("done", !t.optBoolean("done", false))
                saveData()
                renderReviewEditor()
                renderReview()
            }
            row.setOnLongClickListener {
                AlertDialog.Builder(this)
                    .setMessage("删除「" + itemObj.optString("text") + "」？")
                    .setPositiveButton("删除") { _, _ ->
                        val newArr = JSONArray()
                        for (j in 0 until arr.length()) {
                            if (j != idx) newArr.put(arr.getJSONObject(j))
                        }
                        rec.put("review", newArr)
                        saveData()
                        renderReviewEditor()
                        renderReview()
                    }
                    .setNegativeButton("取消", null)
                    .show()
                true
            }
            reviewEditorList.addView(row)
        }
    }

    /** 艾宾浩斯复习提醒：固定显示 1/2/4/7/15/30 天前的日期 + 对应复习项目（仅可勾选，不可删除） */
    private fun renderReview() {
        reviewContainer.removeAllViews()
        val intervals = intArrayOf(1, 2, 4, 7, 15, 30)
        val today = Calendar.getInstance()
        val dueKeys = ArrayList<String>()
        val dueDates = ArrayList<Calendar>()
        for (d in intervals) {
            val c = (today.clone() as Calendar).also { it.add(Calendar.DAY_OF_MONTH, -d) }
            dueKeys.add(keyOf(c))
            dueDates.add(c)
        }

        val dateNames = ArrayList<String>()
        for (c in dueDates) {
            dateNames.add(
                String.format(
                    Locale.getDefault(), "%02d/%02d",
                    c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)
                )
            )
        }
        val dateLine = TextView(this)
        dateLine.text = "需要复习的日期：" + dateNames.joinToString("、")
        dateLine.setTextColor(Color.parseColor("#2B2B2B"))
        dateLine.textSize = 13f
        dateLine.typeface = Typeface.DEFAULT_BOLD
        dateLine.setPadding(dp(2), dp(2), dp(2), dp(8))
        reviewContainer.addView(dateLine)

        var any = false
        for (i in dueKeys.indices) {
            val k = dueKeys[i]
            val c = dueDates[i]
            val rec = getRecordOrNull(k)
            val arr = rec?.optJSONArray("review") ?: JSONArray()
            if (arr.length() == 0) continue
            any = true
            val md = String.format(
                Locale.getDefault(), "%02d/%02d",
                c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)
            )
            val dateTv = TextView(this)
            dateTv.text = md
            dateTv.setTextColor(Color.parseColor("#7C3AED"))
            dateTv.textSize = 13f
            dateTv.typeface = Typeface.DEFAULT_BOLD
            dateTv.setPadding(dp(2), dp(8), dp(2), dp(2))
            reviewContainer.addView(dateTv)

            for (j in 0 until arr.length()) {
                val itemObj = arr.getJSONObject(j)
                val done = itemObj.optBoolean("done", false)
                val row = TextView(this)
                row.text = "    " + (if (done) "✓  " else "·  ") + itemObj.optString("text")
                row.textSize = 14f
                row.setPadding(dp(8), dp(6), dp(8), dp(6))
                if (done) {
                    row.setTextColor(Color.parseColor("#9A9A9A"))
                    row.paintFlags = row.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                } else {
                    row.setTextColor(Color.parseColor("#2B2B2B"))
                }
                val idx = j
                row.setOnClickListener {
                    val t = arr.getJSONObject(idx)
                    t.put("done", !t.optBoolean("done", false))
                    saveData()
                    renderReview()
                }
                reviewContainer.addView(row)
            }
        }
        if (!any) {
            val empty = TextView(this)
            empty.text = "所对应日期还没有复习项目（可在「每日积分」页选中该日期添加）"
            empty.setTextColor(Color.parseColor("#AAAAAA"))
            empty.textSize = 13f
            empty.setPadding(dp(2), dp(4), dp(2), dp(4))
            reviewContainer.addView(empty)
        }
    }

    private fun openAddTaskDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_add_task)

        val etName = dialog.findViewById<EditText>(R.id.etTaskName)
        val rg = dialog.findViewById<RadioGroup>(R.id.rgQuadrant)
        val btnConfirm = dialog.findViewById<Button>(R.id.btnConfirmTask)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancelTask)
        val btnLink = dialog.findViewById<Button>(R.id.btnLinkDate)
        val tvLinked = dialog.findViewById<TextView>(R.id.tvLinkedDates)

        val selectedDates = ArrayList<String>()
        fun refreshLinked() {
            tvLinked.text = if (selectedDates.isEmpty()) "未链接日期"
            else "已链接：" + selectedDates.joinToString("、")
        }
        refreshLinked()

        btnLink.setOnClickListener {
            openLinkDatePicker(selectedDates) { picked ->
                selectedDates.clear()
                selectedDates.addAll(picked)
                refreshLinked()
            }
        }

        btnConfirm.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "请填写待办内容", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val quadrant = when (rg.checkedRadioButtonId) {
                R.id.rbQ1 -> 1
                R.id.rbQ2 -> 2
                R.id.rbQ3 -> 3
                R.id.rbQ4 -> 4
                else -> 1
            }
            val arr = tasksArray()
            val item = JSONObject()
            item.put("name", name)
            item.put("quadrant", quadrant)
            item.put("done", false)
            item.put("id", nextTaskId())
            val datesArr = JSONArray()
            for (d in selectedDates) datesArr.put(d)
            item.put("dates", datesArr)
            arr.put(item)
            saveData()
            dialog.dismiss()
            switchTab(1)
            renderQuadrant()
        }
        btnCancel.setOnClickListener { dialog.dismiss() }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    /** 多选日期日历 */
    private fun openLinkDatePicker(initial: List<String>, onDone: (List<String>) -> Unit) {
        val dialog = Dialog(this)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(16), dp(16), dp(16), dp(16))
        root.setBackgroundColor(Color.WHITE)
        dialog.setContentView(root)

        val title = TextView(this)
        title.text = "链接到日期（可多选）"
        title.textSize = 16f
        title.typeface = Typeface.DEFAULT_BOLD
        title.setTextColor(Color.parseColor("#2B2B2B"))
        root.addView(title)

        val selected = HashSet<String>(initial)

        val monthRow = LinearLayout(this)
        monthRow.orientation = LinearLayout.HORIZONTAL
        monthRow.gravity = Gravity.CENTER_VERTICAL
        val btnPrev = Button(this)
        btnPrev.text = "◀"
        btnPrev.layoutParams = LinearLayout.LayoutParams(dp(44), dp(38))
        btnPrev.setPadding(0, 0, 0, 0)
        val tvMonth = TextView(this)
        tvMonth.gravity = Gravity.CENTER
        tvMonth.textSize = 14f
        tvMonth.typeface = Typeface.DEFAULT_BOLD
        tvMonth.layoutParams =
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        val btnNext = Button(this)
        btnNext.text = "▶"
        btnNext.layoutParams = LinearLayout.LayoutParams(dp(44), dp(38))
        btnNext.setPadding(0, 0, 0, 0)
        monthRow.addView(btnPrev)
        monthRow.addView(tvMonth)
        monthRow.addView(btnNext)
        root.addView(monthRow)

        val weekRow = LinearLayout(this)
        weekRow.orientation = LinearLayout.HORIZONTAL
        for (w in arrayOf("一", "二", "三", "四", "五", "六", "日")) {
            val tv = TextView(this)
            tv.text = w
            tv.gravity = Gravity.CENTER
            tv.textSize = 12f
            tv.setTextColor(Color.parseColor("#888888"))
            tv.layoutParams = LinearLayout.LayoutParams(0, dp(28), 1f)
            weekRow.addView(tv)
        }
        root.addView(weekRow)

        val grid = LinearLayout(this)
        grid.orientation = LinearLayout.VERTICAL
        root.addView(grid)

        val summary = TextView(this)
        summary.textSize = 12f
        summary.setTextColor(Color.parseColor("#666666"))
        summary.setPadding(0, dp(8), 0, 0)
        root.addView(summary)

        var viewYear = Calendar.getInstance().get(Calendar.YEAR)
        var viewMonth0 = Calendar.getInstance().get(Calendar.MONTH)

        fun refresh() {
            tvMonth.text = String.format(Locale.getDefault(), "%04d 年 %02d 月", viewYear, viewMonth0 + 1)
            grid.removeAllViews()
            val first = Calendar.getInstance()
            first.clear()
            first.set(viewYear, viewMonth0, 1)
            val offset = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7
            val daysInMonth = first.getActualMaximum(Calendar.DAY_OF_MONTH)

            var rowBox: LinearLayout? = null
            var col = 0
            for (i in 0 until offset) {
                if (rowBox == null) {
                    val rb = LinearLayout(this)
                    rb.orientation = LinearLayout.HORIZONTAL
                    grid.addView(rb)
                    rowBox = rb
                }
                val blank = TextView(this)
                blank.layoutParams = LinearLayout.LayoutParams(0, dp(36), 1f)
                rowBox!!.addView(blank)
                col++
            }
            var day = 1
            while (day <= daysInMonth) {
                if (rowBox == null || col == 7) {
                    val rb = LinearLayout(this)
                    rb.orientation = LinearLayout.HORIZONTAL
                    grid.addView(rb)
                    rowBox = rb
                    col = 0
                }
                val c = Calendar.getInstance()
                c.clear()
                c.set(viewYear, viewMonth0, day)
                val key = keyOf(c)
                val tv = TextView(this)
                tv.text = day.toString()
                tv.gravity = Gravity.CENTER
                tv.textSize = 13f
                tv.layoutParams = LinearLayout.LayoutParams(0, dp(36), 1f)
                if (selected.contains(key)) {
                    tv.setBackgroundColor(Color.parseColor("#7C3AED"))
                    tv.setTextColor(Color.WHITE)
                } else {
                    tv.setTextColor(Color.parseColor("#2B2B2B"))
                }
                tv.setOnClickListener {
                    if (selected.contains(key)) selected.remove(key) else selected.add(key)
                    refresh()
                }
                rowBox!!.addView(tv)
                col++
                day++
            }
            summary.text = if (selected.isEmpty()) "已选：无"
            else "已选：" + selected.sorted().joinToString("、")
        }

        btnPrev.setOnClickListener {
            if (viewMonth0 == 0) {
                viewMonth0 = 11
                viewYear--
            } else {
                viewMonth0--
            }
            refresh()
        }
        btnNext.setOnClickListener {
            if (viewMonth0 == 11) {
                viewMonth0 = 0
                viewYear++
            } else {
                viewMonth0++
            }
            refresh()
        }

        val btnRow = LinearLayout(this)
        btnRow.orientation = LinearLayout.HORIZONTAL
        btnRow.setPadding(0, dp(10), 0, 0)
        val cancel = Button(this)
        cancel.text = "取消"
        cancel.backgroundTintList =
            android.content.res.ColorStateList.valueOf(Color.parseColor("#94A3B8"))
        cancel.setTextColor(Color.WHITE)
        cancel.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        cancel.setOnClickListener { dialog.dismiss() }
        btnRow.addView(cancel)
        val ok = Button(this)
        ok.text = "完成"
        ok.backgroundTintList =
            android.content.res.ColorStateList.valueOf(Color.parseColor("#7C3AED"))
        ok.setTextColor(Color.WHITE)
        val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        lp.marginStart = dp(8)
        ok.layoutParams = lp
        ok.setOnClickListener {
            onDone(selected.sorted())
            dialog.dismiss()
        }
        btnRow.addView(ok)
        root.addView(btnRow)

        refresh()
        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    /** 每日积分页：只显示链接到当前日期的待办 */
    private fun renderDayTasks() {
        val key = keyOf(curDate)
        val arr = tasksArray()
        val hits = ArrayList<JSONObject>()
        for (i in 0 until arr.length()) {
            val t = arr.getJSONObject(i)
            val dates = t.optJSONArray("dates") ?: continue
            for (j in 0 until dates.length()) {
                if (dates.optString(j) == key) {
                    hits.add(t)
                    break
                }
            }
        }
        if (hits.isEmpty()) {
            dayTaskPanel.visibility = View.GONE
            return
        }
        dayTaskPanel.visibility = View.VISIBLE
        tvDayTaskTitle.text = "本日链接的待办（" + key + "）"
        dayTaskList.removeAllViews()
        for (t in hits) {
            val done = t.optBoolean("done", false)
            val tv = TextView(this)
            tv.text = qLabel(t.optInt("quadrant", 1)) + "  " +
                    (if (done) "✓  " else "·  ") + t.optString("name")
            tv.textSize = 15f
            tv.setPadding(dp(6), dp(10), dp(6), dp(10))
            if (done) {
                tv.setTextColor(Color.parseColor("#9A9A9A"))
                tv.paintFlags = tv.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                tv.setTextColor(Color.parseColor("#2B2B2B"))
            }
            tv.setOnClickListener {
                t.put("done", !t.optBoolean("done", false))
                saveData()
                renderDayTasks()
            }
            dayTaskList.addView(tv)
        }
    }

    // ---------------- 规划（嵌入待办） ----------------
    private fun nextTaskId(): Int {
        val n = data.optInt("nextTaskId", 1)
        data.put("nextTaskId", n + 1)
        return n
    }

    private fun migrateTasks() {
        if (!data.has("tasks")) {
            data.put("tasks", JSONArray())
            return
        }
        val arr = data.getJSONArray("tasks")
        var changed = false
        for (i in 0 until arr.length()) {
            val t = arr.getJSONObject(i)
            if (!t.has("id")) {
                t.put("id", nextTaskId())
                changed = true
            }
        }
        if (changed) saveData()
    }

    private fun plans(): JSONObject {
        if (!data.has("plans")) data.put("plans", JSONObject())
        return data.getJSONObject("plans")
    }

    private fun getPlan(key: String): String = plans().optString(key, "")

    private fun setPlan(key: String, text: String) {
        plans().put(key, text)
        saveData()
    }

    private fun findTaskById(id: Int?): JSONObject? {
        if (id == null) return null
        val arr = tasksArray()
        for (i in 0 until arr.length()) {
            val t = arr.getJSONObject(i)
            if (t.optInt("id", -1) == id) return t
        }
        return null
    }

    private fun resolveTask(token: String): JSONObject? {
        if (token.matches(Regex("\\d+"))) {
            return findTaskById(token.toIntOrNull())
        }
        val arr = tasksArray()
        for (i in 0 until arr.length()) {
            val t = arr.getJSONObject(i)
            if (t.optString("name") == token) return t
        }
        return null
    }

    private fun toggleTaskByName(name: String) {
        val arr = tasksArray()
        for (i in 0 until arr.length()) {
            val t = arr.getJSONObject(i)
            if (t.optString("name") == name) {
                t.put("done", !t.optBoolean("done", false))
                saveData()
                renderQuadrant()
                return
            }
        }
    }

    private fun qLabel(q: Int): String = when (q) {
        1 -> "①"
        2 -> "②"
        3 -> "③"
        else -> "④"
    }

    /** 待办若有链接日期，在名称前显示 MM-DD */
    private fun taskDatePrefix(t: JSONObject): String {
        val arr = t.optJSONArray("dates") ?: return ""
        if (arr.length() == 0) return ""
        val parts = ArrayList<String>()
        for (i in 0 until arr.length()) {
            val d = arr.optString(i)
            parts.add(if (d.length >= 10) d.substring(5) else d)
        }
        return "[" + parts.joinToString(",") + "] "
    }

    private fun renderPlans() {
        renderPlan(getPlan("today"), tvPlanToday, "（空）点上方「今日规划」标题编辑")
        renderPlan(getPlan("tomorrow"), tvPlanTomorrow, "（空）点上方「明日规划」标题编辑")
    }

    private fun renderPlan(raw: String, tv: TextView, placeholder: String) {
        if (raw.isEmpty()) {
            tv.text = placeholder
            tv.setTextColor(Color.parseColor("#AAAAAA"))
            tv.movementMethod = null
            return
        }
        val sb = android.text.SpannableStringBuilder()
        val regex = Regex("\\[\\[(.+?)\\]\\]")
        var last = 0
        for (m in regex.findAll(raw)) {
            sb.append(raw.substring(last, m.range.first))
            val task = resolveTask(m.groupValues[1])
            val name = if (task != null) task.optString("name") else "（已删除）"
            val start = sb.length
            sb.append(name)
            if (task != null) {
                val tname = task.optString("name")
                sb.setSpan(
                    object : android.text.style.ClickableSpan() {
                        override fun onClick(widget: View) {
                            toggleTaskByName(tname)
                        }

                        override fun updateDrawState(ds: android.text.TextPaint) {
                            ds.color = Color.parseColor("#2E9E6B")
                            ds.isUnderlineText = false
                        }
                    },
                    start, sb.length,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                if (task.optBoolean("done", false)) {
                    sb.setSpan(
                        android.text.style.StrikethroughSpan(),
                        start, sb.length,
                        android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
            last = m.range.last + 1
        }
        sb.append(raw.substring(last))
        tv.text = sb
        tv.setTextColor(Color.parseColor("#2B2B2B"))
        tv.movementMethod = android.text.method.LinkMovementMethod.getInstance()
    }

    private fun openEditPlan(key: String, title: String) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_edit_plan)

        val tvTitle = dialog.findViewById<TextView>(R.id.tvEditPlanTitle)
        val et = dialog.findViewById<EditText>(R.id.etPlanText)
        val btnInsert = dialog.findViewById<Button>(R.id.btnInsertTask)
        val btnSave = dialog.findViewById<Button>(R.id.btnSavePlan)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancelPlan)

        tvTitle.text = title
        et.setText(getPlan(key))
        et.setSelection(et.text.length)

        btnInsert.setOnClickListener { showTaskPicker(et) }
        btnSave.setOnClickListener {
            setPlan(key, et.text.toString())
            dialog.dismiss()
            renderPlans()
        }
        btnCancel.setOnClickListener { dialog.dismiss() }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    private fun showTaskPicker(et: EditText) {
        val arr = tasksArray()
        val labels = ArrayList<String>()
        val taskNames = ArrayList<String>()
        for (i in 0 until arr.length()) {
            val t = arr.getJSONObject(i)
            val done = if (t.optBoolean("done", false)) "✓ " else ""
            val nm = t.optString("name")
            labels.add(qLabel(t.optInt("quadrant", 1)) + "  " + done + nm)
            taskNames.add(nm)
        }
        if (labels.isEmpty()) {
            Toast.makeText(this, "四象限里还没有项目，先去加一个吧", Toast.LENGTH_SHORT).show()
            return
        }
        val items = Array<CharSequence>(labels.size) { labels[it] }
        AlertDialog.Builder(this)
            .setTitle("选择要插入的项目")
            .setItems(items) { _, which ->
                val token = "[[" + taskNames[which] + "]]"
                val start = et.selectionStart
                et.text.insert(if (start < 0) et.text.length else start, token)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // ---------------- 数据总览 ----------------
    private fun openDashboard() {
        dashYear = viewYear
        dashMonth0 = viewMonth0
        dashWeekStart = mondayOf(Calendar.getInstance())

        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_dashboard)
        val tvMonth = dialog.findViewById<TextView>(R.id.tvDashMonth)
        val tvWeek = dialog.findViewById<TextView>(R.id.tvDashWeek)
        val container = dialog.findViewById<LinearLayout>(R.id.dashboardContainer)

        fun refresh() {
            tvMonth.text = String.format(Locale.getDefault(), "%04d 年 %02d 月", dashYear, dashMonth0 + 1)
            val we = (dashWeekStart.clone() as Calendar).also { it.add(Calendar.DAY_OF_MONTH, 6) }
            tvWeek.text = String.format(
                Locale.getDefault(), "%02d/%02d - %02d/%02d",
                dashWeekStart.get(Calendar.MONTH) + 1, dashWeekStart.get(Calendar.DAY_OF_MONTH),
                we.get(Calendar.MONTH) + 1, we.get(Calendar.DAY_OF_MONTH)
            )
            buildCards(container)
        }

        dialog.findViewById<Button>(R.id.btnDashPrevMonth).setOnClickListener {
            val r = addMonths(dashYear, dashMonth0, -1)
            dashYear = r.first
            dashMonth0 = r.second
            refresh()
        }
        dialog.findViewById<Button>(R.id.btnDashNextMonth).setOnClickListener {
            val r = addMonths(dashYear, dashMonth0, 1)
            dashYear = r.first
            dashMonth0 = r.second
            refresh()
        }
        dialog.findViewById<Button>(R.id.btnDashPrevWeek).setOnClickListener {
            dashWeekStart = (dashWeekStart.clone() as Calendar).also { it.add(Calendar.DAY_OF_MONTH, -7) }
            refresh()
        }
        dialog.findViewById<Button>(R.id.btnDashNextWeek).setOnClickListener {
            dashWeekStart = (dashWeekStart.clone() as Calendar).also { it.add(Calendar.DAY_OF_MONTH, 7) }
            refresh()
        }
        dialog.findViewById<Button>(R.id.btnLedgerOverview).setOnClickListener {
            dialog.dismiss()
            openLedgerOverview()
        }
        dialog.findViewById<Button>(R.id.btnCloseDashboard).setOnClickListener { dialog.dismiss() }

        refresh()
        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94).toInt(),
            (resources.displayMetrics.heightPixels * 0.86).toInt()
        )
    }

    private fun buildCards(container: LinearLayout) {
        container.removeAllViews()
        for (cat in allCats()) {
            if (!cat.dashboard) continue
            val hdr = TextView(this)
            hdr.text = cat.name + "积分"
            hdr.setTextColor(Color.WHITE)
            hdr.setBackgroundColor(Color.parseColor(cat.color))
            hdr.setPadding(dp(10), dp(6), dp(10), dp(6))
            hdr.textSize = 14f
            hdr.typeface = Typeface.DEFAULT_BOLD
            val hlp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            hlp.setMargins(0, dp(10), 0, 0)
            hdr.layoutParams = hlp
            container.addView(hdr)

            val box = LinearLayout(this)
            box.orientation = LinearLayout.VERTICAL
            box.setBackgroundColor(Color.WHITE)
            box.setPadding(dp(10), dp(6), dp(10), dp(6))
            container.addView(box)

            val ySum = yearSum(dashYear, cat.key)
            val yCnt = yearCount(dashYear)
            val yAvg = if (yCnt > 0) ySum / yCnt else 0.0
            box.addView(lineText(String.format(Locale.getDefault(),
                "年：总计 %.1f ｜ 日均 %.1f（记录 %d 天）", ySum, yAvg, yCnt)))

            val mSum = monthSum(dashYear, dashMonth0, cat.key)
            val mCnt = monthCount(dashYear, dashMonth0)
            val mAvg = if (mCnt > 0) mSum / mCnt else 0.0
            box.addView(lineText(String.format(Locale.getDefault(),
                "月：总计 %.1f ｜ 日均 %.1f（记录 %d 天）", mSum, mAvg, mCnt)))

            val wSum = weekSum(dashWeekStart, cat.key)
            box.addView(lineText(String.format(Locale.getDefault(),
                "周：总计 %.1f ｜ 日均 %.1f（÷7）", wSum, wSum / 7.0)))

            val btnRow = LinearLayout(this)
            btnRow.orientation = LinearLayout.HORIZONTAL
            btnRow.setPadding(0, dp(6), 0, dp(4))
            container.addView(btnRow)

            val b0 = smallButton("年趋势") { showTrend(cat.key, 0) }
            val b1 = smallButton("月趋势") { showTrend(cat.key, 1) }
            val b2 = smallButton("周趋势") { showTrend(cat.key, 2) }
            val lp0 = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            lp0.marginEnd = dp(4)
            b0.layoutParams = lp0
            val lp1 = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            lp1.marginEnd = dp(4)
            b1.layoutParams = lp1
            b2.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            btnRow.addView(b0)
            btnRow.addView(b1)
            btnRow.addView(b2)
        }
    }

    private fun lineText(s: String): TextView {
        val tv = TextView(this)
        tv.text = s
        tv.setTextColor(Color.parseColor("#2B2B2B"))
        tv.textSize = 13f
        tv.setPadding(dp(2), dp(4), dp(2), dp(4))
        return tv
    }

    private fun smallButton(text: String, onClick: () -> Unit): Button {
        val b = Button(this)
        b.text = text
        b.textSize = 12f
        b.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#2E9E6B"))
        b.setTextColor(Color.WHITE)
        b.setOnClickListener { onClick() }
        return b
    }

    // ---- 统计辅助 ----
    private fun allKeys(): List<String> {
        val out = ArrayList<String>()
        val it = records().keys()
        while (it.hasNext()) out.add(it.next())
        out.sort()
        return out
    }

    private fun yearSum(year: Int, key: String): Double {
        val prefix = String.format(Locale.getDefault(), "%04d-", year)
        var sum = 0.0
        for (k in allKeys()) if (k.startsWith(prefix)) sum += catValue(getRecordOrNull(k), key)
        return sum
    }

    private fun yearCount(year: Int): Int {
        val prefix = String.format(Locale.getDefault(), "%04d-", year)
        var n = 0
        for (k in allKeys()) if (k.startsWith(prefix)) n++
        return n
    }

    private fun monthSum(year: Int, month0: Int, key: String): Double {
        val prefix = String.format(Locale.getDefault(), "%04d-%02d-", year, month0 + 1)
        var sum = 0.0
        for (k in allKeys()) if (k.startsWith(prefix)) sum += catValue(getRecordOrNull(k), key)
        return sum
    }

    private fun monthCount(year: Int, month0: Int): Int {
        val prefix = String.format(Locale.getDefault(), "%04d-%02d-", year, month0 + 1)
        var n = 0
        for (k in allKeys()) if (k.startsWith(prefix)) n++
        return n
    }

    private fun weekSum(weekStart: Calendar, key: String): Double {
        var sum = 0.0
        for (i in 0 until 7) {
            val c = weekStart.clone() as Calendar
            c.add(Calendar.DAY_OF_MONTH, i)
            sum += catValue(getRecordOrNull(keyOf(c)), key)
        }
        return sum
    }

    private fun mondayOf(src: Calendar): Calendar {
        val c = src.clone() as Calendar
        val dow = c.get(Calendar.DAY_OF_WEEK)
        val offset = if (dow == Calendar.SUNDAY) 6 else dow - 2
        c.add(Calendar.DAY_OF_MONTH, -offset)
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c
    }

    private fun addMonths(y: Int, m0: Int, delta: Int): Pair<Int, Int> {
        var y2 = y
        var m2 = m0 + delta
        while (m2 < 0) {
            m2 += 12
            y2--
        }
        while (m2 > 11) {
            m2 -= 12
            y2++
        }
        return Pair(y2, m2)
    }

    private fun showTrend(key: String, period: Int) {
        val cat = catByKey(key) ?: return
        val title = cat.name + when (period) {
            0 -> " · 年日均趋势"
            1 -> " · 月日均趋势"
            else -> " · 周日均趋势"
        }
        val counts = when (period) {
            0 -> intArrayOf(3, 5, 10)
            1 -> intArrayOf(6, 12, 24)
            else -> intArrayOf(4, 12, 24)
        }
        val names = when (period) {
            0 -> arrayOf("近3年", "近5年", "近10年")
            1 -> arrayOf("近6月", "近12月", "近24月")
            else -> arrayOf("近4周", "近12周", "近24周")
        }

        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_trend)
        val tvTitle = dialog.findViewById<TextView>(R.id.tvTrendTitle)
        val tvRange = dialog.findViewById<TextView>(R.id.tvTrendRange)
        val chart = dialog.findViewById<TrendChartView>(R.id.trendChart)
        tvTitle.text = title
        chart.chartColor = Color.parseColor(cat.color)

        val b1 = dialog.findViewById<Button>(R.id.btnRange1)
        val b2 = dialog.findViewById<Button>(R.id.btnRange2)
        val b3 = dialog.findViewById<Button>(R.id.btnRange3)
        val btnPrev = dialog.findViewById<Button>(R.id.btnTrendPrev)
        val btnNext = dialog.findViewById<Button>(R.id.btnTrendNext)

        var selIdx = 1
        var endOffset = 0

        fun build(count: Int) {
            val labels = ArrayList<String>()
            val values = ArrayList<Double>()
            when (period) {
                0 -> {
                    val endYear = dashYear - endOffset
                    for (i in count - 1 downTo 0) {
                        val y = endYear - i
                        val cnt = yearCount(y)
                        labels.add(y.toString())
                        values.add(if (cnt > 0) yearSum(y, key) / cnt else 0.0)
                    }
                }
                1 -> {
                    val end = addMonths(dashYear, dashMonth0, -endOffset)
                    for (i in count - 1 downTo 0) {
                        val r = addMonths(end.first, end.second, -i)
                        val cnt = monthCount(r.first, r.second)
                        labels.add(String.format(Locale.getDefault(), "%02d/%02d", r.first % 100, r.second + 1))
                        values.add(if (cnt > 0) monthSum(r.first, r.second, key) / cnt else 0.0)
                    }
                }
                else -> {
                    val endWeek = (dashWeekStart.clone() as Calendar).also {
                        it.add(Calendar.DAY_OF_MONTH, -7 * endOffset)
                    }
                    for (i in count - 1 downTo 0) {
                        val ws = (endWeek.clone() as Calendar).also { it.add(Calendar.DAY_OF_MONTH, -7 * i) }
                        labels.add(String.format(Locale.getDefault(), "%02d/%02d",
                            ws.get(Calendar.MONTH) + 1, ws.get(Calendar.DAY_OF_MONTH)))
                        values.add(weekSum(ws, key) / 7.0)
                    }
                }
            }
            tvRange.text = if (labels.isNotEmpty()) labels.first() + " ~ " + labels.last() else ""
            chart.labels = labels
            chart.values = values
            chart.invalidate()
        }

        fun refreshChart() {
            build(counts[selIdx])
        }

        fun apply(idx: Int) {
            selIdx = idx
            val on = Color.parseColor("#2E9E6B")
            val off = Color.parseColor("#B0BEC5")
            b1.text = names[0]
            b2.text = names[1]
            b3.text = names[2]
            b1.backgroundTintList = android.content.res.ColorStateList.valueOf(if (idx == 0) on else off)
            b2.backgroundTintList = android.content.res.ColorStateList.valueOf(if (idx == 1) on else off)
            b3.backgroundTintList = android.content.res.ColorStateList.valueOf(if (idx == 2) on else off)
            refreshChart()
        }

        b1.setOnClickListener { apply(0) }
        b2.setOnClickListener { apply(1) }
        b3.setOnClickListener { apply(2) }
        btnPrev.setOnClickListener {
            endOffset++
            refreshChart()
        }
        btnNext.setOnClickListener {
            if (endOffset > 0) {
                endOffset--
                refreshChart()
            }
        }
        apply(1)

        dialog.findViewById<Button>(R.id.btnCloseTrend).setOnClickListener { dialog.dismiss() }
        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    private fun moveTomorrowToToday() {
        val tomorrow = getPlan("tomorrow")
        if (tomorrow.isEmpty()) {
            Toast.makeText(this, "明日规划是空的", Toast.LENGTH_SHORT).show()
            return
        }
        setPlan("today", tomorrow)
        setPlan("tomorrow", "")
        renderPlans()
        Toast.makeText(this, "已把明日规划移到今日", Toast.LENGTH_SHORT).show()
    }

    // ---------------- 账本总览 ----------------
    private fun yearExpense(year: Int): Double {
        val prefix = String.format(Locale.getDefault(), "%04d-", year)
        var s = 0.0
        for (k in allKeys()) if (k.startsWith(prefix)) s += dayExpense(getRecordOrNull(k))
        return s
    }

    private fun monthExpense(year: Int, month0: Int): Double {
        val prefix = String.format(Locale.getDefault(), "%04d-%02d-", year, month0 + 1)
        var s = 0.0
        for (k in allKeys()) if (k.startsWith(prefix)) s += dayExpense(getRecordOrNull(k))
        return s
    }

    private fun weekExpense(weekStart: Calendar): Double {
        var s = 0.0
        for (i in 0 until 7) {
            val c = weekStart.clone() as Calendar
            c.add(Calendar.DAY_OF_MONTH, i)
            s += dayExpense(getRecordOrNull(keyOf(c)))
        }
        return s
    }

    private fun openLedgerOverview() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_ledger_overview)
        val summary = dialog.findViewById<LinearLayout>(R.id.ledgerSummary)
        val list = dialog.findViewById<LinearLayout>(R.id.ledgerList)

        val now = Calendar.getInstance()
        val today = dayExpense(getRecordOrNull(keyOf(now)))
        val month = monthExpense(now.get(Calendar.YEAR), now.get(Calendar.MONTH))
        val year = yearExpense(now.get(Calendar.YEAR))
        val daysPassed = now.get(Calendar.DAY_OF_MONTH)
        val dayAvg = if (daysPassed > 0) month / daysPassed else 0.0

        summary.removeAllViews()
        val lines = arrayOf(
            "今日：￥" + fmtMoney(today),
            "本月：￥" + fmtMoney(month),
            "今年：￥" + fmtMoney(year),
            String.format(Locale.getDefault(), "日均：￥%.2f（本月已过 %d 天）", dayAvg, daysPassed)
        )
        for (s in lines) {
            val tv = TextView(this)
            tv.text = s
            tv.setTextColor(Color.parseColor("#2B2B2B"))
            tv.textSize = 14f
            tv.setPadding(0, dp(4), 0, dp(4))
            summary.addView(tv)
        }

        list.removeAllViews()
        var any = false
        for (k in allKeys().sortedDescending()) {
            val rec = getRecordOrNull(k) ?: continue
            val arr = rec.optJSONArray("expenses") ?: continue
            if (arr.length() == 0) continue
            any = true

            val hdr = TextView(this)
            hdr.text = k
            hdr.setTextColor(Color.WHITE)
            hdr.setBackgroundColor(Color.parseColor("#C2410C"))
            hdr.setPadding(dp(8), dp(6), dp(8), dp(6))
            hdr.textSize = 13f
            hdr.typeface = Typeface.DEFAULT_BOLD
            val hl = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            hl.setMargins(0, dp(8), 0, 0)
            hdr.layoutParams = hl
            list.addView(hdr)

            var dayTotal = 0.0
            for (i in 0 until arr.length()) {
                val e = arr.getJSONObject(i)
                val price = e.optDouble("price", 0.0)
                dayTotal += price
                val row = TextView(this)
                row.text = "    " + e.optString("name") + "    ￥" + fmtMoney(price)
                row.setTextColor(Color.parseColor("#2B2B2B"))
                row.textSize = 14f
                row.setPadding(dp(8), dp(6), dp(8), dp(6))
                list.addView(row)
            }
            val tot = TextView(this)
            tot.text = "    当日合计  ￥" + fmtMoney(dayTotal)
            tot.setTextColor(Color.parseColor("#C2410C"))
            tot.textSize = 13f
            tot.setPadding(dp(8), dp(4), dp(8), dp(8))
            list.addView(tot)
        }
        if (!any) {
            val tv = TextView(this)
            tv.text = "还没有记账记录"
            tv.setTextColor(Color.parseColor("#AAAAAA"))
            tv.textSize = 13f
            list.addView(tv)
        }

        dialog.findViewById<Button>(R.id.btnLedgerYear).setOnClickListener { showExpenseTrend(0) }
        dialog.findViewById<Button>(R.id.btnLedgerMonth).setOnClickListener { showExpenseTrend(1) }
        dialog.findViewById<Button>(R.id.btnLedgerWeek).setOnClickListener { showExpenseTrend(2) }
        dialog.findViewById<Button>(R.id.btnCloseLedgerOverview).setOnClickListener { dialog.dismiss() }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94).toInt(),
            (resources.displayMetrics.heightPixels * 0.88).toInt()
        )
    }

    private fun showExpenseTrend(period: Int) {
        val title = "账本 · " + when (period) {
            0 -> "年支出趋势"
            1 -> "月支出趋势"
            else -> "周支出趋势"
        }
        val counts = when (period) {
            0 -> intArrayOf(3, 5, 10)
            1 -> intArrayOf(6, 12, 24)
            else -> intArrayOf(4, 12, 24)
        }
        val names = when (period) {
            0 -> arrayOf("近3年", "近5年", "近10年")
            1 -> arrayOf("近6月", "近12月", "近24月")
            else -> arrayOf("近4周", "近12周", "近24周")
        }

        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_trend)
        val tvTitle = dialog.findViewById<TextView>(R.id.tvTrendTitle)
        val tvRange = dialog.findViewById<TextView>(R.id.tvTrendRange)
        val chart = dialog.findViewById<TrendChartView>(R.id.trendChart)
        tvTitle.text = title
        chart.chartColor = Color.parseColor("#C2410C")

        val b1 = dialog.findViewById<Button>(R.id.btnRange1)
        val b2 = dialog.findViewById<Button>(R.id.btnRange2)
        val b3 = dialog.findViewById<Button>(R.id.btnRange3)
        val btnPrev = dialog.findViewById<Button>(R.id.btnTrendPrev)
        val btnNext = dialog.findViewById<Button>(R.id.btnTrendNext)

        var selIdx = 1
        var endOffset = 0
        val now = Calendar.getInstance()

        fun build(count: Int) {
            val labels = ArrayList<String>()
            val values = ArrayList<Double>()
            when (period) {
                0 -> {
                    val endYear = now.get(Calendar.YEAR) - endOffset
                    for (i in count - 1 downTo 0) {
                        val y = endYear - i
                        labels.add(y.toString())
                        values.add(yearExpense(y))
                    }
                }
                1 -> {
                    val end = addMonths(now.get(Calendar.YEAR), now.get(Calendar.MONTH), -endOffset)
                    for (i in count - 1 downTo 0) {
                        val r = addMonths(end.first, end.second, -i)
                        labels.add(String.format(Locale.getDefault(), "%02d月", r.second + 1))
                        values.add(monthExpense(r.first, r.second))
                    }
                }
                else -> {
                    val endWeek = mondayOf(now)
                    endWeek.add(Calendar.DAY_OF_MONTH, -7 * endOffset)
                    for (i in count - 1 downTo 0) {
                        val ws = (endWeek.clone() as Calendar).also { it.add(Calendar.DAY_OF_MONTH, -7 * i) }
                        labels.add(String.format(Locale.getDefault(), "%02d/%02d",
                            ws.get(Calendar.MONTH) + 1, ws.get(Calendar.DAY_OF_MONTH)))
                        values.add(weekExpense(ws))
                    }
                }
            }
            tvRange.text = if (labels.isNotEmpty()) labels.first() + " ~ " + labels.last() else ""
            chart.labels = labels
            chart.values = values
            chart.invalidate()
        }

        fun apply(idx: Int) {
            selIdx = idx
            val on = Color.parseColor("#C2410C")
            val off = Color.parseColor("#B0BEC5")
            b1.text = names[0]
            b2.text = names[1]
            b3.text = names[2]
            b1.backgroundTintList = android.content.res.ColorStateList.valueOf(if (idx == 0) on else off)
            b2.backgroundTintList = android.content.res.ColorStateList.valueOf(if (idx == 1) on else off)
            b3.backgroundTintList = android.content.res.ColorStateList.valueOf(if (idx == 2) on else off)
            build(counts[idx])
        }

        b1.setOnClickListener { apply(0) }
        b2.setOnClickListener { apply(1) }
        b3.setOnClickListener { apply(2) }
        btnPrev.setOnClickListener {
            endOffset++
            build(counts[selIdx])
        }
        btnNext.setOnClickListener {
            if (endOffset > 0) {
                endOffset--
                build(counts[selIdx])
            }
        }
        apply(1)

        dialog.findViewById<Button>(R.id.btnCloseTrend).setOnClickListener { dialog.dismiss() }
        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    // ---------------- 备份（导出 / 导入） ----------------
    private fun openBackupDialog() {
        AlertDialog.Builder(this)
            .setTitle("导入 / 导出备份")
            .setItems(arrayOf("导出备份（保存为文件）", "导入备份（从文件恢复）")) { _, which ->
                if (which == 0) exportBackup() else importBackup()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun exportBackup() {
        val name = "studypoints_" +
                SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
                    .format(Calendar.getInstance().time) + ".json"
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "application/json"
        intent.putExtra(Intent.EXTRA_TITLE, name)
        try {
            startActivityForResult(intent, REQ_EXPORT)
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开保存界面", Toast.LENGTH_SHORT).show()
        }
    }

    private fun importBackup() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        try {
            startActivityForResult(intent, REQ_IMPORT)
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开文件选择", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        if (requestCode == REQ_EXPORT) {
            try {
                val os = contentResolver.openOutputStream(uri)
                if (os == null) {
                    Toast.makeText(this, "导出失败：无法写入", Toast.LENGTH_LONG).show()
                    return
                }
                os.use { it.write(this.data.toString().toByteArray(Charsets.UTF_8)) }
                Toast.makeText(this, "已导出备份", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "导出失败：" + e.message, Toast.LENGTH_LONG).show()
            }
        } else if (requestCode == REQ_IMPORT) {
            try {
                val ins = contentResolver.openInputStream(uri)
                if (ins == null) {
                    Toast.makeText(this, "导入失败：无法读取", Toast.LENGTH_LONG).show()
                    return
                }
                val text = ins.bufferedReader(Charsets.UTF_8).use { it.readText() }
                if (text.isEmpty()) {
                    Toast.makeText(this, "文件是空的", Toast.LENGTH_SHORT).show()
                    return
                }
                val obj = JSONObject(text)
                if (!obj.has("records") && !obj.has("tasks")) {
                    Toast.makeText(this, "不是有效的备份文件", Toast.LENGTH_SHORT).show()
                    return
                }
                AlertDialog.Builder(this)
                    .setTitle("确认导入")
                    .setMessage("导入会覆盖当前全部数据，确定吗？")
                    .setPositiveButton("导入") { _, _ ->
                        this.data = obj
                        saveData()
                        invalidateCats()
                        rebuildCategoryInputs()
                        switchTab(0)
                        loadDay()
                        renderQuadrant()
                        Toast.makeText(this, "导入成功", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("取消", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(this, "导入失败：" + e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        saveData()
    }
}
