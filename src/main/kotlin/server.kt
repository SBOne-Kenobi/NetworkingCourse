import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import routes.authRoutes
import routes.productRoutes

fun Application.module() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
    install(Routing)
    productRoutes()
    authRoutes()
}

fun main() {
    embeddedServer(
        Netty, port = Settings.port,
        host = Settings.host
    ) {
        module()
    }.start(wait = true)
}