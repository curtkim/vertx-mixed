package vertx.util;

import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ResponseSupport {
	public static String CALLBACK_NAME = "callback";
	public static String CONTENT_TYPE_JSON = "application/json; charset=utf-8";

	public static void outputJson(HttpServerResponse res, JsonObject json) {
		res.putHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON);
		res.end(json.toString());
	}
	public static void outputJson(HttpServerResponse res, JsonObject json, String cbName) {
		res.putHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON);
		res.setChunked(true);
		if(cbName != null)
			res.write(cbName + "(");
		res.write(json.toString());
		if(cbName != null)
			res.write(")");
		res.end();
	}
	public static void outputJson(HttpServerResponse res, JsonArray json) {
		res.putHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON);
		res.end(json.toString());
	}
	public static void outputJson(HttpServerResponse res, JsonArray json, String cbName) {
		res.putHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON);
		res.setChunked(true);
		if(cbName != null)
			res.write(cbName + "(");
		res.write(json.toString());
		if(cbName != null)
			res.write(")");
		res.end();
	}
	public static void outputJsonError(HttpServerResponse res, int status, Object err, String cbName){
		res.setStatusCode(status);
		JsonObject json = new JsonObject();
		json.put("error", err.toString());
		outputJson(res, json, cbName);
	}
	public static void outputJsonError(HttpServerResponse res, Object err){
		outputJsonError(res, 500, err, null);
	}
	public static void outputJsonError(HttpServerResponse res, Object err, String cbName){
		outputJsonError(res, 500, err, cbName);
	}
	public static void outputJson404(HttpServerResponse res){
		outputJsonError(res, 404, "not exist", null);
	}
	public static void outputJson404(HttpServerResponse res, String cbName){
		outputJsonError(res, 404, "not exist", cbName);
	}

}
