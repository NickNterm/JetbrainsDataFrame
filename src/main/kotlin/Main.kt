import org.jetbrains.kotlinx.dataframe.DataFrame
import kotlin.random.Random
import org.jetbrains.kotlinx.dataframe.io.*
import org.jetbrains.kotlinx.dataframe.api.*
import java.io.File
import kotlin.reflect.KType
import kotlin.reflect.typeOf


/*
 * what I want to change here. 1st nice would be to have a flag that invert the color.
 * that means to be able to go from red to green of from green to red (its common that smaller nums are better)
 * also I would add coloring options with 2 colors from colorStart, colorEnd that the range animates between those
 * values and not static red and green
 */
fun getColor(min: Int, max: Int, value: Int): String {
    val ratio = (value - min).toFloat() / (max - min).toFloat()
    val red = ((1 - ratio) * 255).toInt()
    val green = (ratio * 255).toInt()

    return String.format("#%02X%02X00", red, green)
}

/*
 * I use this function to create the title of the HTML table.
 * I have it to extend DataFrame and creates a string with the
 * head, tr, and td elements
 */
fun <T> DataFrame<T>.createHtmlTitle(): String {
    // get the column keys
    val titles = schema().columns.keys.toList()
    var headContent = ""
    // for each title create a <td> element
    titles.forEach { title ->
        headContent += "<td>$title</td>"
    }
    // append all titles on <thead>
    val head = buildString {
        append("<thead><tr>")
        append(headContent)
        append("</tr></thead>")
    }
    return head
}

/*
 * This function is about creating the final td element of a field.
 * currently it checks if we have int, bool fields to have color and
 * other elements are default
 */
fun tdElement(
    columnName: String,
    type: KType,
    value: String,
    extra: MutableMap<String, Pair<Int, Int>>
): String {
    return if (type == typeOf<Int?>() || type == typeOf<Int>()) {
        "<td style=\"background-color:${
            getColor(
                extra[columnName]!!.first,
                extra[columnName]!!.second,
                value.toInt()
            )
        };\">$value</td>"
    } else if (type == typeOf<Boolean?>() || type == typeOf<Boolean>()) {
        if (value.equals("true")) {
            "<td style=\"background-color:green\">&#10003;</td>"
        } else {
            "<td style=\"background-color:red;\">&#10007;</td>"
        }
    } else if (type == typeOf<String?>() || type == typeOf<String>()) {
        val imageRegex = Regex("(https|http)://.*\\.(png|jpg|jpeg)")
        println("value $value")
        println("matches ${imageRegex.matches(value)}")
        if (imageRegex.matches(value)) {
            "<td><img src=\"${value}\" width=100px height=100px></td>"
        } else {
            "<td>$value</td>"
        }
    } else {
        "<td>$value</td>"
    }
}

/*
 * Create HTML Body it gets the rows and creates the necessary table rows
 */
fun <T> DataFrame<T>.createHtmlBody(): String {
    // final list of the row elements
    val rowList: MutableList<String> = mutableListOf()
    // get the description of a DataFrame to get the min,max from the integer types
    // I don't have this inside the getColor function so that we don't call describe many times.
    // I also don't like passing the extra param on get color so something like Singleton may do the job better
    val integerColumns: MutableMap<String, Pair<Int, Int>> = mutableMapOf()
    describe().forEach { it ->
        if (it.toMap()["type"]!! == "Int") {
            integerColumns[it.name] =
                Pair(
                    it.min.toString().toInt(),
                    it.max.toString().toInt()
                )
        }

    }
    // get the rows and start building rows
    rows().map { row ->
        val columns = schema().columns.map { column ->
            Pair(column.key, column.value.type)
        }

        // get the td data for all the columns
        var data = ""
        columns.forEach { column ->
            data += tdElement(column.first, column.second, row[column.first].toString(), integerColumns)
        }
        // add a new row
        val tdRow = buildString {
            append("<tr>")
            append(data)
            append("</tr>")
        }
        rowList.add(tdRow)
    }
    // create the body with the joined rowList
    val body = buildString {
        append("<tbody>")
        append(rowList.joinToString(""))
        append("</tbody>")
    }
    return body
}

fun tablecss(): String {
    // some css for the table
    return """
body{
    font-family: Verdana, sans-serif;
}
        
table {
    font-family: Arial, sans-serif;
    border-radius: 12px;
    border-collapse: collapse;
    background: #e6e6e6;
    margin: 1em;
}

th {
    border-bottom: 1px solid #364043;
    color: #E2B842;
    font-size: 0.85em;
    font-weight: 600;
    padding: 0.5em 1em;
    text-align: left;
}
td {
    color: #000;
    font-weight: 400;
    padding: 0.65em 1em;
}
.disabled td {
    color: #4F5F64;
}

tbody tr {
    transition: background 0.25s ease;
}
tbody tr:hover {
    background: #d6d6d6;
}
    """.trimIndent()
}

/*
 * This is my new way to show off a Dataframe.
 * Showing a dataframe in this way may be more pleasant for the end user
 * to see and understand the data better. I focus on 2 things
 * * Showing booleans with checks/cross and not true/false. Its just simpler
 * * Coloring background of integer fields to show off the real values
 */
fun <T> DataFrame<T>.toCustomHtml(): String {
    // built the html string
    val html = buildString {
        append("<html>")
        append("<style type=\"text/css\">")
        append(tablecss())
        append("</style>")
        append("<table class=\"dataframe\">")
        append(createHtmlTitle())
        append(createHtmlBody())
        append("</table>")
        append("<html>")
    }
    return html
}

fun main() {
    // Read csv to load the DataFrame
    var df = DataFrame.read("netflix.csv")

    var images = listOf(
        "https://pbs.twimg.com/profile_images/1399329694340747271/T5fbWxtN_400x400.png",
        "https://www.jetbrainsmerchandise.com/media/catalog/product/cache/ecfe99657bcf987295ea6f61f389da7e/j/b/jbst-012_jetbrains_logo.png"
    )
    // adding a Score on the Dataframe to have an integer to test on
    df = df.add("Score") { Random.nextInt(0, 6) }
        .rename("Release Year").into("ReleaseYear")
        // add a boolean to have test it out
        .add("isSeries") { (it["Duration"] as String).endsWith("min") }
        .add("Preview") { if (Random.nextBoolean()) images.random() else null }
        .fillNulls("Preview").with { "" }


    println(df.schema())
    File("index.html").writeText(df.toCustomHtml())
}
