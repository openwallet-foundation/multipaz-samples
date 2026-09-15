package org.multipaz.pos.terminal

import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import org.multipaz.rpc.handler.HttpHandler
import org.multipaz.rpc.handler.RpcDispatcherLocal
import org.multipaz.rpc.handler.RpcExceptionMap
import org.multipaz.rpc.handler.RpcPoll
import org.multipaz.rpc.handler.SimpleCipher
import org.multipaz.rpc.server.ClientCheckImpl
import org.multipaz.rpc.server.ClientRegistrationImpl
import org.multipaz.rpc.server.register
import org.multipaz.server.common.ServerEnvironment
import org.multipaz.server.common.runServer
import org.multipaz.server.request.rpc

/**
 * Merchant terminal backend for MultipazWholesalePOS.
 *
 * Run with: `./gradlew :organizations:wholesale_terminal:backend:run`.
 *
 * The RPC endpoint requires **device attestation**: `ServerEnvironment` registers
 * `RpcAuthInspectorAssertion.Default`, and [TerminalPaymentProcessor] mixes it in via
 * `RpcAuthBackendDelegate`. `ClientRegistrationImpl` performs the one-time attestation check against
 * `client_requirements`; `ClientCheckImpl` handles the freshness ping.
 */
class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runServer(
                args = args,
                needAdminPassword = true,
            ) { serverEnvironment ->
                configureRouting(serverEnvironment)
            }
        }
    }
}

fun Application.configureRouting(serverEnvironment: Deferred<ServerEnvironment>) {
    val httpHandler = CoroutineScope(Dispatchers.Default).async {
        val env = serverEnvironment.await()
        val dispatcherBuilder = RpcDispatcherLocal.Builder()
        ClientRegistrationImpl.register(dispatcherBuilder)
        ClientCheckImpl.register(dispatcherBuilder)
        TerminalPaymentProcessor.register(dispatcherBuilder)
        val rpcPoll = env.getInterface(RpcPoll::class)!!
        val localDispatcher = dispatcherBuilder.build(
            env,
            env.getInterface(SimpleCipher::class)!!,
            RpcExceptionMap.Builder().build(),
        )
        HttpHandler(localDispatcher, rpcPoll)
    }
    routing {
        get("/") { call.respondText("Utopia Wholesale terminal backend is running") }
        rpc("/rpc", httpHandler)
    }
}
