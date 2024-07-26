package com.ribbontek.ordermanagement.service.admin

import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.DeleteObjectRequest
import com.amazonaws.services.s3.model.GetObjectRequest
import com.amazonaws.services.s3.model.ListObjectsV2Request
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import com.ribbontek.ordermanagement.exception.BadRequestException
import com.ribbontek.ordermanagement.exception.NotFoundException
import com.ribbontek.ordermanagement.grpc.model.AdminAssetModel
import com.ribbontek.ordermanagement.grpc.model.DeleteAssetCommandModel
import com.ribbontek.ordermanagement.grpc.model.GetAssetCommandModel
import com.ribbontek.ordermanagement.grpc.model.UploadAssetCommandModel
import com.ribbontek.ordermanagement.mapping.toAdminAsset
import com.ribbontek.ordermanagement.mapping.toAssetEntity
import com.ribbontek.ordermanagement.repository.asset.AssetEntity
import com.ribbontek.ordermanagement.repository.asset.AssetRepository
import com.ribbontek.shared.util.logger
import com.ribbontek.shared.util.mapAsync
import jakarta.validation.Valid
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@Validated
interface AdminAssetService {
    fun uploadAssets(
        @Valid cmdModel: List<UploadAssetCommandModel>
    )

    fun getAssets(
        @Valid cmdModel: GetAssetCommandModel
    ): List<AdminAssetModel>

    fun deleteAssets(
        @Valid cmdModel: DeleteAssetCommandModel
    )
}

@Service
class AdminAssetServiceImpl(
    private val assetRepository: AssetRepository,
    private val s3Client: AmazonS3Client,
    @Value("\${com.ribbontek.s3.bucket}") private val s3Bucket: String // TODO: Make this a publicly accessible "asset.ribbontek.com" website
) : AdminAssetService {
    private val log = logger()

    @Transactional
    override fun uploadAssets(cmdModel: List<UploadAssetCommandModel>) {
        cmdModel.validate()
        when (cmdModel.all { it.multipart }) {
            true -> multiFileUpload(cmdModel)
            false -> singleFileUpload(cmdModel)
        }
    }

    @Transactional(readOnly = true)
    override fun getAssets(cmdModel: GetAssetCommandModel): List<AdminAssetModel> {
        return assetRepository.findAllByNameAndAssetReferenceAndAssetType(cmdModel.name, cmdModel.assetReference, cmdModel.assetType)
            .takeIf { it.isNotEmpty() }
            ?.let { assets ->
                when (assets.all { it.multipart }) {
                    true -> assets.toMultipartAdminAsset()
                    false -> assets.toSingleAdminAsset()
                }
            }
            ?: throw NotFoundException("Could not find any assets with name ${cmdModel.name}")
    }

    @Transactional
    override fun deleteAssets(cmdModel: DeleteAssetCommandModel) {
        assetRepository.findAllByNameAndAssetReferenceAndAssetType(cmdModel.name, cmdModel.assetReference, cmdModel.assetType)
            .takeIf { it.isNotEmpty() }
            ?.let { assets ->
                when (assets.all { it.multipart }) {
                    true -> assets.deleteMultipartAdminAsset().deleteEntities()
                    false -> assets.deleteSingleAdminAsset().deleteEntity()
                }
            }
            ?: throw NotFoundException("Could not find any assets with name ${cmdModel.name}")
    }

    private fun List<AssetEntity>.deleteEntities() {
        assetRepository.deleteAll(this)
    }

    private fun AssetEntity.deleteEntity() {
        assetRepository.delete(this)
    }

    private fun List<AssetEntity>.toSingleAdminAsset(): List<AdminAssetModel> {
        if (size > 1) throw BadRequestException("More than one found for single asset upload")
        return first().let { asset ->
            val keyName = "${asset.assetType.name.lowercase()}/${asset.assetReference.lowercase()}/${asset.name.clean()}"
            log.info("Retrieving file $keyName")
            val result = s3Client.getObject(GetObjectRequest(s3Bucket, keyName))
            log.info("Finished retrieving file $keyName")
            listOf(asset.toAdminAsset(decompressByteArray(result.objectContent.readAllBytes())))
        }
    }

    private fun List<AssetEntity>.toMultipartAdminAsset(): List<AdminAssetModel> {
        return runBlocking {
            mapAsync {
                val keyName = "${it.assetType.name.lowercase()}/${it.assetReference.lowercase()}/${it.name.clean()}/${it.partNumber}"
                log.info("Retrieving file $keyName")
                val result = s3Client.getObject(GetObjectRequest(s3Bucket, keyName))
                log.info("Finished retrieving file $keyName")
                it.toAdminAsset(decompressByteArray(result.objectContent.readAllBytes()))
            }
        }.sortedBy { it.partNumber }
    }

    private fun List<AssetEntity>.deleteSingleAdminAsset(): AssetEntity {
        if (size > 1) throw BadRequestException("More than one found for single asset upload")
        with(first()) {
            deleteFromS3()
            return this
        }
    }

    private fun List<AssetEntity>.deleteMultipartAdminAsset(): List<AssetEntity> {
        runBlocking { mapAsync { it.deleteFromS3() } }
        return this
    }

    private fun AssetEntity.deleteFromS3() {
        val keyName = "${assetType.name.lowercase()}/${assetReference.lowercase()}/${name.clean()}"
        log.info("Deleting file $keyName")
        s3Client.deleteObject(DeleteObjectRequest(s3Bucket, keyName))
        log.info("Finished deleting file $keyName")
    }

    private fun List<UploadAssetCommandModel>.validate() {
        if (isEmpty()) {
            throw BadRequestException("Cannot upload empty list of files")
        }
        if (any { it.multipart } && any { !it.multipart }) {
            throw BadRequestException("Cannot mix multipart & non-multipart files in a request")
        }
        if (distinctBy { it.assetReference }.size != 1 ||
            distinctBy { it.assetType }.size != 1 ||
            distinctBy { it.partNumber }.size != size
        ) {
            throw BadRequestException("Cannot upload multipart files with conflicting asset references, types, or part numbers")
        }
    }

    private fun multiFileUpload(cmdModel: List<UploadAssetCommandModel>) {
        if (cmdModel.size == 1) throw BadRequestException("Only one found for multi asset upload")
        log.info("Uploading multipart files - total count ${cmdModel.size}")
        val first = cmdModel.first()
        val directory = "${first.assetType.name.lowercase()}/${first.assetReference.lowercase()}/${first.name.clean()}"
        setUpMultiPartFolders(directory)
        runBlocking {
            cmdModel.mapAsync {
                val keyName = "${it.assetType.name.lowercase()}/${it.assetReference.lowercase()}/${it.name.clean()}/${it.partNumber}"
                log.info("Uploading file $keyName")
                val zippedByteArray = compressByteArray(it.content)
                s3Client.putObject(
                    PutObjectRequest(
                        s3Bucket,
                        keyName,
                        ByteArrayInputStream(zippedByteArray),
                        ObjectMetadata().apply {
                            addUserMetadata("partNumber", it.partNumber.toString())
                            addUserMetadata("multipart", it.multipart.toString())
                            contentLength = zippedByteArray.size.toLong()
                        }
                    )
                )
                log.info("Finished uploading file $keyName")
            }
        }
        log.info("Finished uploading multipart files")
        assetRepository.saveAll(cmdModel.map { it.toAssetEntity() })
    }

    private fun String.clean(): String {
        val validCharacters = ('a'..'z') + ('0'..'9') + '-' + '.'
        val ext = this.trim().split(".").last()
        return this.trim().split(".").dropLast(1).joinToString().lowercase().filter { it in validCharacters }.take(58) + ".$ext"
    }

    private fun singleFileUpload(cmdModel: List<UploadAssetCommandModel>) {
        if (cmdModel.size > 1) throw BadRequestException("Cannot upload more than 1 at a time for single file uploads")
        cmdModel.firstOrNull()?.let {
            val keyName = "${it.assetType.name.lowercase()}/${it.assetReference.lowercase()}/${it.name.clean()}"
            log.info("Uploading file $keyName")
            setUpFolders(keyName)
            val zippedByteArray = compressByteArray(it.content)
            s3Client.putObject(
                PutObjectRequest(
                    s3Bucket,
                    keyName,
                    ByteArrayInputStream(zippedByteArray),
                    ObjectMetadata().apply { contentLength = zippedByteArray.size.toLong() }
                )
            )
            log.info("Finished uploading file $keyName")
            assetRepository.save(it.toAssetEntity())
        }
    }

    private fun setUpMultiPartFolders(directory: String) {
        val folders = directory.split("/")
        var prefix = ""
        folders.forEach { folder ->
            prefix += "$folder/"
            createFolder(prefix)
        }
    }

    private fun setUpFolders(keyName: String) {
        val folders = keyName.split("/").dropLast(1)
        var prefix = ""
        folders.forEach { folder ->
            prefix += "$folder/"
            createFolder(prefix)
        }
    }

    private fun createFolder(folderName: String) {
        log.info("Checking for folder $folderName")
        if (!fileExists(folderName)) {
            log.info("Creating folder $folderName")
            s3Client.putObject(
                PutObjectRequest(
                    s3Bucket,
                    folderName,
                    ByteArrayInputStream(ByteArray(0)),
                    ObjectMetadata().apply { contentLength = 0 }
                )
            )
            log.info("Created folder $folderName")
        }
    }

    private fun compressByteArray(data: ByteArray): ByteArray {
        with(ByteArrayOutputStream()) {
            ZipOutputStream(this).use {
                it.putNextEntry(ZipEntry("compress_data"))
                it.write(data)
                it.closeEntry()
            }
            val compressed = toByteArray()
            log.info("Compressed data from ${data.size} to ${compressed.size}")
            return compressed
        }
    }

    private fun decompressByteArray(data: ByteArray): ByteArray {
        ByteArrayOutputStream().use { byteArrayOutputStream ->
            ZipInputStream(ByteArrayInputStream(data)).use {
                var entry = it.nextEntry
                while (entry != null) {
                    byteArrayOutputStream.writeBytes(it.readAllBytes())
                    entry = it.nextEntry
                }
            }
            val decompressed = byteArrayOutputStream.toByteArray()
            log.info("Decompressed data from ${data.size} to ${decompressed.size}")
            return decompressed
        }
    }

    private fun fileExists(key: String): Boolean {
        return s3Client.listObjectsV2(
            ListObjectsV2Request()
                .withBucketName(s3Bucket)
                .withPrefix(key)
                .withMaxKeys(1)
        ).objectSummaries.any { it.key == key }
    }
}
