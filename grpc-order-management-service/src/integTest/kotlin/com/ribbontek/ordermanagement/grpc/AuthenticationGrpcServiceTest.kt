package com.ribbontek.ordermanagement.grpc

import com.google.protobuf.Empty
import com.ribbontek.grpccourse.AuthenticationServiceGrpcKt.AuthenticationServiceCoroutineStub
import com.ribbontek.grpccourse.loginUserCommand
import com.ribbontek.grpccourse.refreshTokenCommand
import com.ribbontek.grpccourse.resetPasswordCommand
import com.ribbontek.ordermanagement.context.AbstractIntegTest
import com.ribbontek.ordermanagement.factory.AuthenticationFactory
import com.ribbontek.ordermanagement.factory.AuthenticationFactory.toResetPasswordCommand
import com.ribbontek.ordermanagement.repository.user.UserRepository
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.runBlocking
import net.devh.boot.grpc.client.inject.GrpcClient
import org.apache.commons.lang3.RandomStringUtils
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import kotlin.random.Random

class AuthenticationGrpcServiceTest : AbstractIntegTest() {

    @GrpcClient("clientstub")
    private lateinit var authenticationServiceCoroutineStub: AuthenticationServiceCoroutineStub

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    fun `login user flow - no user`() {
        val resetPasswordResult = assertThrows<StatusException> {
            runBlocking { authenticationServiceCoroutineStub.login(AuthenticationFactory.loginUserCommand()) }
        }
        assertThat(resetPasswordResult.status.code, equalTo(Status.PERMISSION_DENIED.code))
        assertThat(resetPasswordResult.status.description, equalTo("Invalid username or password"))
    }

    @Test
    fun `login user flow - invalid email & no explicit validation on password`() {
        val cmd = AuthenticationFactory.loginUserCommand(email = "asdfv", password = "12345")
        val registerResult = assertThrows<StatusException> {
            runBlocking { authenticationServiceCoroutineStub.login(cmd) }
        }
        assertThat(registerResult.status.code, equalTo(Status.INVALID_ARGUMENT.code))
        assertThat(registerResult.status.description, equalTo("[username]: must be a well-formed email address"))
    }

    @Test
    fun `reset password flow - admin user`() {
        withAdminUser {
            val loginUserCommand = loginUserCommand {
                this.username = this@withAdminUser.email
                this.password = this@withAdminUser.password
            }
            // reset password
            runBlocking { authenticationServiceCoroutineStub.resetPassword(Empty.getDefaultInstance(), authMetadata) }
            // user can no longer login
            val loginResult = assertThrows<StatusException> {
                runBlocking { authenticationServiceCoroutineStub.login(loginUserCommand) }
            }
            assertThat(loginResult.status.code, equalTo(Status.PERMISSION_DENIED.code))
            assertThat(loginResult.status.description, equalTo("Invalid username or password"))
            // reset password again - throws error (user locked)
            val resetPassword2Result = assertThrows<StatusException> {
                runBlocking { authenticationServiceCoroutineStub.resetPassword(Empty.getDefaultInstance(), authMetadata) }
            }
            assertThat(resetPassword2Result.status.code, equalTo(Status.PERMISSION_DENIED.code))
            assertThat(resetPassword2Result.status.description, equalTo("Invalid Authentication"))
            // retrieve user before
            val userEntityResetState = userRepository.findByEmail(this.email)
            assertNotNull(userEntityResetState)
            assertNotNull(userEntityResetState!!.idpUserName)
            assertThat(userEntityResetState.idpStatus, equalTo("DELETED"))
            assertNotNull(userEntityResetState.authCode)
            assertThat(userEntityResetState.loginAttempts, equalTo(1)) // should be one as attempted login above
            assertTrue(userEntityResetState.locked)
            // confirm reset password - invalid code
            val resetCmdError = loginUserCommand.toResetPasswordCommand(List(6) { Random.nextInt(10) }.joinToString(""))
            val confirmResetErrorResult = assertThrows<StatusException> {
                runBlocking { authenticationServiceCoroutineStub.confirmResetPassword(resetCmdError, authMetadata) }
            }
            assertThat(confirmResetErrorResult.status.code, equalTo(Status.PERMISSION_DENIED.code))
            assertThat(confirmResetErrorResult.status.description, equalTo("Invalid Authentication Code"))
            // confirm reset password - valid code
            val resetCmd = loginUserCommand.toResetPasswordCommand(userEntityResetState.authCode!!)
            runBlocking { authenticationServiceCoroutineStub.confirmResetPassword(resetCmd, authMetadata) }
            // retrieve user after
            val userEntityConfirmedState = userRepository.findByEmail(this.email)
            assertNotNull(userEntityConfirmedState)
            assertNotNull(userEntityConfirmedState!!.idpUserName)
            assertThat(userEntityConfirmedState.idpStatus, equalTo("CONFIRMED"))
            assertNull(userEntityConfirmedState.authCode)
            assertThat(userEntityConfirmedState.loginAttempts, equalTo(0))
            assertFalse(userEntityConfirmedState.locked)
            // user can login again
            val loginUserCommand2 = loginUserCommand {
                this.username = resetCmd.email
                this.password = resetCmd.password
            }
            val result = runBlocking { authenticationServiceCoroutineStub.login(loginUserCommand2, authMetadata) }
            assertNotNull(result.refreshToken)
            assertNotNull(result.idToken)
            assertNotNull(result.accessToken)
            assertNotNull(result.tokenType)
        }
    }

    @Test
    fun `reset password flow - standard user`() {
        withStandardUser {
            val loginUserCommand = loginUserCommand {
                this.username = this@withStandardUser.email
                this.password = this@withStandardUser.password
            }
            // reset password
            runBlocking { authenticationServiceCoroutineStub.resetPassword(Empty.getDefaultInstance(), authMetadata) }
            // user can no longer login
            val loginResult = assertThrows<StatusException> {
                runBlocking { authenticationServiceCoroutineStub.login(loginUserCommand) }
            }
            assertThat(loginResult.status.code, equalTo(Status.PERMISSION_DENIED.code))
            assertThat(loginResult.status.description, equalTo("Invalid username or password"))
            // reset password again - throws error (user locked)
            val resetPassword2Result = assertThrows<StatusException> {
                runBlocking { authenticationServiceCoroutineStub.resetPassword(Empty.getDefaultInstance(), authMetadata) }
            }
            assertThat(resetPassword2Result.status.code, equalTo(Status.PERMISSION_DENIED.code))
            assertThat(resetPassword2Result.status.description, equalTo("Invalid Authentication"))
            // retrieve user before
            val userEntityResetState = userRepository.findByEmail(this.email)
            assertNotNull(userEntityResetState)
            assertNotNull(userEntityResetState!!.idpUserName)
            assertThat(userEntityResetState.idpStatus, equalTo("DELETED"))
            assertNotNull(userEntityResetState.authCode)
            assertThat(userEntityResetState.loginAttempts, equalTo(1)) // should be one as attempted login above
            assertTrue(userEntityResetState.locked)
            // confirm reset password - invalid code
            val resetCmdError = loginUserCommand.toResetPasswordCommand(List(6) { Random.nextInt(10) }.joinToString(""))
            val confirmResetErrorResult = assertThrows<StatusException> {
                runBlocking { authenticationServiceCoroutineStub.confirmResetPassword(resetCmdError, authMetadata) }
            }
            assertThat(confirmResetErrorResult.status.code, equalTo(Status.PERMISSION_DENIED.code))
            assertThat(confirmResetErrorResult.status.description, equalTo("Invalid Authentication Code"))
            // confirm reset password - valid code
            val resetCmd = loginUserCommand.toResetPasswordCommand(userEntityResetState.authCode!!)
            runBlocking { authenticationServiceCoroutineStub.confirmResetPassword(resetCmd, authMetadata) }
            // retrieve user before
            val userEntityConfirmedState = userRepository.findByEmail(this.email)
            assertNotNull(userEntityConfirmedState)
            assertNotNull(userEntityConfirmedState!!.idpUserName)
            assertThat(userEntityConfirmedState.idpStatus, equalTo("CONFIRMED"))
            assertNull(userEntityConfirmedState.authCode)
            assertThat(userEntityConfirmedState.loginAttempts, equalTo(0))
            assertFalse(userEntityConfirmedState.locked)
            // user can login again
            val loginUserCommand2 = loginUserCommand {
                this.username = resetCmd.email
                this.password = resetCmd.password
            }
            val result = runBlocking { authenticationServiceCoroutineStub.login(loginUserCommand2, authMetadata) }
            assertNotNull(result.refreshToken)
            assertNotNull(result.idToken)
            assertNotNull(result.accessToken)
            assertNotNull(result.tokenType)
        }
    }

    @Test
    fun `reset password flow - no user`() {
        val resetPasswordResult = assertThrows<StatusException> {
            runBlocking { authenticationServiceCoroutineStub.resetPassword(Empty.getDefaultInstance()) }
        }
        assertThat(resetPasswordResult.status.code, equalTo(Status.PERMISSION_DENIED.code))
        assertThat(resetPasswordResult.status.description, equalTo("Invalid Authentication"))
        val confirmResetPasswordResult = assertThrows<StatusException> {
            runBlocking { authenticationServiceCoroutineStub.confirmResetPassword(resetPasswordCommand { }) }
        }
        assertThat(confirmResetPasswordResult.status.code, equalTo(Status.PERMISSION_DENIED.code))
        assertThat(confirmResetPasswordResult.status.description, equalTo("Invalid Authentication"))
    }

    @Test
    fun `register user flow - invalid email`() {
        val cmd = AuthenticationFactory.registerUserCommand(email = "asdfv")
        val registerResult = assertThrows<StatusException> {
            runBlocking { authenticationServiceCoroutineStub.register(cmd) }
        }
        assertThat(registerResult.status.code, equalTo(Status.INVALID_ARGUMENT.code))
        assertThat(registerResult.status.description, equalTo("[email]: must be a well-formed email address"))
    }

    @Test
    fun `register user flow - invalid email & password`() {
        val cmd = AuthenticationFactory.registerUserCommand(email = "asdfv", password = "12345")
        val registerResult = assertThrows<StatusException> {
            runBlocking { authenticationServiceCoroutineStub.register(cmd) }
        }
        assertThat(registerResult.status.code, equalTo(Status.INVALID_ARGUMENT.code))
        assertThat(
            registerResult.status.description,
            equalTo(
                "[email]: must be a well-formed email address; " +
                    "[password]: must match \"^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^\\^\$*.[\\]{}()\\-\"!@#%&/,><':;|_~`]])\\S{8,99}\$\""
            )
        )
    }

    @Test
    fun `refresh token flow - admin user`() {
        withAdminUser {
            val cmd = refreshTokenCommand {
                this.refreshToken = this@withAdminUser.refreshToken!!
            }
            val result = runBlocking { authenticationServiceCoroutineStub.refreshToken(cmd, authMetadata) }
            assertNotNull(result.refreshToken)
            assertNotNull(result.idToken)
            assertNotNull(result.accessToken)
            assertNotNull(result.tokenType)
        }
    }

    @Test
    fun `refresh token flow - standard user`() {
        withStandardUser {
            val cmd = refreshTokenCommand {
                this.refreshToken = this@withStandardUser.refreshToken!!
            }
            val result = runBlocking { authenticationServiceCoroutineStub.refreshToken(cmd, authMetadata) }
            assertNotNull(result.refreshToken)
            assertNotNull(result.idToken)
            assertNotNull(result.accessToken)
            assertNotNull(result.tokenType)
        }
    }

    @Test
    fun `refresh token flow - auth fail - no user`() {
        val cmd = refreshTokenCommand {
            this.refreshToken = RandomStringUtils.randomAlphabetic(256)
        }
        val result = assertThrows<StatusException> {
            runBlocking { authenticationServiceCoroutineStub.refreshToken(cmd) }
        }
        assertThat(result.status.code, equalTo(Status.PERMISSION_DENIED.code))
        assertThat(result.status.description, equalTo("Invalid Authentication"))
    }
}
