package com.example.ainotes.data.ai

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.ainotes.data.model.Note
import com.google.mlkit.common.model.DownloadCallback
import com.google.mlkit.common.model.FeatureStatus
import com.google.mlkit.common.model.GenAiException
import com.google.mlkit.genai.summarizer.InputType
import com.google.mlkit.genai.summarizer.Language
import com.google.mlkit.genai.summarizer.OutputType
import com.google.mlkit.genai.summarizer.Summarization
import com.google.mlkit.genai.summarizer.SummarizationRequest
import com.google.mlkit.genai.summarizer.Summarizer
import com.google.mlkit.genai.summarizer.SummarizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object NoteSummarizer {
    private const val TAG = "NoteSummarizer"

    suspend fun summarizeForTitle(context: Context, text: String): String =
        withContext(Dispatchers.IO) {
            val normalized = text.replace(Regex("\\s+"), " ").trim()
            if (normalized.isBlank() || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return@withContext ""

            val summary = summarize(
                context = context,
                text = normalized,
                outputType = OutputType.ONE_BULLET
            )

            return@withContext summary
                .lineSequence()
                .firstOrNull()
                .orEmpty()
                .trimStart('•', '-', '\\t', ' ')
                .take(Note.MAX_TITLE_LENGTH)
        }

    suspend fun summarizeContent(context: Context, text: String): String =
        withContext(Dispatchers.IO) {
            val normalized = text.replace(Regex("\\s+"), " ").trim()
            if (normalized.isBlank() || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return@withContext ""

            return@withContext summarize(
                context = context,
                text = normalized,
                outputType = OutputType.THREE_BULLET
            )
        }

    private suspend fun summarize(
        context: Context,
        text: String,
        outputType: OutputType
    ): String {
        val options = SummarizerOptions.builder(context)
            .setInputType(InputType.ARTICLE)
            .setOutputType(outputType)
            .setLanguage(Language.ENGLISH)
            .setLongInputAutoTruncationEnabled(true)
            .build()

        val summarizer = Summarization.getClient(options)
        try {
            when (summarizer.checkFeatureStatus().await()) {
                FeatureStatus.DOWNLOADABLE -> downloadFeature(summarizer)
                FeatureStatus.UNAVAILABLE -> return ""
                else -> Unit
            }

            val request = SummarizationRequest.builder(text).build()
            val result = summarizer.runInference(request).await()
            val bullets = result.summary
            return bullets.joinToString(" ") { it.trim().trimStart('•', '-', '\\t') }.trim()
        } catch (t: Throwable) {
            Log.w(TAG, "ML Kit summarization failed", t)
            return ""
        } finally {
            try { summarizer.close() } catch (_: Throwable) {}
        }
    }

    private suspend fun downloadFeature(summarizer: Summarizer) {
        suspendCancellableCoroutine { cont ->
            summarizer.downloadFeature(object : DownloadCallback {
                override fun onDownloadStarted(bytesToDownload: Long) { }

                override fun onDownloadFailed(e: GenAiException) {
                    if (cont.isActive) cont.resumeWithException(e)
                }

                override fun onDownloadProgress(totalBytesDownloaded: Long) { }

                override fun onDownloadCompleted() {
                    if (cont.isActive) cont.resume(Unit)
                }
            })
        }
    }
}
