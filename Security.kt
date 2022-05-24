package com.example.plugins

import io.ktor.server.auth.*
import io.ktor.util.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.litote.kmongo.KMongo
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.getCollection

fun Application.configureSecurity() {
    
    authentication {
    		basic(name = "myauth1") {
    			realm = "Ktor Server"

				val client = KMongo.createClient() //get com.mongodb.MongoClient new instance
				val database = client.getDatabase(imadDB) //normal java driver usage
				val colUser = database.getCollection<User>() //KMongo extension method


    			validate { credentials ->
					val curName = credentials.name
					val curPass = credentials.password

					val curUserName: User? = colUser.findOne(User::name eq curName)
					val curUserPass: User? = colUser.findOne(User::pass eq curPass)

    				if (curUserName != null && curUserPass != null) {
    					UserIdPrincipal(credentials.name)
    				} else {
    					null
    				}
    			}
    		}
    
    	    form(name = "myauth2") {
    	        userParamName = "user"
    	        passwordParamName = "password"
    	        challenge {
    	        	/**/
    			}
    	    }
    	}

    routing {
        authenticate("myauth1") {
            get("/protected/route/basic") {
                val principal = call.principal<UserIdPrincipal>()!!
                call.respondText("Bienvenido ${principal.name}")
            }
        }
        authenticate("myauth1") {
            get("/protected/route/form") {
                val principal = call.principal<UserIdPrincipal>()!!
                call.respondText("Hello ${principal.name}")
            }
        }
    }
}
