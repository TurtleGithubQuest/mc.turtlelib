package dev.turtle.turtlelib.database.mysql

import dev.turtle.turtlelib.database.Database
import dev.turtle.turtlelib.TurtlePlugin
import java.sql.Connection
import java.sql.DriverManager

abstract class MySql(
    private val dbName: String,
    ip: String,
    port: String?,
    sslMode: String,
    private val turtle: TurtlePlugin
) : Database(dbName, ip, port, sslMode, turtle=turtle) {
    override val dbType = "mysql"
    override fun onConnect(username: String, password: String) {
        val conn: Connection = DriverManager.getConnection(host(), username, password)
        this.connection = conn
        turtle.messageFactory.newMessage("&7Established connection to &e$dbName &8(&9$dbType&8)&7.").enablePrefix().send()
    }
}