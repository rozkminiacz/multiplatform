package index

import app.*
import kotlinext.js.*
import react.dom.*
import kotlin.browser.*

fun main(args: Array<String>) {
    window.onload = {
        render(document.getElementById("root")) {
            app()
        }
    }
}
