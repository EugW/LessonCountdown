package pro.eugw.lessoncountdown.util.network

import com.android.volley.NetworkResponse
import com.android.volley.ParseError
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonRequest
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset

class JsObRe(requestMethod: Int, urlS: String, body: Any, listener: Response.Listener<JsonObject>, errorListener: Response.ErrorListener) : JsonRequest<JsonObject>(requestMethod, urlS, body.toString(), listener, errorListener) {

    constructor(requestMethod: Int, urlS: String, listener: Response.Listener<JsonObject>, errorListener: Response.ErrorListener) : this(requestMethod, urlS, "", listener, errorListener)

    override fun parseNetworkResponse(response: NetworkResponse): Response<JsonObject> {
        return try {
            val jsonString = String(response.data, Charset.forName(HttpHeaderParser.parseCharset(headers, PROTOCOL_CHARSET)))
            Response.success(JsonParser().parse(jsonString).asJsonObject, HttpHeaderParser.parseCacheHeaders(response))
        } catch (e: UnsupportedEncodingException) {
            Response.error(ParseError(e))
        } catch (je: JsonParseException) {
            Response.error(ParseError(je))
        }

    }
}