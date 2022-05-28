package routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import models.*
import java.net.InetAddress

fun Application.productRoutes() {
    routing {
        route("/product") {
            productRoute()
            productByIdRoute()
            createProductRoute()
            updateProductRoute()
            removeProductRoute()
        }
    }
}

suspend fun PipelineContext<Unit, ApplicationCall>.notify() {
    val ipAddress = withContext(Dispatchers.IO) {
        InetAddress.getByName(call.request.origin.remoteHost)
    }
    val user = UserStorage.getUserByIpAddress(ipAddress) ?: return
    UserNotifier.notify(user)
}

suspend fun PipelineContext<Unit, ApplicationCall>.checkRegisteredAndNotify(token: Token?): Boolean {
    return token?.let {
        UserStorage.getUserByToken(it)?.let { true }
    } ?: run {
        notify()
        false
    }
}

suspend fun PipelineContext<Unit, ApplicationCall>.receiveToken(): Token? {
    return runCatching { call.receive<Token>() }.getOrNull()
}

suspend fun PipelineContext<Unit, ApplicationCall>.receiveProductAndToken(): Pair<Product, Token?> {
    try {
        val (product, tokenString) = call.receive<ProductWithToken>()
        val token = tokenString?.let { Token(it) }
        return product to token
    } catch (e: Exception) {
        println(e)
        throw e
    }
}

fun Route.productRoute() {
    get("/") {
        val token = receiveToken()
        val registered = checkRegisteredAndNotify(token)
        val snapshot = ProductStorage.snapshot(registered)
        if (snapshot.isNotEmpty()) {
            call.respond(snapshot)
        } else {
            call.respondText("Products not found", status = HttpStatusCode.NotFound)
        }
    }
}

fun Route.productByIdRoute() {
    get("/{id}") {
        val token = receiveToken()
        val registered = checkRegisteredAndNotify(token)
        val id = call.parameters["id"] ?: return@get call.respondText(
            "Missing or malformed id",
            status = HttpStatusCode.BadRequest
        )
        val product =
            ProductStorage.getById(id, registered) ?: return@get call.respondText(
                "No product with id $id",
                status = HttpStatusCode.NotFound
            )
        call.respond(product)
    }
}

fun Route.createProductRoute() {
    post("/") {
        val (product, token) = receiveProductAndToken()
        checkRegisteredAndNotify(token)
        ProductStorage.add(product)
        call.respondText("Product stored correctly", status = HttpStatusCode.Created)
    }
}

fun Route.updateProductRoute() {
    put("/{id}") {
        val (product, token) = receiveProductAndToken()
        val registered = checkRegisteredAndNotify(token)
        val id = call.parameters["id"] ?: return@put call.respondText(
            "Missing or malformed id",
            status = HttpStatusCode.BadRequest
        )
        val oldProduct = ProductStorage.getById(id, registered) ?: return@put call.respondText(
            "No product with id $id",
            status = HttpStatusCode.NotFound
        )
        oldProduct.update(product)
        call.respondText("Product modified correctly", status = HttpStatusCode.Accepted)
    }
}

fun Route.removeProductRoute() {
    delete("/{id}") {
        val token = receiveToken()
        val registered = checkRegisteredAndNotify(token)
        val id = call.parameters["id"] ?: return@delete call.respondText(
            "Missing or malformed id",
            status = HttpStatusCode.BadRequest
        )
        if (ProductStorage.deleteById(id, registered)) {
            call.respondText("Product removed correctly", status = HttpStatusCode.Accepted)
        } else {
            call.respondText("Not found", status = HttpStatusCode.NotFound)
        }
    }
}
