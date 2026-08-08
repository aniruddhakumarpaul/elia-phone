package com.antigravity.smarthub.core.persistence

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Properties

/** Fault hook used by tests to prove that a mutation never crosses a failed durable boundary. */
fun interface PersistenceFailureInjector {
    fun beforeAtomicRename(target: File)
}

object NoPersistenceFailure : PersistenceFailureInjector {
    override fun beforeAtomicRename(target: File) = Unit
}

class DurablePersistenceException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Atomic Properties persistence for app-private state. */
class AtomicPropertiesStore(
    private val target: File,
    private val failureInjector: PersistenceFailureInjector = NoPersistenceFailure
) {
    fun read(): Properties {
        val properties = Properties()
        target.inputStream().use { properties.load(it) }
        return properties
    }

    @Synchronized
    @Throws(DurablePersistenceException::class)
    fun write(properties: Properties) {
        val parent = target.parentFile
        if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.isDirectory) {
            throw DurablePersistenceException("Unable to create persistence directory ${parent.absolutePath}")
        }
        val temp = File(parent ?: target.parentFile, "${target.name}.tmp.${System.nanoTime()}")
        try {
            FileOutputStream(temp).use { output ->
                properties.store(output, "Smart Hub durable state")
                output.fd.sync()
            }
            failureInjector.beforeAtomicRename(target)
            try {
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: Exception) {
                // Some filesystems do not advertise ATOMIC_MOVE. REPLACE_EXISTING is still
                // safe here because the fully-written temp file is the source of the rename.
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (e: DurablePersistenceException) {
            throw e
        } catch (e: Exception) {
            throw DurablePersistenceException("Atomic persistence failed for ${target.absolutePath}", e)
        } finally {
            if (temp.exists()) temp.delete()
        }
    }
}
