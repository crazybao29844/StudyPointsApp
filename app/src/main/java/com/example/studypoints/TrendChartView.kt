package com.example.studypoints

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import java.util.Locale

/** 极简柱状趋势图：数值标签 + 自动比例适配（不依赖第三方库） */
class TrendChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var labels: List<String> = emptyList()
    var values: List<Double> = emptyList()
    var chartColor: Int = Color.parseColor("#2E9E6B")

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E5E5E5")
        strokeWidth = 2f
    }
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val axisText = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#888888")
    }
    private val valueText = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#333333")
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val padL = 58f
        val padR = 16f
        val padT = 28f
        val padB = 50f
        val plotW = w - padL - padR
        val plotH = h - padT - padB
        if (values.isEmpty() || plotW <= 0f || plotH <= 0f) return

        val n = values.size
        val maxRaw = values.maxOrNull() ?: 0.0
        if (maxRaw <= 0.0) {
            axisText.textSize = 26f
            val t = "暂无数据"
            canvas.drawText(t, w / 2f - axisText.measureText(t) / 2f, h / 2f, axisText)
            return
        }
        val maxV = niceMax(maxRaw)

        // Y 轴网格 + 刻度（比例自动适配）
        val steps = 4
        axisText.textSize = 20f
        for (i in 0..steps) {
            val y = padT + plotH * i / steps
            canvas.drawLine(padL, y, padL + plotW, y, gridPaint)
            val v = maxV * (steps - i) / steps
            canvas.drawText(fmt(v), 4f, y + 6f, axisText)
        }

        val slot = plotW / n
        val barW = slot * 0.6f
        barPaint.color = chartColor

        // 数值字号随柱子数量自动缩放
        valueText.textSize = when {
            n <= 8 -> 18f
            n <= 12 -> 15f
            else -> 12f
        }
        // 柱子太多时，数值标签隔一根显示
        val showEvery = if (n <= 12) 1 else 2

        for (i in 0 until n) {
            val v = values[i]
            val bh = (plotH * v / maxV).toFloat()
            val x = padL + slot * i + (slot - barW) / 2f
            val y = padT + plotH - bh
            canvas.drawRect(x, y, x + barW, padT + plotH, barPaint)

            // 数值标签
            if (i % showEvery == 0) {
                val txt = fmt(v)
                val tw = valueText.measureText(txt)
                if (tw <= slot * showEvery * 1.1f) {
                    canvas.drawText(txt, x + barW / 2f - tw / 2f, y - 6f, valueText)
                }
            }
        }

        // X 标签：自动抽稀
        val stepX = when {
            n <= 8 -> 1
            n <= 14 -> 2
            else -> 3
        }
        axisText.textSize = 18f
        for (i in 0 until n) {
            if (i % stepX != 0 && i != n - 1) continue
            val lbl = labels.getOrElse(i) { "" }
            val tw = axisText.measureText(lbl)
            canvas.drawText(lbl, padL + slot * i + slot / 2f - tw / 2f, padT + plotH + 32f, axisText)
        }
    }

    /** 把最大值向上取整到“好看”的刻度：1/2/2.5/5/10 × 10^n */
    private fun niceMax(v: Double): Double {
        if (v <= 0.0) return 1.0
        val exp = Math.floor(Math.log10(v))
        val base = Math.pow(10.0, exp)
        val f = v / base
        val nf = when {
            f <= 1.0 -> 1.0
            f <= 1.5 -> 1.5
            f <= 2.0 -> 2.0
            f <= 2.5 -> 2.5
            f <= 3.0 -> 3.0
            f <= 4.0 -> 4.0
            f <= 5.0 -> 5.0
            f <= 6.0 -> 6.0
            f <= 8.0 -> 8.0
            else -> 10.0
        }
        return nf * base
    }

    private fun fmt(v: Double): String {
        return if (v == Math.floor(v) && !v.isInfinite()) v.toLong().toString()
        else String.format(Locale.getDefault(), "%.1f", v)
    }
}
