package dev.turtle.turtlelib.database

import dev.turtle.turtlelib.TurtlePlugin
import java.sql.Connection
import java.sql.SQLException

abstract class Database(
    private val dbName: String,
    private val ip: String,
    private val port: String?,
    private val sslMode: String,
    private val turtle: TurtlePlugin
) {
    abstract val dbType: String
    lateinit var connection: Connection
    fun host(): String { return "jdbc:$dbType://$ip${port?.let {":$it"}?:""}/$dbName?sslmode=$sslMode" }
    abstract fun onConnect(username: String, password: String)
    fun connect(username: String, password: String) {
        try {
            this.onConnect(username, password)
        } catch (ex: SQLException) {
            turtle.disable("&7Database connection error: &c"+ex.localizedMessage)
        }
    }
}