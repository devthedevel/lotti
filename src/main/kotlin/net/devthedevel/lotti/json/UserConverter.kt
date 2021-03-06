package net.devthedevel.lotti.json

import com.beust.klaxon.Converter
import com.beust.klaxon.JsonValue
import sx.blah.discord.handle.obj.IGuild
import sx.blah.discord.handle.obj.IUser

class UserConverter(val guild: IGuild) {
    val converter = object : Converter {
        override fun canConvert(cls: Class<*>): Boolean {
            return cls == IUser::class.java
        }

        override fun fromJson(jv: JsonValue): Any {
            val str = jv.string
            if (str != null) {
                val users = guild.getUsersByName(str, true)
                return users[0]
            }
            return jv
        }

        override fun toJson(value: Any): String {
            return (value as IUser).name
        }

    }
}