package com.example.plugins

import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*

fun Application.configureRouting() {

    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        post("/create/user"){
            val parameters:Parameters = call.receiveParameters()

            var name = parameters["user"]!!
            var password = parameters["password"]!!

            println("${call.request.httpMethod.value} request. My name is: $name and my password is $password")
        }

        post("/create/file"){
            val parameters:Parameters = call.receiveParameters()

            var fileName = parameters["file"]!!

            println("${call.request.httpMethod.value} request. My file created is: $fileName")
            call.respondText("file created")
        }

        put ("/file/{fileID}"){

            val parameters:Parameters = call.receiveParameters()
            val fileID = call.parameters["fileid"]

            var fileContents = parameters["fileContents"]!!

            println ("${call.request.httpMethod.value}. Contents of file is $fileContents")
                    }


    }
}
