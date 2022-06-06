package com.example.plugins

//import com.mongodb.client.FindIterable
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import org.litote.kmongo.*

//Data class for boards
data class Boards(val name:String,val FBQN:String )
//Data class for arduino info
data class Arduino(val address: String, val Protocol:String, val protocol_label:String,val boards: Array<Boards> )

data class User(val name:String, val pass: String, val _id:String?=null)
data class File(val name:String, val fileContent:String, val _id:String?=null, val owner:String?=null)

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

        post("/create/file"){

            val parameters:Parameters = call.receiveParameters()

            val userID = call.parameters["UserID"]
            val fileName = parameters["file"]!!
            val currId = call.request.queryParameters["auth"]

            val client = KMongo.createClient() //get com.mongodb.MongoClient new instance
            val database = client.getDatabase(imadDB) //normal java driver usage
            val colFile = database.getCollection<File>() //KMongo extension method
            val colUser = database.getCollection<User>()

            val currUser : User? = colUser.findOne(User::_id eq currId)
            //val curFile: File? = colFile.findOne(File::name eq fileName)

            if(currUser == null){
                client.close()
                call.respondText("You are not authorized", contentType = ContentType.Text.Plain)
            }

            else {
                val curFile : File? = colFile.findOne(File::name eq fileName)

                if (curFile == null){
                    val insertFile=File(fileName, fileContent = "",owner = userID)
                    colFile.insertOne(insertFile)
                    call.respondText( insertFile._id!! ,contentType=ContentType.Text.Plain )
                    client.close()
                }
                else {
                    client.close()

                    //Sends response to client if user already exists
                    call.respondText("File already exists", contentType = ContentType.Text.Plain)
                }
            }
        }

        put ("/file/{fileID}"){

            val parameters:Parameters = call.receiveParameters()

            val userID = call.parameters["UserID"]
            val fileID = call.parameters["fileID"]
            val currId = call.request.queryParameters["auth"]

            val contents = parameters["fileContents"]!!

            val client = KMongo.createClient() //get com.mongodb.MongoClient new instance
            val database = client.getDatabase(imadDB) //normal java driver usage
            val colFile = database.getCollection<File>() //KMongo extension method
            val colUser = database.getCollection<User>()

            val currUser : User? = colUser.findOne(User::_id eq currId)
            //val curFile: File? = colFile.findOne(File::name eq fileName)

            //Verificamos si el usuario existe
            if(currUser == null){
                client.close()
                call.respondText("You are not authorized", contentType = ContentType.Text.Plain)
            }

            else {
                //Verificamos que el archivo existe
                val curFile: File? = colFile.findOne(File:: _id eq fileID)

                if(curFile == null){

                    call.respondText("Wrong File", contentType = ContentType.Text.Plain)
                }

                else{
                    
                    //Verificamos si el archivo le pertenece al usuario
                    
                    if (curFile.owner == currUser._id) {

                        val editFile = File(fileID.toString(), contents)
                        colFile.updateOne(File::_id eq fileID, set(curFile::fileContent setTo contents))
                        client.close()
                    
                        //Sends user ID (API key) to client
                        call.respondText(editFile._id!!, contentType = ContentType.Text.Plain)
                        call.respondText("OK", contentType = ContentType.Text.Plain)
                    }
                    else{
                        call.respondText("Wrong File", contentType = ContentType.Text.Plain)
                    }
                }
            }
        }

        get ("/files/{fileID}") {

            val parameters:Parameters = call.receiveParameters()

            val userID = call.parameters["UserID"]
            val fileID = call.parameters["fileID"]
            val currId = call.request.queryParameters["auth"]

            val client = KMongo.createClient() //get com.mongodb.MongoClient new instance
            val database = client.getDatabase(imadDB) //normal java driver usage
            val colFile = database.getCollection<File>() //KMongo extension method
            val colUser = database.getCollection<User>()

            val currUser : User? = colUser.findOne(User::_id eq currId)

            //Verificamos que el usuario tiene autorizacion

            if(currUser == null){

                call.respondText("You are not authorized", contentType = ContentType.Text.Plain)
                client.close()

            }
            else{
                
                //Verificamos que el archivo existe
                val curFile: File? = colFile.findOne(File:: _id eq fileID)

                if(curFile == null){
                    call.respondText("Wrong File", contentType = ContentType.Text.Plain)
                    client.close()
                }
                else{
                    
                    //Verificamos que el archivo pertenezca al usuario
                    
                    if (curFile.owner == currUser._id) {
                        call.respondText(curFile.fileContent, contentType = ContentType.Text.Plain)
                        client.close() 
                    }
                    else{
                            call.respondText("Wrong file", contentType = ContentType.Text.Plain)
                            client.close()
                    }
                    
                }
            }
        }

        get("/files/{UserID}"){

            val userId = call.parameters["UserID"]

            val client = KMongo.createClient() //get com.mongodb.MongoClient new instance
            val database = client.getDatabase(imadDB) //normal java driver usage
            val colFile = database.getCollection<File>() //KMongo extension method

            //val curFile: MutableList<File> = colFile.find(File::owner eq userId).toMutableList()
            //val curFile: Iterable<File> = colFile.find(File::owner eq userId).toMutableList()
            val listOfFiles = colFile.find(File:: owner eq userId).toMutableList()

            if (listOfFiles != null){

                call.respondText(curFile.toString(), contentType = ContentType.Text.Plain)

            }

            else{
                
                call.respondText("There arent files owned by this user", contentType = ContentType.Text.Plain)
            }

        }

        get("/flash/file/{fileid}") {

            //Reads auth (API key) from URI
            val currId = call.request.queryParameters["auth"]

            //Reads file ID from URI
            val fileID=call.parameters["fileid"]

            //Auxiliary objects to manage MongoDB database
            val client = KMongo.createClient() //get com.mongodb.MongoClient new instance
            val database = client.getDatabase(imadDB) //normal java driver usage
            val colUser = database.getCollection<User>() //KMongo extension method
            val colFile = database.getCollection<File>() //KMongo extension method

            //Checks if user already exists
            val curUser: User? = colUser.findOne(User::_id eq currId)

            //If user does not exist
            if(curUser==null){
                client.close()
                //Sends response to client
                call.respondText("You are not authorized", contentType = ContentType.Text.Plain)

            }else {

                //User is authorized, then finds requested file
                val curFile = colFile.findOne(File::_id eq fileID)


                //If requested file does not exist
                if (curFile == null) {

                    //Sends response to client
                    call.respondText("Wrong file", contentType = ContentType.Text.Plain)
                    client.close()



                } else {
                    //File exists, then checks if it belongs to current user
                    if (curFile.owner == curUser._id) {

                        val dir = java.io.File(curFile.name.substringBefore('.'));
                        dir.mkdir()

                        val file=java.io.File(dir,curFile.name)

                        val fos = FileOutputStream(file)

                        fos.write(curFile.fileContent.toByteArray())


                        fos.close()

                        var process =

                            ProcessBuilder("arduino-cli", "board", "list", "--format","json").start()
                        var stdout=process.inputStream

                        var isstdout=InputStreamReader(stdout)

                        var brstdout=BufferedReader(isstdout)

                        process.waitFor()

                        var response=brstdout.readText()

                        val obj=Gson().fromJson(response,Array<Arduino>::class.java)

                        val arduino=obj.filter { it ->
                            if(it.boards!=null) {
                                val device= it.boards.filter { it ->

                                    it.name.equals("Arduino Uno")

                                }

                                device!=null

                            }else
                                false
                        }

                        var port="COM1"
                        if(arduino.size!=0)
                            port=arduino[0].address;

                        process =
                            ProcessBuilder("arduino-cli", "compile", "-b", "arduino:avr:uno","-u", "-p",
                                port, curFile.name.substringBefore('.')).start()
                        var errs=process.errorStream

                        var isrerr = InputStreamReader(errs)

                        var brerr=BufferedReader(isrerr)

                        process.waitFor()

                        response=brerr.readText()

                        response =if(response.isEmpty()) "Sketch was successfully flashed\n" else response

                            //Sends response from Arduino CLI
                        call.respondText(response, contentType = ContentType.Text.Plain)

                        client.close()

                    } else {
                        //Even though file exists, it does not belong to current user
                        call.respondText("Wrong file", contentType = ContentType.Text.Plain)
                        client.close()

                    }


                }

            }


        }
        
    }
}
