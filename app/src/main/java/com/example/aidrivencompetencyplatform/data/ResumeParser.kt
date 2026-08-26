package com.example.aidrivencompetencyplatform.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.InputStream
import java.util.zip.ZipInputStream

class ResumeParser(private val context: Context) {

    init {
        PDFBoxResourceLoader.init(context)
    }

    companion object {
        const val ERROR_SCANNED_PDF = "[ERROR_SCANNED_PDF]"
    }

    fun extractText(uri: Uri): String {
        val mimeType = context.contentResolver.getType(uri)
        val extension = getExtensionFromUri(uri, mimeType)
        
        Log.d("ResumeParser", "Uri: $uri")
        Log.d("ResumeParser", "Detected MIME: $mimeType, Extension: $extension")

        return try {
            val text = when {
                mimeType == "application/pdf" || extension.equals("pdf", ignoreCase = true) -> {
                    extractFromPdf(uri)
                }
                mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" || 
                extension.equals("docx", ignoreCase = true) -> {
                    extractFromDocx(uri)
                }
                else -> {
                    Log.w("ResumeParser", "Unsupported file type: $mimeType / $extension")
                    ""
                }
            }
            
            if (text == ERROR_SCANNED_PDF) return text

            val cleanedText = cleanText(text)
            Log.d("ResumeParser", "Extracted text length: ${cleanedText.length}")
            cleanedText
        } catch (e: Exception) {
            Log.e("ResumeParser", "Text extraction failed", e)
            ""
        }
    }

    private fun extractFromPdf(uri: Uri): String {
        Log.d("ResumeParser", "Starting PDF extraction...")
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val document = PDDocument.load(inputStream)
            val pageCount = document.numberOfPages
            Log.d("ResumeParser", "PDF Pages: $pageCount")
            
            val stripper = PDFTextStripper()
            val text = stripper.getText(document)
            document.close()
            
            // Basic heuristic to detect scanned PDFs (images instead of text)
            if (text.trim().length < 50 && pageCount > 0) {
                Log.w("ResumeParser", "PDF has $pageCount pages but almost no text. Likely scanned.")
                return ERROR_SCANNED_PDF
            }
            
            return text
        }
        return ""
    }

    private fun extractFromDocx(uri: Uri): String {
        Log.d("ResumeParser", "Starting DOCX extraction...")
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val zipInputStream = ZipInputStream(inputStream)
            var entry = zipInputStream.nextEntry
            while (entry != null) {
                // Word document text is stored in word/document.xml
                if (entry.name == "word/document.xml") {
                    return parseDocxXml(zipInputStream)
                }
                entry = zipInputStream.nextEntry
            }
        }
        return ""
    }

    private fun parseDocxXml(inputStream: InputStream): String {
        val content = inputStream.bufferedReader().readText()
        val sb = StringBuilder()
        
        // Match <w:t> tags which contain text in OOXML
        val regex = Regex("<w:t[^>]*>(.*?)</w:t>")
        regex.findAll(content).forEach { match ->
            sb.append(match.groupValues[1]).append(" ")
        }
        
        return sb.toString().trim()
    }

    private fun cleanText(text: String): String {
        // Preserve meaningful whitespace but collapse excessive formatting artifacts
        return text.replace(Regex("\\r\\n|\\r|\\n"), "\n")
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\\n\\s*\\n+"), "\n\n")
            .trim()
    }

    private fun getExtensionFromUri(uri: Uri, mimeType: String?): String {
        var extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        
        if (extension == null) {
            val fileName = getFileName(uri)
            extension = fileName?.substringAfterLast('.', "")
        }
        
        return extension ?: ""
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    fun parseCandidateInfo(rawText: String): ParsedCandidateInfo {
        if (rawText.isBlank() || rawText == ERROR_SCANNED_PDF) {
            return ParsedCandidateInfo()
        }

        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

        // 1. Email extraction
        val emailRegex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        val emailMatch = emailRegex.find(rawText)?.value?.trim()

        // 2. Phone extraction
        val phoneRegex = Regex("(?:\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}")
        val phoneMatch = phoneRegex.find(rawText)?.value?.trim()

        // 3. Name extraction heuristic from top header
        var candidateName: String? = null
        val blacklistWords = setOf("resume", "curriculum", "vitae", "cv", "page", "contact", "summary", "profile", "github", "linkedin", "http", "https", "www", "portfolio", "email", "phone")

        for (i in 0 until minOf(6, lines.size)) {
            val line = lines[i]
            val lower = line.lowercase()
            
            // Skip lines with emails, phones, urls, or blacklist words
            if (emailMatch != null && line.contains(emailMatch)) continue
            if (phoneMatch != null && line.contains(phoneMatch)) continue
            if (blacklistWords.any { lower.contains(it) }) continue
            if (line.length > 50 || line.length < 3) continue
            if (line.any { it.isDigit() }) continue

            val words = line.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (words.size in 2..4 && words.all { it.first().isUpperCase() }) {
                candidateName = line
                break
            }
        }

        // 4. Location extraction heuristic
        var candidateLocation: String? = null
        val locationRegex = Regex("([A-Za-z\\s]+),\\s*([A-Za-z\\s]{2,}|[A-Z]{2})(?:\\s+\\d{5})?")
        for (i in 0 until minOf(8, lines.size)) {
            val line = lines[i]
            val locMatch = locationRegex.find(line)
            if (locMatch != null && !line.lowercase().contains("university") && !line.lowercase().contains("college")) {
                candidateLocation = locMatch.value.trim()
                break
            }
        }

        return ParsedCandidateInfo(
            name = candidateName,
            email = emailMatch,
            phone = phoneMatch,
            location = candidateLocation
        )
    }
}

data class ParsedCandidateInfo(
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val location: String? = null
)
