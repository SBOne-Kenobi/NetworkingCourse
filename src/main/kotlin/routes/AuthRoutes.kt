package routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import models.User
import models.UserStorage
import java.net.InetAddress

fun Application.authRoutes() {
    routing {
        route("/auth") {
            registerUser()
            signInUser()
        }
    }
}

fun Route.registerUser() {
    post("/RegisterUser") {
        val newUser = call.receive<User>()
        newUser.ipAddress = withContext(Dispatchers.IO) {
            InetAddress.getByName(call.request.origin.remoteHost)
        }
        if (UserStorage.registerUser(newUser)) {
            call.respondText("User registered successful", status = HttpStatusCode.Created)
        } else {
            call.respondText("User's already registered", status = HttpStatusCode.BadRequest)
        }
    }
}

fun Route.signInUser() {
    get("/SignIn") {
        val user = call.receive<User>()
        if (UserStorage.checkPassword(user)) {
            val token = UserStorage.createNewToken(user.email).token
            call.respondText("Sign in successful with token $token", status = HttpStatusCode.OK)
        } else {
            call.respondText("Wrong password or email", status = HttpStatusCode.BadRequest)
        }
    }
}
