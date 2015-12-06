package vertx.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hazelcast.core.HazelcastInstance;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.impl.VertxImpl;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.AsyncMap;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

public class Utils {
	
	public static JsonArray list2JsonArray(List<JsonObject> list) {
		JsonArray result = new JsonArray();
		for(JsonObject json : list){
			result.add(json);
		}
		return result;
	}
	
	public static String readFile(String path) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, "UTF-8");
	}
	
	public static void getMap(Vertx vertx, String name, Callback cb){
		if( vertx.isClustered())
			vertx.sharedData().getClusterWideMap(name, res -> {
				if(res.succeeded())
					cb.done(null, res.result());
				else
					cb.done(res.cause(), null);
			});
		else
			cb.done( null, new AsyncMapDummyImpl(vertx));
	}
	
	public static void getIMap(Vertx vertx, String name, Callback cb){
		vertx.executeBlocking(future -> {
			HazelcastClusterManager clusterManger = (HazelcastClusterManager)((VertxImpl)vertx).getClusterManager();
			HazelcastInstance hazelInstance = clusterManger.getHazelcastInstance();
			future.complete(hazelInstance.getMap(name));
		}, res -> {
			if( res.succeeded())
				cb.done(null, res.result());
			else 
				cb.done(res.cause(), null);
		});
	}
	
	public static void post(HttpClient client, int port, String host, String path, Map<String, String> form, Callback3 callback){
		Buffer totalBuffer = Buffer.buffer();
		HttpClientRequest req = client.post(port, host, path, res -> {
			res.handler(buffer -> totalBuffer.appendBuffer(buffer));
			res.endHandler(v -> callback.done(null, res, totalBuffer.toString("utf8")));			
		});
		req.exceptionHandler(e-> callback.done(e, null, null) );		
		String body = makeBody(form);
		Buffer formBuffer = Buffer.buffer(body);		
		req.putHeader("Content-Type", "application/x-www-form-urlencoded");
		req.putHeader("Content-Length", formBuffer.length()+"");
		req.write(formBuffer);
		req.end();
	}

	public static void get(HttpClient client, int port, String host, String path, Callback3 callback){
		Buffer totalBuffer = Buffer.buffer();
		HttpClientRequest req = client.get(port, host, path, (res) -> {
			res.handler(buffer -> totalBuffer.appendBuffer(buffer));
			res.endHandler(v ->
				callback.done(null, res, totalBuffer.toString("utf8"))
			);
		});
		req.exceptionHandler(e-> callback.done(e, null, null) );
		req.end();
	}
	
	public static void getJson(HttpClient client, int port, String host, String path, Callback3 callback){
		get(client, port ,host, path, (err, res, body)->{
			try {
				callback.done(err, res, parse((String)body));
			}
			catch(Exception ex){
				callback.done(ex, null, null);
			}
		});
	}
	
	static JsonObject parse(String str){
		return new JsonObject(str);
	}	
	
	private static String makeBody(Map<String, String> form){
		List<String> pairs = new ArrayList<String>();		
		for(String k : form.keySet()){
			String v = form.get(k);
			try {
				pairs.add(String.format("%s=%s", URLEncoder.encode(k, "UTF-8"), URLEncoder.encode(v, "UTF-8")));
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}			
		}
		return String.join("&", pairs);
	}
}

class AsyncMapDummyImpl implements AsyncMap{

	private Vertx vertx;
	private Map map;
	AsyncMapDummyImpl(Vertx vertx){
		this.vertx = vertx;
		this.map = new HashMap();
	}
	
	public void get(Object k, Handler resultHandler) {
		resultHandler.handle(new DummyAsyncResult(map.get(k)));		
	}
	public void put(Object k, Object v, Handler completionHandler) {
		map.put(k, v);
		completionHandler.handle(new DummyAsyncResult(null));
	}
	public void put(Object k, Object v, long timeout, Handler completionHandler) {
		map.put(k, v);
		completionHandler.handle(new DummyAsyncResult(null));		
	}
	public void putIfAbsent(Object k, Object v, Handler completionHandler) {
		throw new UnsupportedOperationException();
		//map.put(k, v);
		//completionHandler.handle(new DummyAsyncResult(null));				
	}
	public void putIfAbsent(Object k, Object v, long timeout, Handler completionHandler) {
		throw new UnsupportedOperationException();
		//map.put(k, v);
		//completionHandler.handle(new DummyAsyncResult(null));		
	}
	public void remove(Object k, Handler resultHandler) {		
		resultHandler.handle(new DummyAsyncResult(map.remove(k)));				
	}

	public void removeIfPresent(Object k, Object v, Handler resultHandler) {
		throw new UnsupportedOperationException();
		//resultHandler.handle(new DummyAsyncResult(map.remove(k, v)));						
	}
	public void replace(Object k, Object v, Handler resultHandler) {
		throw new UnsupportedOperationException();
		//Object old = map.remove(k);
		//map.put(k, v);
		//resultHandler.handle(new DummyAsyncResult(old));						
	}
	public void replaceIfPresent(Object k, Object oldValue, Object newValue, Handler resultHandler) {
		throw new UnsupportedOperationException();
		//Object old = map.remove(k);
		//map.put(k, newValue);
		//resultHandler.handle(new DummyAsyncResult(old));							
	}
	public void clear(Handler resultHandler) {
		map.clear();
		resultHandler.handle(new DummyAsyncResult(null));
	}
	public void size(Handler resultHandler) {
		resultHandler.handle(new DummyAsyncResult(map.size()));		
	}
}

class DummyAsyncResult implements AsyncResult {
	
	private Object value;
	DummyAsyncResult(Object value){
		this.value = value;
	}
	public Object result() {
		return value;
	}
	public Throwable cause() {
		return null;
	}
	public boolean succeeded() {
		return true;
	}
	public boolean failed() {
		return false;
	}
}