package com.ribbontek.ordermanagement.grpc

import com.amazonaws.services.s3.AmazonS3Client
import com.google.protobuf.Empty
import com.ribbontek.grpccourse.admin.AdminAsset
import com.ribbontek.grpccourse.admin.AdminAssetServiceGrpcKt.AdminAssetServiceCoroutineStub
import com.ribbontek.grpccourse.admin.AssetType.GLOBAL
import com.ribbontek.grpccourse.admin.AssetType.PRODUCT
import com.ribbontek.grpccourse.admin.DeleteAssetCommand
import com.ribbontek.grpccourse.admin.GetAssetCommand
import com.ribbontek.grpccourse.admin.UploadAssetCommand
import com.ribbontek.grpccourse.admin.copy
import com.ribbontek.grpccourse.admin.deleteAssetCommand
import com.ribbontek.grpccourse.admin.getAssetCommand
import com.ribbontek.ordermanagement.context.AbstractIntegTest
import com.ribbontek.ordermanagement.factory.UploadAssetCommandFactory
import com.ribbontek.ordermanagement.mapping.toAssetTypeEnum
import com.ribbontek.ordermanagement.repository.asset.AssetRepository
import com.ribbontek.shared.result.onFailure
import com.ribbontek.shared.result.onSuccess
import com.ribbontek.shared.result.tryRun
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import net.devh.boot.grpc.client.inject.GrpcClient
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

class AdminAssetGrpcServiceTest : AbstractIntegTest() {
    @GrpcClient("clientstub")
    private lateinit var adminAssetServiceCoroutineStub: AdminAssetServiceCoroutineStub

    @Autowired
    private lateinit var assetRepository: AssetRepository

    @Autowired
    private lateinit var s3Client: AmazonS3Client

    @Value("\${com.ribbontek.s3.bucket}")
    private lateinit var s3Bucket: String
    private val videoFile: ByteArray by lazy {
        "cat_shocked.mp4".readAllBytesFromClassPathResource()
    }
    private val gifFile: ByteArray by lazy {
        "deepracer_evo_spin.gif".readAllBytesFromClassPathResource()
    }
    private val jpegFile: ByteArray by lazy {
        "monkeymart.jpeg".readAllBytesFromClassPathResource()
    }
    private val pngFile: ByteArray by lazy {
        "postgres_logo.png".readAllBytesFromClassPathResource()
    }

    @AfterAll
    fun afterAll() {
        assetRepository.deleteAll()
        // delete everything from test bucket
        tryRun {
            s3Client.listObjectsV2(s3Bucket).objectSummaries.forEach {
                s3Client.deleteObject(s3Bucket, it.key)
            }
        }.onFailure {
            log.error("Failed to delete objects from $s3Bucket", it)
        }.onSuccess {
            log.info("Successfully deleted objects from $s3Bucket")
        }
    }

    @Test
    fun `upload & get asset single file - PNG - admin user - success`() {
        withAdminUser {
            log.info("Testing PNG Single File Upload with length ${pngFile.size}")
            val asset = UploadAssetCommandFactory.create(
                "postgres_logo.png",
                pngFile,
                PRODUCT,
                UUID.randomUUID().toString()
            )
            val uploadResult = runBlocking {
                adminAssetServiceCoroutineStub.uploadAsset(flowOf(asset), authMetadata)
            }
            assertThat(uploadResult, equalTo(Empty.getDefaultInstance()))
            val result = runBlocking {
                adminAssetServiceCoroutineStub.getAsset(asset.toGetAssetCommand(), authMetadata).singleOrNull()
            }
            assertNotNull(result)
            assertThat(result!!.name, equalTo(asset.name))
            assertThat(result.assetType, equalTo(asset.assetType))
            assertThat(result.assetReference, equalTo(asset.assetReference))
            assertFalse(result.multipart)
            assertThat(result.partNumber, equalTo(1))
            assertThat(result.totalNumber, equalTo(1))
            assertThat(result.content.size(), equalTo(asset.content.size()))
            saveToBuildDir("${result.assetType.name}/${result.assetReference}/${result.name}", result.content.toByteArray())
        }
    }

    @Test
    fun `upload & get asset single file - GIF - admin user - success`() {
        withAdminUser {
            log.info("Testing GIF Single File Upload with length ${gifFile.size}")
            val asset = UploadAssetCommandFactory.create(
                "deepracer_evo_spin.gif",
                gifFile,
                PRODUCT,
                UUID.randomUUID().toString()
            )
            val uploadResult = runBlocking {
                adminAssetServiceCoroutineStub.uploadAsset(flowOf(asset), authMetadata)
            }
            assertThat(uploadResult, equalTo(Empty.getDefaultInstance()))
            val result = runBlocking {
                adminAssetServiceCoroutineStub.getAsset(asset.toGetAssetCommand(), authMetadata).singleOrNull()
            }
            assertNotNull(result)
            assertThat(result!!.name, equalTo(asset.name))
            assertThat(result.assetType, equalTo(asset.assetType))
            assertThat(result.assetReference, equalTo(asset.assetReference))
            assertFalse(result.multipart)
            assertThat(result.partNumber, equalTo(1))
            assertThat(result.totalNumber, equalTo(1))
            assertThat(result.content.size(), equalTo(asset.content.size()))
            saveToBuildDir("${result.assetType.name}/${result.assetReference}/${result.name}", result.content.toByteArray())
        }
    }

    @Test
    fun `upload & get asset single file - JPEG - admin user - success`() {
        withAdminUser {
            log.info("Testing JPEG Single File Upload with length ${jpegFile.size}")
            val asset = UploadAssetCommandFactory.create(
                "monkeymart.jpeg",
                jpegFile,
                PRODUCT,
                UUID.randomUUID().toString()
            )
            val uploadResult = runBlocking {
                adminAssetServiceCoroutineStub.uploadAsset(flowOf(asset), authMetadata)
            }
            assertThat(uploadResult, equalTo(Empty.getDefaultInstance()))
            val result = runBlocking {
                adminAssetServiceCoroutineStub.getAsset(asset.toGetAssetCommand(), authMetadata).singleOrNull()
            }
            assertNotNull(result)
            assertThat(result!!.name, equalTo(asset.name))
            assertThat(result.assetType, equalTo(asset.assetType))
            assertThat(result.assetReference, equalTo(asset.assetReference))
            assertFalse(result.multipart)
            assertThat(result.partNumber, equalTo(1))
            assertThat(result.totalNumber, equalTo(1))
            assertThat(result.content.size(), equalTo(asset.content.size()))
            saveToBuildDir("${result.assetType.name}/${result.assetReference}/${result.name}", result.content.toByteArray())
        }
    }

    @Test
    fun `upload & get asset single file - standard user - fail`() {
        withStandardUser {
            val asset = UploadAssetCommandFactory.create(
                "postgres_logo.png",
                pngFile,
                PRODUCT,
                UUID.randomUUID().toString()
            )
            val result = assertThrows<StatusException> {
                runBlocking { adminAssetServiceCoroutineStub.uploadAsset(flowOf(asset), authMetadata) }
            }
            assertThat(result.status.code, equalTo(Status.PERMISSION_DENIED.code))
            assertThat(result.status.description, equalTo("Invalid Authentication"))
            val results = assetRepository.findAllByNameAndAssetReferenceAndAssetType(
                asset.name,
                asset.assetReference,
                asset.assetType.toAssetTypeEnum()
            )
            assertTrue(results.isEmpty())
            val getResult = assertThrows<StatusException> {
                runBlocking { adminAssetServiceCoroutineStub.getAsset(asset.toGetAssetCommand(), authMetadata).singleOrNull() }
            }
            assertThat(getResult.status.code, equalTo(Status.PERMISSION_DENIED.code))
            assertThat(getResult.status.description, equalTo("Invalid Authentication"))
        }
    }

    @Test
    fun `upload & get asset single file - no user - fail`() {
        val asset = UploadAssetCommandFactory.create(
            "postgres_logo.png",
            pngFile,
            PRODUCT,
            UUID.randomUUID().toString()
        )
        val result = assertThrows<StatusException> {
            runBlocking { adminAssetServiceCoroutineStub.uploadAsset(flowOf(asset)) }
        }
        assertThat(result.status.code, equalTo(Status.PERMISSION_DENIED.code))
        assertThat(result.status.description, equalTo("Invalid Authentication"))
        val results = assetRepository.findAllByNameAndAssetReferenceAndAssetType(
            asset.name,
            asset.assetReference,
            asset.assetType.toAssetTypeEnum()
        )
        assertTrue(results.isEmpty())
        val getResult = assertThrows<StatusException> {
            runBlocking { adminAssetServiceCoroutineStub.getAsset(asset.toGetAssetCommand()).singleOrNull() }
        }
        assertThat(getResult.status.code, equalTo(Status.PERMISSION_DENIED.code))
        assertThat(getResult.status.description, equalTo("Invalid Authentication"))
    }

    @Test
    fun `upload & get asset multi file - admin user - validation failures`() {
        withAdminUser {
            val asset = UploadAssetCommandFactory.create(
                "cat_shocked.mp4",
                videoFile,
                PRODUCT,
                UUID.randomUUID().toString()
            )
            val uploadResult = assertThrows<StatusException> {
                runBlocking { adminAssetServiceCoroutineStub.uploadAsset(flowOf(asset), authMetadata) }
            }
            assertThat(uploadResult.status.code, equalTo(Status.INVALID_ARGUMENT.code))
            assertThat(uploadResult.status.description, containsString("size must be between 1 and 5242880"))
            val asset2 = UploadAssetCommandFactory.create(
                name = "deepracer_evo_spin.gif",
                content = gifFile,
                assetType = PRODUCT,
                assetReference = UUID.randomUUID().toString()
            )
            val asset3 = UploadAssetCommandFactory.create(
                name = "monkeymart.jpeg",
                content = jpegFile,
                assetType = GLOBAL,
                assetReference = UUID.randomUUID().toString()
            )
            val conflictResult = assertThrows<StatusException> {
                runBlocking { adminAssetServiceCoroutineStub.uploadAsset(flowOf(asset2, asset3), authMetadata) }
            }
            assertThat(conflictResult.status.code, equalTo(Status.FAILED_PRECONDITION.code))
            assertThat(
                conflictResult.status.description,
                containsString("Cannot upload multipart files with conflicting asset references, types, or part numbers")
            )
            val multipartAsset2 = asset2.copy {
                multipart = true
            }
            val multipartMixingResult = assertThrows<StatusException> {
                runBlocking { adminAssetServiceCoroutineStub.uploadAsset(flowOf(multipartAsset2, asset3), authMetadata) }
            }
            assertThat(multipartMixingResult.status.code, equalTo(Status.FAILED_PRECONDITION.code))
            assertThat(
                multipartMixingResult.status.description,
                containsString("Cannot mix multipart & non-multipart files in a request")
            )
            val emptyResult = assertThrows<StatusException> {
                runBlocking { adminAssetServiceCoroutineStub.uploadAsset(emptyFlow(), authMetadata) }
            }
            assertThat(emptyResult.status.code, equalTo(Status.FAILED_PRECONDITION.code))
            assertThat(emptyResult.status.description, containsString("Cannot upload empty list of files"))
            val assetReference = UUID.randomUUID().toString()
            val jpegFileChunks = jpegFile.toList().chunked(1024 * 1024) // 1MB Uploads
            val assets = jpegFileChunks.mapIndexed { index, bytes ->
                UploadAssetCommandFactory.create(
                    name = "monkeymart.jpeg",
                    content = bytes.toByteArray(),
                    assetType = PRODUCT,
                    assetReference = assetReference,
                    multipart = true,
                    partNumber = index + 1,
                    totalNumber = jpegFileChunks.size
                )
            }.asFlow()
            val onlyOneForMultiErrorResult = assertThrows<StatusException> {
                runBlocking { adminAssetServiceCoroutineStub.uploadAsset(assets, authMetadata) }
            }
            assertThat(onlyOneForMultiErrorResult.status.code, equalTo(Status.FAILED_PRECONDITION.code))
            assertThat(onlyOneForMultiErrorResult.status.description, containsString("Only one found for multi asset upload"))
        }
    }

    @Test
    fun `upload & get asset multi file - MP4 video file at 1MB - admin user - success`() {
        withAdminUser {
            log.info("Testing MP4 Multi File Upload with length ${videoFile.size}")
            val assetReference = UUID.randomUUID().toString()
            val videoFileChunks = videoFile.toList().chunked(1024 * 1024) // 1MB Uploads
            val assets = videoFileChunks.mapIndexed { index, bytes ->
                UploadAssetCommandFactory.create(
                    name = "cat_shocked.mp4",
                    content = bytes.toByteArray(),
                    assetType = PRODUCT,
                    assetReference = assetReference,
                    multipart = true,
                    partNumber = index + 1,
                    totalNumber = videoFileChunks.size
                )
            }.asFlow()
            val uploadResult = runBlocking {
                adminAssetServiceCoroutineStub.uploadAsset(assets, authMetadata)
            }
            assertThat(uploadResult, equalTo(Empty.getDefaultInstance()))
            val results = runBlocking {
                adminAssetServiceCoroutineStub.getAsset(assets.first().toGetAssetCommand(), authMetadata).toList()
            }
            assertTrue(results.isNotEmpty())
            results.validateAndSaveMultipartFileResults(assetReference, videoFileChunks)
        }
    }

    @Test
    fun `upload & get asset multi file - MP4 video file at 5MB - admin user - success`() {
        withAdminUser {
            log.info("Testing MP4 Multi File Upload with length ${videoFile.size}")
            val assetReference = UUID.randomUUID().toString()
            val videoFileChunks = videoFile.toList().chunked(5 * 1024 * 1024) // 5MB Uploads
            val assets = videoFileChunks.mapIndexed { index, bytes ->
                UploadAssetCommandFactory.create(
                    name = "cat_shocked.mp4",
                    content = bytes.toByteArray(),
                    assetType = PRODUCT,
                    assetReference = assetReference,
                    multipart = true,
                    partNumber = index + 1,
                    totalNumber = videoFileChunks.size
                )
            }.asFlow()
            val uploadResult = runBlocking {
                adminAssetServiceCoroutineStub.uploadAsset(assets, authMetadata)
            }
            assertThat(uploadResult, equalTo(Empty.getDefaultInstance()))
            // note explicit configuration of client stub here to retrieve >4mb responses
            val results = runBlocking {
                adminAssetServiceCoroutineStub.withMaxInboundMessageSize(Int.MAX_VALUE)
                    .getAsset(assets.first().toGetAssetCommand(), authMetadata).toList()
            }
            assertTrue(results.isNotEmpty())
            results.validateAndSaveMultipartFileResults(assetReference, videoFileChunks)
        }
    }

    @Test
    fun `upload & get asset multi file - standard user - fail`() {
        withStandardUser {
            val assetReference = UUID.randomUUID().toString()
            val videoFileChunks = videoFile.toList().chunked(1024 * 1024) // 1MB Uploads
            val assets = videoFileChunks.mapIndexed { index, bytes ->
                UploadAssetCommandFactory.create(
                    name = "cat_shocked.mp4",
                    content = bytes.toByteArray(),
                    assetType = PRODUCT,
                    assetReference = assetReference,
                    multipart = true,
                    partNumber = index + 1,
                    totalNumber = videoFileChunks.size
                )
            }.asFlow()
            val result = assertThrows<StatusException> {
                runBlocking { adminAssetServiceCoroutineStub.uploadAsset(assets, authMetadata) }
            }
            assertThat(result.status.code, equalTo(Status.PERMISSION_DENIED.code))
            assertThat(result.status.description, equalTo("Invalid Authentication"))
            val results = assetRepository.findAllByNameAndAssetReferenceAndAssetType(
                name = "cat_shocked.mp4",
                assetReference = assetReference,
                assetType = PRODUCT.toAssetTypeEnum()
            )
            assertTrue(results.isEmpty())
            val getResult = assertThrows<StatusException> {
                runBlocking { adminAssetServiceCoroutineStub.getAsset(assets.first().toGetAssetCommand(), authMetadata).toList() }
            }
            assertThat(getResult.status.code, equalTo(Status.PERMISSION_DENIED.code))
            assertThat(getResult.status.description, equalTo("Invalid Authentication"))
        }
    }

    @Test
    fun `upload & get asset multi file - no user - fail`() {
        val assetReference = UUID.randomUUID().toString()
        val videoFileChunks = videoFile.toList().chunked(1024 * 1024) // 1MB Uploads
        val assets = videoFileChunks.mapIndexed { index, bytes ->
            UploadAssetCommandFactory.create(
                name = "cat_shocked.mp4",
                content = bytes.toByteArray(),
                assetType = PRODUCT,
                assetReference = assetReference,
                multipart = true,
                partNumber = index + 1,
                totalNumber = videoFileChunks.size
            )
        }.asFlow()
        val result = assertThrows<StatusException> {
            runBlocking { adminAssetServiceCoroutineStub.uploadAsset(assets) }
        }
        assertThat(result.status.code, equalTo(Status.PERMISSION_DENIED.code))
        assertThat(result.status.description, equalTo("Invalid Authentication"))
        val results = assetRepository.findAllByNameAndAssetReferenceAndAssetType(
            name = "cat_shocked.mp4",
            assetReference = assetReference,
            assetType = PRODUCT.toAssetTypeEnum()
        )
        assertTrue(results.isEmpty())
        val getResult = assertThrows<StatusException> {
            runBlocking { adminAssetServiceCoroutineStub.getAsset(assets.first().toGetAssetCommand()).toList() }
        }
        assertThat(getResult.status.code, equalTo(Status.PERMISSION_DENIED.code))
        assertThat(getResult.status.description, equalTo("Invalid Authentication"))
    }

    @Test
    fun `upload & delete asset single file - JPEG - admin user - success`() {
        withAdminUser {
            val asset = UploadAssetCommandFactory.create(
                "monkeymart.jpeg",
                jpegFile,
                PRODUCT,
                UUID.randomUUID().toString()
            )
            val uploadResult = runBlocking {
                adminAssetServiceCoroutineStub.uploadAsset(flowOf(asset), authMetadata)
            }
            assertThat(uploadResult, equalTo(Empty.getDefaultInstance()))
            runBlocking {
                adminAssetServiceCoroutineStub.deleteAsset(asset.toDeleteAssetCommand(), authMetadata)
            }
            val entities = assetRepository.findAllByNameAndAssetReferenceAndAssetType(
                asset.name,
                asset.assetReference,
                asset.assetType.toAssetTypeEnum()
            )
            assertTrue(entities.isEmpty())
        }
    }

    @Test
    fun `delete asset single file - file doesn't exist - admin user - failure`() {
        withAdminUser {
            val asset = UploadAssetCommandFactory.create(
                "postgres_logo.png",
                pngFile,
                PRODUCT,
                UUID.randomUUID().toString()
            )
            val result = assertThrows<StatusException> {
                runBlocking { adminAssetServiceCoroutineStub.deleteAsset(asset.toDeleteAssetCommand(), authMetadata) }
            }
            assertThat(result.status.code, equalTo(Status.NOT_FOUND.code))
            assertThat(result.status.description, equalTo("Could not find any assets with name ${asset.name}"))
            val entities = assetRepository.findAllByNameAndAssetReferenceAndAssetType(
                asset.name,
                asset.assetReference,
                asset.assetType.toAssetTypeEnum()
            )
            assertTrue(entities.isEmpty())
        }
    }

    @Test
    fun `upload & delete asset single file - standard user - fail`() {
        withStandardUser {
            val asset = UploadAssetCommandFactory.create(
                "postgres_logo.png",
                pngFile,
                PRODUCT,
                UUID.randomUUID().toString()
            )
            val result = assertThrows<StatusException> {
                runBlocking { adminAssetServiceCoroutineStub.uploadAsset(flowOf(asset), authMetadata) }
            }
            assertThat(result.status.code, equalTo(Status.PERMISSION_DENIED.code))
            assertThat(result.status.description, equalTo("Invalid Authentication"))
            val results = assetRepository.findAllByNameAndAssetReferenceAndAssetType(
                asset.name,
                asset.assetReference,
                asset.assetType.toAssetTypeEnum()
            )
            assertTrue(results.isEmpty())
            val getResult = assertThrows<StatusException> {
                runBlocking { adminAssetServiceCoroutineStub.deleteAsset(asset.toDeleteAssetCommand(), authMetadata) }
            }
            assertThat(getResult.status.code, equalTo(Status.PERMISSION_DENIED.code))
            assertThat(getResult.status.description, equalTo("Invalid Authentication"))
        }
    }

    @Test
    fun `upload & delete asset single file - no user - fail`() {
        val asset = UploadAssetCommandFactory.create(
            "postgres_logo.png",
            pngFile,
            PRODUCT,
            UUID.randomUUID().toString()
        )
        val result = assertThrows<StatusException> {
            runBlocking { adminAssetServiceCoroutineStub.uploadAsset(flowOf(asset)) }
        }
        assertThat(result.status.code, equalTo(Status.PERMISSION_DENIED.code))
        assertThat(result.status.description, equalTo("Invalid Authentication"))
        val results = assetRepository.findAllByNameAndAssetReferenceAndAssetType(
            asset.name,
            asset.assetReference,
            asset.assetType.toAssetTypeEnum()
        )
        assertTrue(results.isEmpty())
        val getResult = assertThrows<StatusException> {
            runBlocking { adminAssetServiceCoroutineStub.deleteAsset(asset.toDeleteAssetCommand()) }
        }
        assertThat(getResult.status.code, equalTo(Status.PERMISSION_DENIED.code))
        assertThat(getResult.status.description, equalTo("Invalid Authentication"))
    }

    private fun List<AdminAsset>.validateAndSaveMultipartFileResults(assetReference: String, videoFileChunks: List<List<Byte>>) {
        val byteArrayOutputStream = ByteArrayOutputStream()
        forEachIndexed { index, result ->
            assertThat(result.name, equalTo("cat_shocked.mp4"))
            assertThat(result.assetType, equalTo(PRODUCT))
            assertThat(result.assetReference, equalTo(assetReference))
            assertTrue(result.multipart)
            assertThat(result.partNumber, equalTo(index + 1))
            assertThat(result.totalNumber, equalTo(videoFileChunks.size))
            assertThat(result.content.size(), equalTo(videoFileChunks[index].toByteArray().size))
            byteArrayOutputStream.writeBytes(result.content.toByteArray())
        }

        saveToBuildDir("${PRODUCT.name}/$assetReference/cat_shocked.mp4", byteArrayOutputStream.toByteArray())
    }

    private fun saveToBuildDir(filePath: String, byteArray: ByteArray) {
        val file = File("build/$filePath")
        file.parentFile.mkdirs()
        file.writeBytes(byteArray)
        log.info("Saved MP4 Multi File Upload to ${file.path}")
    }

    private fun UploadAssetCommand.toDeleteAssetCommand(): DeleteAssetCommand {
        val asset = this
        return deleteAssetCommand {
            name = asset.name
            assetType = asset.assetType
            assetReference = asset.assetReference
        }
    }

    private fun UploadAssetCommand.toGetAssetCommand(): GetAssetCommand {
        val asset = this
        return getAssetCommand {
            name = asset.name
            assetType = asset.assetType
            assetReference = asset.assetReference
        }
    }

    private fun String.readAllBytesFromClassPathResource(): ByteArray = ClassPathResource(this).inputStream.use { it.readAllBytes() }
}
