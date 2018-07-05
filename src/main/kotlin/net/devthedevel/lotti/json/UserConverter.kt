package net.devthedevel.lotti.json

import com.beust.klaxon.Converter
import com.beust.klaxon.JsonValue
import sx.blah.discord.handle.obj.IGuild
import sx.blah.discord.handle.obj.IRole

class UserConverter(val guild: IGuild) {
    val converter = object : Converter {
        override fun canConvert(cls: Class<*>): Boolean {
            return cls == IRole::class.java
        }

        override fun fromJson(jv: JsonValue): Any {
            val str = jv.string
            if (str != null) {
                val roles = guild.getRolesByName(str)
                return roles[0]
            }
            return jv
        }

        override fun toJson(value: Any): String {
            return (value as IRole).name
        }
    }
}