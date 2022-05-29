package com.example.plugins

import com.mongodb.client.FindIterable
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import org.litote.kmongo.*

data class User(val name:String, val pass: String, val _id:String?=null)
data class File(val name:String, val content:String, val _id:String?=null, val owner:String?=null)

const val imadDB="imad-db-1-2022"

fun Application.configureRouting() {

    routing {
        get("/") {
            call.respondText("pagina principal del servidor")
        }

        post("/create/user"){

            //Reads parameters from POST request
            val parameters: Parameters = call.receiveParameters()

            //Retrieves each argument of POST request to register new user
            val name= parameters["user"]!!
            val pass = parameters["password"]!!

            //Objects to manage MongoDB database
            val client = KMongo.createClient() //get com.mongodb.MongoClient new instance
            val database = client.getDatabase(imadDB) //normal java driver usage
            val colUser = database.getCollection<User>() //KMongo extension method

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

        post("/create/file?auth={UserID}"){

            val parameters:Parameters = call.receiveParameters()

            val userID = call.parameters["UserID"]
            val fileName = parameters["file"]!!

            val client = KMongo.createClient() //get com.mongodb.MongoClient new instance
            val database = client.getDatabase(imadDB) //normal java driver usage
            val colFile = database.getCollection<File>() //KMongo extension method

            val curFile: File? = colFile.findOne(File::name eq fileName)

            if(curFile==null){

                //Creates new User object and inserts it in database
                val insertFile=File(fileName, content = "",owner = userID)
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

        put ("/file/{fileID}?auth={UserID}"){

            val parameters:Parameters = call.receiveParameters()

            val userID = call.parameters["UserID"]
            val fileID = call.parameters["fileID"]

            val contents = parameters["fileContents"]!!

            val client = KMongo.createClient() //get com.mongodb.MongoClient new instance
            val database = client.getDatabase(imadDB) //normal java driver usage
            val colFile = database.getCollection<File>() //KMongo extension method

            val curFile: File? = colFile.findOne(File::_id eq fileID)
            val curUser: File? = colFile.findOne(File::owner eq userID)

            if(curFile != null && curUser != null){

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

        get ("/file/{fileID}?auth={userID}") {

            val userId = call.parameters["userID"]
            val fileID = call.parameters["fileID"]

            val client = KMongo.createClient() //get com.mongodb.MongoClient new instance
            val database = client.getDatabase(imadDB) //normal java driver usage
            val colFile = database.getCollection<File>() //KMongo extension method

            val curFile: File? = colFile.findOne(File::_id eq fileID)
            val curUser: File? = colFile.findOne(File::owner eq userId)

            if(curFile != null && curUser != null){

                call.respondText(curFile.content, contentType = ContentType.Text.Plain)
                client.close()

            }else
            {
                client.close()

                //Sends response to client if user already exists
                call.respondText("Wrong FileID", contentType = ContentType.Text.Plain)
            }

        }

        get("/files?auth={UserID}"){

            val userId = call.parameters["UserID"]

            val client = KMongo.createClient() //get com.mongodb.MongoClient new instance
            val database = client.getDatabase(imadDB) //normal java driver usage
            val colFile = database.getCollection<File>() //KMongo extension method

            //val curFile: MutableList<File> = colFile.find(File::owner eq userId).toMutableList()
            val curFile: Iterable<File> = colFile.find(File::owner eq userId).toMutableList()

            if (curFile != null){

                call.respondText(curFile.toString(), contentType = ContentType.Text.Plain)

            }

            else{
                call.respondText("There arent files owned by this user", contentType = ContentType.Text.Plain)
            }

        }
        
    }
}
