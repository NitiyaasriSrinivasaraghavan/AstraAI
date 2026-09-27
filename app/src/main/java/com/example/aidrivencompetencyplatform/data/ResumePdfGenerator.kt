package com.example.aidrivencompetencyplatform.data

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import com.example.aidrivencompetencyplatform.model.ResumeModificationReport
import com.example.aidrivencompetencyplatform.model.ReportPriority
import java.io.File
import java.io.FileOutputStream

object ResumePdfGenerator {

    private const val TAG = "ResumePdfGenerator"
    private const val PAGE_WIDTH = 595 // A4 width in points (72 dpi)
    private const val PAGE_HEIGHT = 842 // A4 height in points
    private const val MARGIN_LEFT = 40f
    private const val MARGIN_RIGHT = 40f
    private const val MARGIN_TOP = 40f
    private const val MARGIN_BOTTOM = 40f
    private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT

    /**
     * Generates a PDF version of the Resume Modification Report.
     */
    fun generateModificationReportPdf(
        context: Context,
        report: ResumeModificationReport
    ): File? {
        val document = PdfDocument()
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var currentY = MARGIN_TOP

        val titlePaint = TextPaint().apply {
            color = Color.rgb(30, 41, 59) // Slate 800
            textSize = 20f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val headerPaint = TextPaint().apply {
            color = Color.rgb(15, 23, 42) // Slate 900
            textSize = 12f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val normalPaint = TextPaint().apply {
            color = Color.rgb(71, 85, 105) // Slate 600
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        val priorityPaintRed = TextPaint().apply {
            color = Color.rgb(225, 29, 72) // Rose 600
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val priorityPaintOrange = TextPaint().apply {
            color = Color.rgb(245, 158, 11) // Amber 500
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val priorityPaintGreen = TextPaint().apply {
            color = Color.rgb(22, 163, 74) // Green 600
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        fun checkPageBreak(neededHeight: Float) {
            if (currentY + neededHeight > PAGE_HEIGHT - MARGIN_BOTTOM) {
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                currentY = MARGIN_TOP
            }
        }

        fun drawWrappedText(text: String, paint: TextPaint, indent: Float = 0f, spacing: Float = 2f) {
            val availableWidth = (CONTENT_WIDTH - indent).toInt()
            if (availableWidth <= 0) return

            val staticLayout = StaticLayout.Builder.obtain(text, 0, text.length, paint, availableWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(spacing, 1f)
                .setIncludePad(false)
                .build()

            val height = staticLayout.height.toFloat()
            checkPageBreak(height + 10f)

            canvas.save()
            canvas.translate(MARGIN_LEFT + indent, currentY)
            staticLayout.draw(canvas)
            canvas.restore()

            currentY += height + spacing + 5f
        }

        try {
            // Title
            canvas.drawText("Resume Modification Report", MARGIN_LEFT, currentY + 20f, titlePaint)
            currentY += 40f

            // Summary Info
            drawWrappedText("Target Role: ${report.targetRole}", headerPaint)
            drawWrappedText("Overall Match: ${report.overallMatch}%", headerPaint)
            drawWrappedText(report.summary, normalPaint)
            currentY += 10f

            // Summary Stats
            drawWrappedText("Matches found: ${report.strongMatchCount}", priorityPaintGreen)
            drawWrappedText("Recommended improvements: ${report.recommendedImprovementCount}", priorityPaintOrange)
            drawWrappedText("Potential gaps identified: ${report.potentialGapCount}", priorityPaintRed)
            currentY += 10f

            // Section Analysis
            for (section in report.sections) {
                checkPageBreak(80f)
                currentY += 20f
                
                val pPaint = when (section.priority) {
                    ReportPriority.HIGH -> priorityPaintRed
                    ReportPriority.RECOMMENDED -> priorityPaintOrange
                    ReportPriority.ALIGNED -> priorityPaintGreen
                }
                
                drawWrappedText("${section.sectionName} - Status: ${section.priority}", pPaint)
                
                drawWrappedText("Original Content:", headerPaint.apply { textSize = 10f })
                drawWrappedText(section.currentContent, normalPaint, indent = 10f)
                
                drawWrappedText("Analysis & Reasoning:", headerPaint.apply { textSize = 10f })
                drawWrappedText(section.reason, normalPaint, indent = 10f)
                
                drawWrappedText("Action Required:", headerPaint.apply { textSize = 10f })
                drawWrappedText(section.modificationRequired, normalPaint.apply { color = Color.BLACK }, indent = 10f)
                
                if (section.suggestedConsiderations.isNotEmpty()) {
                    drawWrappedText("Consider adding (if true):", headerPaint.apply { textSize = 10f })
                    drawWrappedText(section.suggestedConsiderations.joinToString(", "), normalPaint, indent = 10f)
                }
                
                if (section.caution != null) {
                    drawWrappedText("⚠️ Advisory Note:", priorityPaintRed.apply { textSize = 9f })
                    drawWrappedText(section.caution, normalPaint, indent = 10f)
                }
                
                canvas.drawLine(MARGIN_LEFT, currentY, PAGE_WIDTH - MARGIN_RIGHT, currentY, Paint().apply { color = Color.LTGRAY; strokeWidth = 0.5f })
                currentY += 5f
            }

            document.finishPage(page)

            val outputDir = File(context.cacheDir, "reports").apply { if (!exists()) mkdirs() }
            val outputFile = File(outputDir, "NoviQ_Analysis_Report_${System.currentTimeMillis()}.pdf")
            FileOutputStream(outputFile).use { out ->
                document.writeTo(out)
            }
            document.close()
            return outputFile

        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate report PDF", e)
            try { document.close() } catch(_: Exception) {}
            return null
        }
    }
}
