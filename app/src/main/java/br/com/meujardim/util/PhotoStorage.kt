package br.com.meujardim.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** Guarda as fotos na pasta interna do app, para não depender de permissões de galeria. */
object PhotoStorage {

    private fun dir(context: Context): File =
        File(context.filesDir, "photos").apply { if (!exists()) mkdirs() }

    /** Copia a imagem escolhida na galeria para a pasta interna. */
    suspend fun importFromUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val target = File(dir(context), "${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return@runCatching null
            target.absolutePath
        }.getOrNull()
    }

    /** Cria um arquivo vazio para a câmera gravar e devolve o Uri compartilhável. */
    fun newCameraTarget(context: Context): Pair<File, Uri> {
        val file = File(dir(context), "${UUID.randomUUID()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return file to uri
    }

    fun delete(path: String) {
        runCatching { File(path).takeIf { it.exists() }?.delete() }
    }
}
