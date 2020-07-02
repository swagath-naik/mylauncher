package com.swagath.mylauncher

data class AppInfoModel(
    var package_name: String,
    var package_label: String

) {

    override fun toString(): String {
        return """
            Package Name   :  $package_name
            Package Label  :  $package_label
        """.trimIndent()

    }


}