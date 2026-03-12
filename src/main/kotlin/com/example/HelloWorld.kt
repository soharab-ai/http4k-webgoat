package com.example

import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.TEMPORARY_REDIRECT
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.SunHttp
import org.http4k.server.asServer

import kotlinx.html.*
import kotlinx.html.dom.*
import kotlinx.html.stream.appendHTML
import org.http4k.routing.body

import java.io.BufferedReader
import java.io.InputStreamReader

import org.ktorm.database.Database
import org.ktorm.support.sqlite.SQLiteDialect

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.text.StringBuilder


class Config {
    companion object {
        val adminUsername = "barnabyjack"
        val adminPassword = "RIPquaeShueth8ohf5Aebi4RIP"
    }
}

val dbFileName = "http4k-webgoat.db"

// FIX: Replace regex-based sanitization with UUID-based file naming to eliminate directory traversal risk
private val fileNameMappings = ConcurrentHashMap<String, String>()

fun generateUUIDFileName(name: String): String {
    // Generate a cryptographically secure UUID instead of sanitizing user input
    return fileNameMappings.computeIfAbsent(name) {
        UUID.randomUUID().toString()
    }
}

// FIX: Simplified to generate UUID and handle message validation only
fun parseParams(name: String, msg: String): Map<String, String?> {
    // FIX: Generate UUID-based filename - completely eliminates path traversal risk
    val uuidFilename = generateUUIDFileName(name)
    
    // FIX: Sanitize message content
    val checkedMsg = msg.ifBlank { "default_msg" }
    
    return mapOf("parsed_name" to uuidFilename, "parsed_msg" to checkedMsg)
}

        },
        "/ping" bind GET to { Response(OK).body("ok") },
{ req ->
    // FIX: Extract query parameters
    val name = req.query("name")
    val msg = req.query("msg")
    
    // FIX: Validate required parameters exist
    if (name == null || msg == null) {
        Response(BAD_REQUEST).body("The `name` & `msg` parameters are required.")
    }
    // FIX: Validate name is not empty or excessively long for business logic
    else if (name.isEmpty() || name.length > 255) {
        Response(BAD_REQUEST).body("Invalid name parameter.")
    } else {
        val parsedParams = parseParams(name, msg)
        val parsedName = parsedParams["parsed_name"]
        
        if (parsedName == null) {
            Response(FORBIDDEN).body("Access denied: Unable to process request.")
        } else {
            val baseDir = File("/tmp/http4kexample/")
            baseDir.mkdirs() // Ensure directory exists
            
            // FIX: Add fixed file extension for security
            val targetFile = File(baseDir, "$parsedName.txt")
            
            val finalMsg = "MESSAGE: " + parsedParams["parsed_msg"]
            
            // FIX: Implement file size limit to prevent resource exhaustion
            if (finalMsg.length > 10000) {
                Response(BAD_REQUEST).body("Message too large.")
            } else {
                // FIX: Atomic file writing using temporary file to prevent partial writes
                val tempFile = File(baseDir, "${targetFile.name}.tmp")
                tempFile.writeText(finalMsg)
                tempFile.renameTo(targetFile)
                
                Response(OK).body("Message written successfully.")
            }
        }
    }
}

            val username = req.query("username")
            val password = req.query("password")
            val out = db.useConnection {
                // vulnerability: SQL Injection
                val sql = "INSERT INTO user (username, password) VALUES ('$username', '$password');"
                val stmt = it.createStatement()
                stmt.execute(sql)
                username
            }
            Response(OK).body("Did insert new user `" + out + "`.")
        },
        "forgotten_debug_route" bind GET to { req ->
            val url = req.query("url")
            // vulnerability: Open Redirect
            Response(TEMPORARY_REDIRECT).header("Location", url)
        },
        "render_html" bind GET to { req ->
            val name = req.query("name")
            val out = StringBuilder()

            out.appendHTML().html {
                body {
                    // vulnerability: XSS
                    unsafe {
                        + "<h1>Hello there, `$name`!</h1>"
                    }
                    div {
                        a("https://shiftleft.io") {
                            target = ATarget.blank
                            + "Hitting F12 is a crime, clicking this link is not"
                        }
                    }
                }
            }
            Response(OK).body(out.toString())
        },
    )
}

fun main() {
    Files.deleteIfExists(Paths.get(dbFileName)) // remove db file on each application start

    val db = Database.connect(url = "jdbc:sqlite:$dbFileName", dialect = SQLiteDialect())
    db.useConnection { conn ->
        val createTableSQL = "CREATE TABLE IF NOT EXISTS user (id INTEGER PRIMARY KEY, username TEXT, password TEXT)"
        val createStmt = conn.createStatement()
        createStmt.execute(createTableSQL)

        val insertUserSQL = "INSERT INTO user (id, username, password) VALUES (1, 'admin', 'admin')"
        val insertUserStmt = conn.createStatement()
        insertUserStmt.execute(insertUserSQL)
    }

    val port = 8080
    println("Serving content on port " + port.toString() + ".")
    HelloWorld(db).asServer(SunHttp(port)).start()
}
