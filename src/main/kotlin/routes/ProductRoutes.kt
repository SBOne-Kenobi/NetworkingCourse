package routes

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import models.ImageStorage
import models.Product
import models.productStorage

fun Application.productRoutes() {
    routing {
        route("/product") {
            productRoute()
            productByIdRoute()
            createProductRoute()
            updateProductRoute()
            removeProductRoute()
            productImageById()
        }
    }
}

fun Route.productRoute() {
    get("/") {
        if (productStorage.isNotEmpty()) {
            call.respond(productStorage)
        } else {
            call.respondText("Products not found", status = HttpStatusCode.NotFound)
        }
    }
}

fun Route.productByIdRoute() {
    get("/{id}") {
        val id = call.parameters["id"] ?: return@get call.respondText(
            "Missing or malformed id",
            status = HttpStatusCode.BadRequest
        )
        val product =
            productStorage.find { it.id == id } ?: return@get call.respondText(
                "No product with id $id",
                status = HttpStatusCode.NotFound
            )
        call.respond(product)
    }
}

fun Route.createProductRoute() {
    post("/") {
        val product = call.receive<Product>()
        productStorage.add(product)
        call.respondText("Product stored correctly", status = HttpStatusCode.Created)
    }
}

fun Route.updateProductRoute() {
    put("/{id}") {
        val id = call.parameters["id"] ?: return@put call.respondText(
            "Missing or malformed id",
            status = HttpStatusCode.BadRequest
        )
        val product = call.receive<Product>()
        val oldProduct = productStorage.find { it.id == id } ?: return@put call.respondText(
            "No product with id $id",
            status = HttpStatusCode.NotFound
        )
        oldProduct.name = product.name
        call.respondText("Product modified correctly", status = HttpStatusCode.Accepted)
    }
}

fun Route.removeProductRoute() {
    delete("/{id}") {
        val id = call.parameters["id"] ?: return@delete call.respondText(
            "Missing or malformed id",
            status = HttpStatusCode.BadRequest
        )
        if (productStorage.removeIf { it.id == id }) {
            call.respondText("Product removed correctly", status = HttpStatusCode.Accepted)
        } else {
            call.respondText("Not found", status = HttpStatusCode.NotFound)
        }
    }
}

fun Route.productImageById() {
    get ("/{id}/image") {
        val id = call.parameters["id"] ?: return@get call.respondText(
            "Missing or malformed id",
            status = HttpStatusCode.BadRequest
        )
        val product = productStorage.find { it.id == id } ?: return@get call.respondText(
            "No product with id $id",
            status = HttpStatusCode.NotFound
        )
        val imageFile = ImageStorage.getImage(product.imageUrl) ?: return@get call.respondText(
            "Image not found",
            status = HttpStatusCode.NoContent
        )
        call.response.header(
            HttpHeaders.ContentDisposition,
            ContentDisposition.Attachment
                .withParameter(ContentDisposition.Parameters.FileName, imageFile.name)
                .toString()
        )
        call.respondFile(imageFile)
    }
}
