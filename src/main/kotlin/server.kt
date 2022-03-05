import io.ktor.application.*
import io.ktor.features.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.serialization.json.Json
import routes.productRoutes

fun Application.module() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
        })
    }
    install(Routing)
    productRoutes()
}

fun main() {
    embeddedServer(
        Netty, port = 8080,
        host = "127.0.0.1"
    ) {
        module()
    }.start(wait = true)
}