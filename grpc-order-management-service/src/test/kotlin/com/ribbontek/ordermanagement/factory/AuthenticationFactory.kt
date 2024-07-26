package com.ribbontek.ordermanagement.factory

import com.ribbontek.grpccourse.LoginUserCommand
import com.ribbontek.grpccourse.RegisterUserCommand
import com.ribbontek.grpccourse.ResetPasswordCommand
import com.ribbontek.grpccourse.loginUserCommand
import com.ribbontek.grpccourse.registerUserCommand
import com.ribbontek.grpccourse.resetPasswordCommand
import com.ribbontek.ordermanagement.util.FakerUtil
import java.util.UUID

object AuthenticationFactory {
    fun registerUserCommand(
        email: String? = null,
        password: String? = null
    ): RegisterUserCommand {
        return registerUserCommand {
            this.email = email ?: "ribbontek+${UUID.randomUUID()}@gmail.com"
            this.password = password ?: FakerUtil.password()
            firstName = FakerUtil.alphanumeric(255)
            lastName = FakerUtil.alphanumeric(255)
        }
    }

    fun loginUserCommand(
        email: String? = null,
        password: String? = null
    ): LoginUserCommand {
        return loginUserCommand {
            this.username = email ?: "ribbontek+${UUID.randomUUID()}@gmail.com"
            this.password = password ?: FakerUtil.password()
        }
    }

    fun RegisterUserCommand.toLoginUserCommand(): LoginUserCommand =
        let {
            return loginUserCommand {
                this.username = it.email
                this.password = it.password
            }
        }

    fun LoginUserCommand.toResetPasswordCommand(code: String): ResetPasswordCommand =
        let {
            return resetPasswordCommand {
                this.email = it.username
                this.password = FakerUtil.password()
                this.code = code
            }
        }
}
