package pro.eugw.lessoncountdown.util.network

import com.android.volley.NetworkResponse
import com.android.volley.ParseError
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonRequest
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import org.json.JSONException
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset

class JsArRe(requestMethod: Int, urlS: String, body: Any, listener: Response.Listener<JsonArray>, errorListener: Response.ErrorListener) : JsonRequest<JsonArray>(requestMethod, urlS, body.toString(), listener, errorListener) {

    constructor(requestMethod: Int, urlS: String, listener: Response.Listener<JsonArray>, errorListener: Response.ErrorListener) : this(requestMethod, urlS, "", listener, errorListener)

    override fun parseNetworkResponse(response: NetworkResponse): Response<JsonArray> {
        return try {
            val jsonString = String(response.data, Charset.forName(HttpHeaderParser.parseCharset(headers, PROTOCOL_CHARSET)))
            Response.success(JsonParser().parse(jsonString).asJsonArray, HttpHeaderParser.parseCacheHeaders(response))
        } catch (e: UnsupportedEncodingException) {
            Response.error(ParseError(e))
        } catch (je: JSONException) {
            Response.error(ParseError(je))
        }

    }
}