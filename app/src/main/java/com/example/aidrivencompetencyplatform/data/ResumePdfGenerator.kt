package com.example.aidrivencompetencyplatform.data

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import com.example.aidrivencompetencyplatform.model.ResumeAnalysisResult
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

    fun generateCleanResumePdf(
        context: Context,
        resume: ResumeAnalysisResult,
        targetRole: String? = null
    ): File? {
        val document = PdfDocument()
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var currentY = MARGIN_TOP

        val namePaint = TextPaint().apply {
            color = Color.rgb(15, 23, 42) // #0F172A
            textSize = 18f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val contactPaint = TextPaint().apply {
            color = Color.rgb(71, 85, 105) // #475569
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        val sectionHeaderPaint = TextPaint().apply {
            color = Color.rgb(15, 23, 42) // #0F172A
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val dividerPaint = Paint().apply {
            color = Color.rgb(203, 213, 225) // #CBD5E1
            strokeWidth = 1f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val itemTitlePaint = TextPaint().apply {
            color = Color.rgb(15, 23, 42) // #0F172A
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val itemSubtitlePaint = TextPaint().apply {
            color = Color.rgb(71, 85, 105) // #475569
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
            isAntiAlias = true
        }

        val bodyPaint = TextPaint().apply {
            color = Color.rgb(51, 65, 85) // #334155
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
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

        fun drawSectionTitle(title: String) {
            checkPageBreak(30f)
            currentY += 8f
            canvas.drawText(title.uppercase(), MARGIN_LEFT, currentY + 10f, sectionHeaderPaint)
            currentY += 14f
            canvas.drawLine(MARGIN_LEFT, currentY, PAGE_WIDTH - MARGIN_RIGHT, currentY, dividerPaint)
            currentY += 8f
        }

        fun drawWrappedText(text: String, paint: TextPaint, indent: Float = 0f, lineSpacingExtra: Float = 2f) {
            val availableWidth = (CONTENT_WIDTH - indent).toInt()
            if (availableWidth <= 0) return

            val staticLayout = StaticLayout.Builder.obtain(text, 0, text.length, paint, availableWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(lineSpacingExtra, 1f)
                .setIncludePad(false)
                .build()

            val height = staticLayout.height.toFloat()
            checkPageBreak(height + 4f)

            canvas.save()
            canvas.translate(MARGIN_LEFT + indent, currentY)
            staticLayout.draw(canvas)
            canvas.restore()

            currentY += height + lineSpacingExtra
        }

        try {
            // 1. Candidate Name & Target Role
            val candidateName = resume.candidateName?.ifBlank { "Candidate" } ?: "Candidate"
            canvas.drawText(candidateName, MARGIN_LEFT, currentY + 16f, namePaint)
            currentY += 22f

            // Target Role subtitle if provided
            val role = targetRole ?: resume.targetRole
            if (!role.isNullOrBlank()) {
                val rolePaint = TextPaint().apply {
                    color = Color.rgb(37, 99, 235) // #2563EB Primary Blue
                    textSize = 10f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    isAntiAlias = true
                }
                canvas.drawText(role, MARGIN_LEFT, currentY + 9f, rolePaint)
                currentY += 14f
            }

            // Contact Info line
            val contactItems = mutableListOf<String>()
            resume.candidateEmail?.let { if (it.isNotBlank()) contactItems.add(it) }
            resume.candidatePhone?.let { if (it.isNotBlank()) contactItems.add(it) }
            resume.candidateLocation?.let { if (it.isNotBlank()) contactItems.add(it) }
            if (contactItems.isNotEmpty()) {
                val contactLine = contactItems.joinToString("  •  ")
                canvas.drawText(contactLine, MARGIN_LEFT, currentY + 8f, contactPaint)
                currentY += 14f
            }
            currentY += 6f

            // 2. Summary
            if (!resume.summary.isNullOrBlank()) {
                drawSectionTitle("Professional Summary")
                drawWrappedText(resume.summary.trim(), bodyPaint, indent = 0f, lineSpacingExtra = 3f)
            }

            // 3. Experience
            if (!resume.experience.isNullOrEmpty()) {
                drawSectionTitle("Work Experience")
                for (exp in resume.experience) {
                    val lines = exp.lines().map { it.trim() }.filter { it.isNotBlank() }
                    if (lines.isEmpty()) continue

                    // First line usually has Role & Company
                    val headerLine = lines.first()
                    checkPageBreak(24f)
                    canvas.drawText(headerLine, MARGIN_LEFT, currentY + 9f, itemTitlePaint)
                    currentY += 13f

                    // Remaining lines are bullets or descriptions
                    for (i in 1 until lines.size) {
                        val line = lines[i]
                        val cleanBullet = line.removePrefix("•").removePrefix("-").removePrefix("*").trim()
                        checkPageBreak(16f)
                        canvas.drawText("•", MARGIN_LEFT + 4f, currentY + 9f, bodyPaint)
                        drawWrappedText(cleanBullet, bodyPaint, indent = 16f, lineSpacingExtra = 2f)
                    }
                    currentY += 6f
                }
            }

            // 4. Skills
            if (!resume.extractedSkills.isNullOrEmpty()) {
                drawSectionTitle("Technical Skills")
                val skillsText = resume.extractedSkills.joinToString(", ") { it.name }
                drawWrappedText(skillsText, bodyPaint, indent = 0f, lineSpacingExtra = 3f)
                currentY += 4f
            }

            // 5. Projects
            if (!resume.projectAnalysis.isNullOrEmpty()) {
                drawSectionTitle("Key Projects")
                for (proj in resume.projectAnalysis) {
                    checkPageBreak(22f)
                    val projHeader = proj.name
                    canvas.drawText(projHeader, MARGIN_LEFT, currentY + 9f, itemTitlePaint)
                    currentY += 13f

                    if (!proj.technologies.isNullOrEmpty()) {
                        val techText = "Technologies: ${proj.technologies.joinToString(", ")}"
                        drawWrappedText(techText, itemSubtitlePaint, indent = 8f, lineSpacingExtra = 2f)
                    }

                    if (!proj.strengths.isNullOrEmpty()) {
                        for (desc in proj.strengths) {
                            val cleanBullet = desc.removePrefix("•").removePrefix("-").trim()
                            checkPageBreak(16f)
                            canvas.drawText("•", MARGIN_LEFT + 4f, currentY + 9f, bodyPaint)
                            drawWrappedText(cleanBullet, bodyPaint, indent = 16f, lineSpacingExtra = 2f)
                        }
                    }
                    currentY += 6f
                }
            }

            // 6. Education
            if (!resume.education.isNullOrEmpty()) {
                drawSectionTitle("Education")
                for (edu in resume.education) {
                    checkPageBreak(18f)
                    canvas.drawText("•", MARGIN_LEFT + 4f, currentY + 9f, bodyPaint)
                    drawWrappedText(edu.trim(), bodyPaint, indent = 16f, lineSpacingExtra = 2f)
                    currentY += 3f
                }
            }

            // 7. Certifications
            if (!resume.certifications.isNullOrEmpty()) {
                drawSectionTitle("Certifications")
                for (cert in resume.certifications) {
                    checkPageBreak(18f)
                    canvas.drawText("•", MARGIN_LEFT + 4f, currentY + 9f, bodyPaint)
                    drawWrappedText(cert.trim(), bodyPaint, indent = 16f, lineSpacingExtra = 2f)
                    currentY += 3f
                }
            }

            document.finishPage(page)

            // Save to cache file
            val outputDir = File(context.cacheDir, "resumes").apply { if (!exists()) mkdirs() }
            val outputFile = File(outputDir, "Optimized_Resume_${System.currentTimeMillis()}.pdf")
            FileOutputStream(outputFile).use { out ->
                document.writeTo(out)
            }
            document.close()
            Log.d(TAG, "Generated clean vector PDF at ${outputFile.absolutePath}")
            return outputFile

        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate clean resume PDF", e)
            try {
                document.close()
            } catch (_: Exception) {}
            return null
        }
    }
}
