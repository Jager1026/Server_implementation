package com.example.plugins

import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import org.litote.kmongo.*

data class User(val name:String, val pass: String, val _id:String?=null)
data class File(val name:String, val content:String, val _id:String?=null)

val imadDB="imad-db-1-2022"

fun Application.configureRouting() {

    routing {
        get("/") {
            call.respondText("Pagina principal del servidor")
        }

        post("/create/user"){
            //Reads parameters from POST request
            val parameters: Parameters = call.receiveParameters()

            //Retrieves each argument of POST request to register new user
            var name= parameters["user"]!!
            var pass = parameters["password"]!!

            //Objects to manage MongoDB database
            var client = KMongo.createClient() //get com.mongodb.MongoClient new instance
            var database = client.getDatabase(imadDB) //normal java driver usage
            var colUser = database.getCollection<User>() //KMongo extension method

            //Checks if user already exists
            val curUser: User? = colUser.findOne(User::name eq name)

            //If user does not exist
            if(curUser==null){

                //Creates new User object and inserts it in database
                val insertUser=User(name,pass)
                colUser.insertOne(insertUser)
                client.close()

                //Sends user ID (API key) to client
                call.respondText(insertUser._id!!, contentType = ContentType.Text.Plain)
            }else
            {
                client.close()

                //Sends response to client if user already exists
                call.respondText("User already exists", contentType = ContentType.Text.Plain)
            }
        }

        post("/create/file"){

            val parameters:Parameters = call.receiveParameters()

            var fileName = parameters["file"]!!

            var client = KMongo.createClient() //get com.mongodb.MongoClient new instance
            var database = client.getDatabase(imadDB) //normal java driver usage
            var colFile = database.getCollection<File>() //KMongo extension method

            val curFile: File? = colFile.findOne(File::name eq fileName)

            if(curFile==null){

                //Creates new User object and inserts it in database
                val insertFile=File(fileName, content = "empty")
                colFile.insertOne(insertFile)
                client.close()

                //Sends user ID (API key) to client
                call.respondText(insertFile._id!!, contentType = ContentType.Text.Plain)
            }else
            {
                client.close()

                //Sends response to client if user already exists
                call.respondText("File already exists", contentType = ContentType.Text.Plain)
            }

        }

        put ("/file/{fileID}"){

            val parameters:Parameters = call.receiveParameters()

            val fileID = call.parameters["fileid"]

            var contents = parameters["fileContents"]!!

            var client = KMongo.createClient() //get com.mongodb.MongoClient new instance
            var database = client.getDatabase(imadDB) //normal java driver usage
            var colFile = database.getCollection<File>() //KMongo extension method

            val curFile: File? = colFile.findOne(File::_id eq fileID)

            if(curFile != null){

                //Creates new File object
                val editFile=File(fileID.toString(), contents)
                colFile.updateOne(File::_id eq fileID, set (curFile::content setTo contents))
                client.close()

                //Sends user ID (API key) to client
                call.respondText(editFile._id!!, contentType = ContentType.Text.Plain)
            }else
            {
                client.close()

                //Sends response to client if user already exists
                call.respondText("Wrong File", contentType = ContentType.Text.Plain)
            }
        }


    }
}
