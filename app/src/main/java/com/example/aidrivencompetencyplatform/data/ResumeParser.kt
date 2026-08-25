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
}
